package com.zjx.travel.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web 配置：注册 Sa-Token 拦截器做统一登录校验，并把上传目录映射成静态资源。
 *
 * <p>替换掉了原来的 {@code JwtInterceptor}（手写解析 Authorization 头 + 校验签名）。
 * Sa-Token 的拦截器除了登录校验，还负责解析方法上的 {@code @SaCheckRole} /
 * {@code @SaCheckPermission} 注解（Phase 4 会用到），这是手写拦截器做不到的。
 *
 * <p>白名单外的所有接口都要求携带 {@code Authorization: Bearer <token>}，
 * 未登录或 token 失效由 {@code GlobalExceptionHandler} 统一转成 401 JSON。
 *
 * <p>路径不含 context-path（{@code /travel}），因为拦截器匹配的是 servlet 内部路径。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 上传根目录，与 FileStorageService 使用同一个配置项 */
    private final String uploadDir;

    public WebConfig(@Value("${travel.upload.dir:./uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    /**
     * 免登录白名单：
     * <ul>
     *   <li>登录、注册（{@code /travelUser/add} 是注册接口）；</li>
     *   <li>接口文档相关（Knife4j 的 doc.html、SpringDoc 的 api-docs、静态资源）；</li>
     *   <li>上传后的图片（{@code /uploads/**}）：浏览器 {@code <img>} 无法携带 token，
     *       必须匿名可读，否则图片全部裂开；</li>
     *   <li>错误页，避免未登录时二次错误处理再次触发拦截。</li>
     * </ul>
     */
    private static final String[] WHITE_LIST = {
            "/travelUser/login",
            "/travelUser/add",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/uploads/**",
            "/favicon.ico",
            "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 放行 CORS 预检请求，否则浏览器跨域时会先被 401 拦掉
            SaRouter.match(SaHttpMethod.OPTIONS).free(r -> {
            }).stop();

            SaRouter.match("/**")
                    .notMatch(WHITE_LIST)
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }

    /**
     * 把 {@code /uploads/**} 映射到本地上传目录。
     *
     * <p>用 {@code file:} 协议指向绝对路径；路径统一转成正斜杠，避免 Windows 下
     * 反斜杠被当成转义字符导致 404。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = "file:" + root.toString().replace("\\", "/") + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
