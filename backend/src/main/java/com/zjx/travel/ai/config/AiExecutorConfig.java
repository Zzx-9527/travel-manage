package com.zjx.travel.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 流式生成用的线程池。
 *
 * <p>为什么需要独立线程池：Spring MVC 的 {@code SseEmitter} 要求 Controller
 * 先返回 emitter，再在别的线程里持续写数据；如果直接在请求线程里阻塞读取模型流，
 * 就把 Tomcat 的工作线程占死了，并发几个用户就排队。
 *
 * <p>池大小 4：单次生成 3-10 秒，4 个并发对 demo 足够，也避免无限制打满上游额度。
 * 线程设为 daemon，应用停止时不阻塞退出。
 */
@Configuration
public class AiExecutorConfig {

    @Bean(name = "aiStreamExecutor", destroyMethod = "shutdown")
    public ExecutorService aiStreamExecutor() {
        AtomicInteger seq = new AtomicInteger();
        return Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "ai-stream-" + seq.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }
}
