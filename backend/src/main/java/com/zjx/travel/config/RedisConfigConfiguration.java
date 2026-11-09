package com.zjx.travel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类。
 *
 * <p>Spring Boot 的 Redis 自动配置会给一个 {@code RedisTemplate<Object, Object>}，
 * 且默认使用 JDK 序列化（redis-cli 里 key 是 \xac\xed 开头的乱码、value 是二进制）。
 * 这里把它替换成项目统一使用的 {@code RedisTemplate<String, Object>}，并指定序列化方式。
 *
 * <p>序列化约定：
 * <ul>
 *   <li>key / hashKey 用 {@link StringRedisSerializer}：redis-cli 里直接看到可读字符串；</li>
 *   <li>value / hashValue 用 {@link GenericJackson2JsonRedisSerializer}：存 JSON 文本，
 *       同时写入 @class 类型信息，取出时能还原成实体对象，而不是 LinkedHashMap。</li>
 * </ul>
 *
 * <p>刻意不做的事：
 * <ul>
 *   <li>不配置连接池 —— 默认客户端是 Lettuce，线程安全 + 基于 Netty 多路复用，
 *       普通命令共用一个连接即可；本项目也没有 BLPOP 这类阻塞命令，加池没有收益；</li>
 *   <li>不在这里统一设置过期时间 —— TTL 和 key 前缀由调用方（RedisUtil / 业务层）显式指定，
 *       否则所有缓存会被同一个默认 TTL 抹平，也无法按业务区分冷热；</li>
 *   <li>不再单独声明 StringRedisTemplate —— 自动配置已经提供，重复声明会出现同类型多 Bean，
 *       注入时产生歧义。</li>
 * </ul>
 */
@Configuration
public class RedisConfigConfiguration {

    /**
     * 项目统一使用的 RedisTemplate。
     *
     * <p>Bean 名必须叫 redisTemplate：自动配置的
     * {@code @ConditionalOnMissingBean(name = "redisTemplate")} 检测到同名 Bean 后会退让，
     * 从而保证全项目只有一个 RedisTemplate，注入时不会出现两个候选。
     *
     * @param connectionFactory 由 Boot 根据 spring.redis.* 自动创建的 Lettuce 连接工厂
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        // 无参构造：内部会开启 Jackson 的 default typing（写入 @class），
        // 千万不要换成自己 new 的 ObjectMapper，否则类型信息丢失，反序列化只能拿到 LinkedHashMap。
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        // 触发初始化校验（序列化器、默认序列化器等），便于启动阶段暴露配置错误
        template.afterPropertiesSet();
        return template;
    }
}
