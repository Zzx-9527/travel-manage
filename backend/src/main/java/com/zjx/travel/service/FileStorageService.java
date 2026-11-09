package com.zjx.travel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 图片存储服务：本地磁盘 + 静态访问。
 *
 * <p>安全要点（上传功能的核心考点）：
 * <ol>
 *   <li><b>防重名</b>：文件名用 UUID，不拼接原始文件名，天然不会覆盖别人的文件；</li>
 *   <li><b>防路径穿越</b>：原始文件名只用来取后缀，且后缀必须在白名单内；落盘路径必须
 *       归一化后仍然位于上传根目录之内（{@code startsWith} 校验），{@code ..} 直接拒绝；</li>
 *   <li><b>类型校验</b>：后缀白名单 + Content-Type 必须是 image/*；</li>
 *   <li><b>删除同样要校验</b>：只允许删除本服务生成过的 URL，不能借删除接口删任意文件。</li>
 * </ol>
 *
 * <p>目录结构：{@code uploads/yyyy/MM/dd/{uuid}.{ext}}，按日期分目录避免单目录文件过多。
 * 访问 URL：{@code {context-path}/uploads/yyyy/MM/dd/{uuid}.{ext}}。
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    /** 允许的图片后缀 */
    private static final Set<String> ALLOWED_EXTENSIONS =
            new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp"));

    private static final DateTimeFormatter DATE_DIR_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 上传根目录（绝对路径、已归一化） */
    private final Path root;

    /** URL 前缀，例如 /travel/uploads */
    private final String urlPrefix;

    public FileStorageService(@Value("${travel.upload.dir:./uploads}") String uploadDir,
                              @Value("${server.servlet.context-path:}") String contextPath) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
        String normalizedContext = contextPath == null ? "" : contextPath.trim();
        this.urlPrefix = (normalizedContext.isEmpty() ? "" : normalizedContext) + "/uploads";
        log.info("文件上传目录：{}，访问前缀：{}", this.root, this.urlPrefix);
    }

    /**
     * 保存图片。
     *
     * @return 包含访问 URL、原始文件名、大小的结果
     * @throws IllegalArgumentException 文件为空、类型不在白名单、或路径非法
     */
    public StoredFile store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("只支持 jpg/jpeg/png/gif/webp 图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("文件类型不合法，仅支持图片");
        }

        String dateDir = LocalDate.now().format(DATE_DIR_FORMAT);
        Path targetDir = root.resolve(dateDir).normalize();
        if (!targetDir.startsWith(root)) {
            throw new IllegalArgumentException("非法的存储路径");
        }
        Files.createDirectories(targetDir);

        // 文件名只用 UUID：原始文件名不参与路径，既防重名也防穿越
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = targetDir.resolve(fileName).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法的存储路径");
        }

        file.transferTo(target.toFile());
        String url = urlPrefix + "/" + dateDir + "/" + fileName;
        return new StoredFile(url, file.getOriginalFilename(), file.getSize());
    }

    /**
     * 删除本服务保存过的图片（用于"单图覆盖"：新图入库成功后删旧图）。
     *
     * <p>只接受自己生成的 URL 前缀；相对路径归一化后必须仍在根目录内，否则直接返回 false。
     *
     * @return true 表示文件确实被删除；false 表示不是本服务的 URL、路径非法或文件不存在
     */
    public boolean deleteByUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String prefix = urlPrefix + "/";
        if (!url.startsWith(prefix)) {
            return false;
        }
        String relative = url.substring(prefix.length());
        if (relative.contains("..")) {
            return false;
        }
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            return false;
        }
        try {
            boolean deleted = Files.deleteIfExists(target);
            if (deleted) {
                log.info("已删除旧图片：{}", url);
            }
            return deleted;
        } catch (IOException e) {
            log.warn("删除旧图片失败：{}，cause={}", url, e.toString());
            return false;
        }
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            throw new IllegalArgumentException("文件缺少扩展名");
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** 上传结果 */
    public static class StoredFile {
        private final String url;
        private final String originalName;
        private final long size;

        public StoredFile(String url, String originalName, long size) {
            this.url = url;
            this.originalName = originalName;
            this.size = size;
        }

        public String getUrl() {
            return url;
        }

        public String getOriginalName() {
            return originalName;
        }

        public long getSize() {
            return size;
        }
    }
}
