package org.example.springboot.config;

import org.example.springboot.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MyInterceptorConfig implements WebMvcConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyInterceptorConfig.class);

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        try {
            // 获取文件基础路径
            String basePath = FileUtil.FILE_BASE_PATH;
            LOGGER.info("File base path: {}", basePath);
            
            // 构建图片目录路径
            Path imgFolderPath = Paths.get(basePath, "img");
            
            // 确保目录存在
            if (!Files.exists(imgFolderPath)) {
                Files.createDirectories(imgFolderPath);
                LOGGER.info("Created image directory: {}", imgFolderPath.toAbsolutePath());
            }
            
            // 转换为绝对路径并规范化（处理Windows路径）
            String absolutePath = imgFolderPath.toAbsolutePath().normalize().toString();
            
            // Windows路径需要特殊处理：将反斜杠转换为正斜杠，或使用file:///协议
            // Spring的file:协议会自动处理路径分隔符
            String fileUrl = "file:" + absolutePath;
            
            // 确保路径以分隔符结尾
            if (!absolutePath.endsWith(File.separator) && !absolutePath.endsWith("/")) {
                fileUrl += "/";
            }
            
            LOGGER.info("Registering image resource handler:");
            LOGGER.info("  Handler pattern: /api/img/**");
            LOGGER.info("  Resource location: {}", fileUrl);
            LOGGER.info("  Actual directory: {}", absolutePath);
            LOGGER.info("  Directory exists: {}", Files.exists(imgFolderPath));
            
            // 注册资源处理器
            registry.addResourceHandler("/api/img/**")
                    .addResourceLocations(fileUrl)
                    .resourceChain(false); // 禁用资源链以允许直接文件访问
            
        } catch (Exception e) {
            LOGGER.error("Failed to configure image resource handler", e);
            // 即使配置失败，也尝试注册一个基本的处理器，避免完全无法访问
            try {
                Path fallbackPath = Paths.get(FileUtil.FILE_BASE_PATH, "img");
                String fallbackUrl = "file:" + fallbackPath.toAbsolutePath().normalize() + "/";
                registry.addResourceHandler("/api/img/**")
                        .addResourceLocations(fallbackUrl);
                LOGGER.warn("Using fallback image resource handler: {}", fallbackUrl);
            } catch (Exception ex) {
                LOGGER.error("Failed to register fallback image resource handler", ex);
            }
        }
    }

}
