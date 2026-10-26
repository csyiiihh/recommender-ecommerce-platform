package org.example.springboot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {
    private final static  Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
    public final static String FILE_BASE_PATH = System.getProperty("app.upload-dir", System.getenv().getOrDefault("FILE_STORAGE_ROOT", System.getProperty("user.dir") + "/files/"));
    // 公共的文件保存方�?
    public static String saveFile(MultipartFile file, String folderName, String baseDir) {
        if (file == null || file.isEmpty() || file.getSize() > 8 * 1024 * 1024) {
            throw new org.example.springboot.exception.BusinessException("文件为空或超过8MB");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.matches("(?i).+\\.(png|jpe?g|gif)")) {
            throw new org.example.springboot.exception.BusinessException("仅支持 PNG、JPEG、GIF 图片");
        }
        Path root = Paths.get(FILE_BASE_PATH).toAbsolutePath().normalize();
        Path directory = root.resolve(baseDir).resolve(folderName == null ? "" : folderName).normalize();
        if (!directory.startsWith(root)) throw new org.example.springboot.exception.BusinessException("无效存储路径");
        try (var input = javax.imageio.ImageIO.createImageInputStream(file.getInputStream())) {
            var readers = javax.imageio.ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new org.example.springboot.exception.BusinessException("文件内容不是有效图片");
            var reader = readers.next();
            try {
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 25_000_000L) {
                    throw new org.example.springboot.exception.BusinessException("图片尺寸过大");
                }
                var image = reader.read(0);
                Files.createDirectories(directory);
                String name = java.util.UUID.randomUUID().toString() + ".png";
                Path target = directory.resolve(name);
                if (!javax.imageio.ImageIO.write(image, "png", target.toFile())) throw new IOException("无法编码图片");
                return "/" + baseDir + "/" + (folderName == null || folderName.isBlank() ? "" : folderName + "/") + name;
            } finally { reader.dispose(); }
        } catch (IOException e) {
            throw new org.example.springboot.exception.BusinessException("图片读取或保存失败");
        }
    }

    // 保存图片的方�?
    public static String saveImage(MultipartFile file, String folderName) {
        return saveFile(file, folderName, "img");
    }

    /**
     * 根据文件名删除文�?
     *
     * @param filename 文件名（相对于项目根目录的相对路径）
     * @return 删除成功返回 true，否则返�?false
     */
    public static boolean deleteFile(String filename) {
        try {
            // 获取文件的绝对路�?
            Path root = Paths.get(FILE_BASE_PATH).toAbsolutePath().normalize();
            Path filePath = root.resolve(filename.replaceFirst("^[\\/]+", "")).normalize();
            if (!filePath.startsWith(root) || filePath.equals(root)) return false;
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                LOGGER.info("File deleted: {}", filePath);
                return true;
            } else {
                LOGGER.warn("File not found: {}", filePath);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Error deleting file: {}", filename, e);
            return false;
        }
    }


}
