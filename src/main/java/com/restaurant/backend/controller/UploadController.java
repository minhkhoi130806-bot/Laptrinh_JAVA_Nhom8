package com.restaurant.backend.controller;

import com.restaurant.backend.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

// Upload ảnh dùng chung cho admin (món ăn, danh mục...) và sau này cho shop
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final FileStorageService storageService;

    public UploadController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    // POST /api/uploads  (multipart/form-data: file, folder = menu | categories | employees | misc)
    // Trả về {"url": "/uploads/menu/xxxx.jpg"} -> lưu chuỗi này vào cột image_url
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "folder", defaultValue = "misc") String folder) {
        return Map.of("url", storageService.store(file, folder));
    }
}
