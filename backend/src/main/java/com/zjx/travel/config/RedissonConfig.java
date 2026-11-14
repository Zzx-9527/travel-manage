package com.zjx.travel.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Redisson 配置：只负责构建 {@link RedissonClient}，不接管 Spring 的 Redis 连接。
 *
 * <p>为什么手写 Bean 而不是引 {@code redisson-spring-boot-starter}：
 * starter 会注册 {@code RedissonConnectionFactory} 去顶替 {@code RedisConnectionFactory}，
 * 等于让锁框架接管整条缓存链路，序列化、TTL、连接参数都要重新回归；而且 starter 内部
 * 绑定 spring-data 版本，Boot 2.7 / 3.x 适配不同，升级容易踩坑。手写 10 行，边界清晰：
 * <b>Lettuce 管缓存与会话，Redisson 管锁与限流</b>。
 *
 * <p>配置复用 {@code spring.data.redis.*}（host / port / password / database / timeout），
 * 不新增一套连接参数，避免两边不一致。
 */
@Configuration
public class RedissonConfig {

    private static final Logger log = LoggerFactory.getLogger(RedissonConfig.class);

    /** 与 Lettuce 的 client-name 区分开，方便在 CLIENT LIST 里分辨两套连接 */
    private static final String CLIENT_NAME_SUFFIX = "-redisson";

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        // 延迟初始化：应用启动时不强制连 Redis，避免"缓存组件挂了应用起不来"，
        // 真正的连接在第一次使用锁/限流时才建立。
        config.setLazyInitialization(true);

        SingleServerConfig server = config.useSingleServer()
                .setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setDatabase(redisProperties.getDatabase())
                // Redisson 内部自带连接池，这里给个小一点的池即可（只跑锁和限流）
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(8)
                .setRetryAttempts(3)
                .setClientName(resolveClientName(redisProperties));

        if (StringUtils.hasText(redisProperties.getPassword())) {
            server.setPassword(redisProperties.getPassword());
        }
        Duration timeout = redisProperties.getTimeout();
        if (timeout != null) {
            server.setTimeout((int) timeout.toMillis());
        }
        Duration connectTimeout = redisProperties.getConnectTimeout();
        if (connectTimeout != null) {
            server.setConnectTimeout((int) connectTimeout.toMillis());
        }

        log.info("初始化 RedissonClient：{}:{}, database={}", redisProperties.getHost(),
                redisProperties.getPort(), redisProperties.getDatabase());
        return Redisson.create(config);
    }

    private String resolveClientName(RedisProperties redisProperties) {
        String base = StringUtils.hasText(redisProperties.getClientName())
                ? redisProperties.getClientName()
                : "travel-app";
        return base + CLIENT_NAME_SUFFIX;
    }
}
