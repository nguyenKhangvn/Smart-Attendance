package com.dinhkhang.code.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation; // Import quan trọng
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/**
 * Service để upload ảnh lên Cloudinary
 */
@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    @Value("${cloudinary.folder}")
    private String folder;

    /**
     * Upload ảnh từ Base64 string lên Cloudinary
     */
    public String uploadFaceImage(String base64Image, Long studentId, Long sessionId) throws IOException {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            return null;
        }

        try {
            String imageData = base64Image;
            if (base64Image.contains(",")) {
                imageData = base64Image.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(imageData);

            String publicId = String.format("%s/session_%d/student_%d_%d",
                    folder, sessionId, studentId, System.currentTimeMillis());

            // ===== ĐÃ SỬA: Thêm .generate() =====
            Map uploadResult = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", folder,
                    "resource_type", "image",
                    "format", "jpg",
                    "transformation", new Transformation()
                            .width(800)
                            .height(800)
                            .crop("limit")
                            .quality("auto:good")
                            .generate() // <--- QUAN TRỌNG
            ));

            return (String) uploadResult.get("secure_url");

        } catch (Exception e) {
            throw new IOException("Failed to upload image to Cloudinary: " + e.getMessage(), e);
        }
    }

    public boolean deleteImage(String publicId) {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equals(result.get("result"));
        } catch (Exception e) {
            return false;
        }
    }
}