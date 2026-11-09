package com.zjx.travel.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置（Knife4j 4.5 + SpringDoc）。
 *
 * <p>原来的 springfox 3.0.0 自 2020 年起停止维护，与 Spring Boot 2.6+ 的
 * {@code PathPatternParser} 冲突，只能通过 {@code spring.mvc.pathmatch.matching-strategy=ANT_PATH_MATCHER}
 * 降级兼容。SpringDoc 直接基于 Spring 的 {@code HandlerMethod} 做扫描，是当前的标准实现；
 * Knife4j 在 SpringDoc 之上提供中文界面、接口分组与调试能力。
 *
 * <p>项目里没有历史 swagger 注解，因此迁移只需换依赖 + 这个 {@link OpenAPI} Bean：
 * 文档地址由 {@code /swagger-ui/index.html} 变为 {@code /doc.html}。
 *
 * <p>后续给接口加说明时，使用 OpenAPI 3 注解：
 * {@code @Tag}（类上）、{@code @Operation}（方法上）、{@code @Parameter}、{@code @Schema}（实体上）。
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI travelOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("旅游管理平台 API")
                .description("景点 / 攻略 / 路线 / 订单 / 收藏 / 消息 等模块的接口文档")
                .version("1.0.0")
                .contact(new Contact().name("travel-team")));
    }
}
