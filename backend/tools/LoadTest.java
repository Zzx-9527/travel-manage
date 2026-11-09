import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轻量 HTTP 压测器（无第三方依赖，JDK 8 直接编译运行）。
 *
 * <p>为什么不用 PowerShell 的 Invoke-WebRequest：它单次调用就有十几毫秒的客户端开销，
 * 而本项目热缓存接口的 RT 就在个位数毫秒，客户端开销会把结论完全淹没。
 * 这里用 HttpURLConnection + 固定线程池，尽量把测量误差压到 1ms 以内。
 *
 * <p>用法：
 * <pre>
 * javac -d target/loadtest tools/LoadTest.java
 * java -cp target/loadtest LoadTest &lt;url&gt; &lt;token&gt; &lt;threads&gt; &lt;perThread&gt; [warmup]
 * </pre>
 *
 * <p>输出一行 JSON（含 P50/P95/P99/QPS），便于脚本拼报告。
 */
public class LoadTest {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("用法: LoadTest <url> <token> <threads> <perThread> [warmup]");
            System.exit(2);
        }
        String url = args[0];
        String token = "-".equals(args[1]) ? null : args[1];
        int threads = Integer.parseInt(args[2]);
        int perThread = Integer.parseInt(args[3]);
        int warmup = args.length > 4 ? Integer.parseInt(args[4]) : 100;

        // 预热：建连接、触发 JIT，避免第一批请求把"冷启动"算进结果
        for (int i = 0; i < warmup; i++) {
            call(url, token);
        }

        final int total = threads * perThread;
        final long[] latencies = new long[total];
        final AtomicInteger errors = new AtomicInteger();
        final CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>(threads);

        for (int t = 0; t < threads; t++) {
            final int threadIndex = t;
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < perThread; i++) {
                    int idx = threadIndex * perThread + i;
                    long begin = System.nanoTime();
                    try {
                        call(url, token);
                        latencies[idx] = System.nanoTime() - begin;
                    } catch (Exception e) {
                        latencies[idx] = -1L;
                        errors.incrementAndGet();
                    }
                }
            }));
        }

        long wallBegin = System.nanoTime();
        startGate.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        long elapsedNanos = System.nanoTime() - wallBegin;
        pool.shutdown();

        long[] ok = Arrays.stream(latencies).filter(v -> v >= 0).sorted().toArray();
        long elapsedMs = Math.max(1L, elapsedNanos / 1_000_000L);
        double qps = total * 1000.0 / elapsedMs;

        System.out.println(String.format(
                "{\"url\":\"%s\",\"threads\":%d,\"perThread\":%d,\"requests\":%d,\"errors\":%d,"
                        + "\"elapsedMs\":%d,\"qps\":%.1f,\"avgMs\":%.2f,\"p50Ms\":%.2f,"
                        + "\"p95Ms\":%.2f,\"p99Ms\":%.2f,\"minMs\":%.2f,\"maxMs\":%.2f}",
                url, threads, perThread, total, errors.get(),
                elapsedMs, qps,
                avgMs(ok), percentile(ok, 0.50), percentile(ok, 0.95), percentile(ok, 0.99),
                ok.length == 0 ? 0.0 : ok[0] / 1_000_000.0,
                ok.length == 0 ? 0.0 : ok[ok.length - 1] / 1_000_000.0));
    }

    private static double avgMs(long[] sorted) {
        if (sorted.length == 0) {
            return 0.0;
        }
        long sum = 0L;
        for (long value : sorted) {
            sum += value;
        }
        return sum / (double) sorted.length / 1_000_000.0;
    }

    private static double percentile(long[] sorted, double p) {
        if (sorted.length == 0) {
            return 0.0;
        }
        int index = (int) Math.ceil(p * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(index, sorted.length - 1))] / 1_000_000.0;
    }

    private static void call(String url, String token) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Connection", "keep-alive");
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        try {
            int code = conn.getResponseCode();
            InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (stream != null) {
                byte[] buffer = new byte[8192];
                while (stream.read(buffer) >= 0) {
                    // 必须读完，否则连接无法复用
                }
                stream.close();
            }
            if (code != 200) {
                throw new IOException("HTTP " + code);
            }
        } finally {
            conn.disconnect();
        }
    }
}
