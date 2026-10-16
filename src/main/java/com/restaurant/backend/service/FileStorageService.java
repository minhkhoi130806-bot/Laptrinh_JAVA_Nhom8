package com.restaurant.backend.service;

import com.restaurant.backend.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

// Lưu ảnh upload vào thư mục trên ổ đĩa của backend (mặc định ./uploads)
// File được phục vụ lại qua URL /uploads/** (xem WebConfig)
@Service
public class FileStorageService {

    // Không cho SVG vì SVG có thể chứa script (XSS)
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    // Chỉ cho lưu vào các thư mục con đã biết, tránh ghi bậy ra ngoài
    private static final Set<String> FOLDERS = Set.of("menu", "categories", "employees", "misc");
    private static final String URL_PREFIX = "/uploads/";

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được thư mục upload: " + this.root, e);
        }
    }

    // Lưu file, trả về URL tương đối, ví dụ /uploads/menu/3f2a....jpg
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Chưa chọn file ảnh");
        }
        String dir = (folder == null || folder.isBlank()) ? "misc" : folder.toLowerCase(Locale.ROOT);
        if (!FOLDERS.contains(dir)) {
            throw new BadRequestException("Thư mục lưu ảnh không hợp lệ");
        }

        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BadRequestException("Chỉ chấp nhận ảnh JPG, PNG, GIF hoặc WEBP");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BadRequestException("File tải lên không phải ảnh");
        }

        try {
            byte[] head = new byte[12];
            int read;
            try (InputStream in = file.getInputStream()) {
                read = in.readNBytes(head, 0, head.length);
            }
            if (!looksLikeImage(head, read)) {
                throw new BadRequestException("Nội dung file không phải ảnh hợp lệ");
            }

            Path targetDir = root.resolve(dir).normalize();
            Files.createDirectories(targetDir);
            // Tên file ngẫu nhiên: không lộ tên gốc, không bị trùng, không path traversal
            String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = targetDir.resolve(name).normalize();
            if (!target.startsWith(root)) {
                throw new BadRequestException("Đường dẫn lưu file không hợp lệ");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return URL_PREFIX + dir + "/" + name;
        } catch (IOException e) {
            throw new IllegalStateException("Không thể lưu file ảnh", e);
        }
    }

    // Xóa file theo URL đã lưu trong DB; bỏ qua nếu không phải file do hệ thống upload
    public void deleteQuietly(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }
        try {
            Path target = root.resolve(url.substring(URL_PREFIX.length())).normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException | RuntimeException ignored) {
            // xóa không được cũng không sao, không làm hỏng nghiệp vụ chính
        }
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    // Kiểm tra "chữ ký" đầu file để chắc đây đúng là ảnh (không tin vào đuôi file)
    private static boolean looksLikeImage(byte[] b, int n) {
        if (n < 4) {
            return false;
        }
        boolean jpeg = (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
        boolean png = (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        boolean gif = b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8';
        boolean webp = n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
        return jpeg || png || gif || webp;
    }
}
