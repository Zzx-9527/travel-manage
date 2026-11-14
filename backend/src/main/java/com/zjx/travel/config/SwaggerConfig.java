package com.zjx.travel.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置（SpringDoc + OpenAPI 3）。
 *
 * <p>演进路径：springfox 3.0.0（2020 年停更，与 Boot 2.6+ 的 {@code PathPatternParser} 冲突）
 * → Knife4j 4.5（Boot 2 时代的中文 UI）→ <b>SpringDoc 官方 starter</b>。
 * 换掉 Knife4j 的原因很具体：它内置的 springdoc 2.3.0 调用了 Spring 6.2 已删除的
 * {@code ControllerAdviceBean(Object)} 构造器，在 Boot 3.4+ 上文档页必崩；
 * 强行升 springdoc 又会撞 {@code SpringDocConfigProperties.getGroupConfigs()} 改名。
 * 与其打补丁，不如回到官方实现：功能完整、升级路径可控。
 *
 * <p>文档地址：{@code /swagger-ui/index.html}（{@code /swagger-ui.html} 会重定向过去），
 * 原始 OpenAPI JSON 在 {@code /v3/api-docs}。
 *
 * <p>给接口加说明时使用 OpenAPI 3 注解：
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
