package com.dinhkhang.code.service;

import com.cloudinary.Cloudinary;
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
     * 
     * @param base64Image Base64 encoded image string (có thể có hoặc không có
     *                    prefix "data:image/...")
     * @param studentId   ID của sinh viên (dùng để đặt tên file)
     * @param sessionId   ID của session (dùng để tổ chức thư mục)
     * @return URL của ảnh đã upload
     * @throws IOException nếu upload thất bại
     */
    public String uploadFaceImage(String base64Image, Long studentId, Long sessionId) throws IOException {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            return null;
        }

        try {
            // Loại bỏ prefix "data:image/png;base64," nếu có
            String imageData = base64Image;
            if (base64Image.contains(",")) {
                imageData = base64Image.split(",")[1];
            }

            // Decode Base64 to byte array
            byte[] imageBytes = Base64.getDecoder().decode(imageData);

            // Tạo public_id cho ảnh (format:
            // smart-attendance/faces/session_123/student_456_timestamp)
            String publicId = String.format("%s/session_%d/student_%d_%d",
                    folder, sessionId, studentId, System.currentTimeMillis());

            // Upload lên Cloudinary với optimization
            Map uploadResult = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", folder,
                    "resource_type", "image",
                    "format", "jpg",
                    "transformation", new com.cloudinary.Transformation()
                            .width(800)
                            .height(800)
                            .crop("limit")
                            .quality("auto:good")));

            // Trả về secure URL
            return (String) uploadResult.get("secure_url");

        } catch (Exception e) {
            throw new IOException("Failed to upload image to Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa ảnh từ Cloudinary
     * 
     * @param publicId Public ID của ảnh cần xóa
     * @return true nếu xóa thành công
     */
    public boolean deleteImage(String publicId) {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equals(result.get("result"));
        } catch (Exception e) {
            return false;
        }
    }
}
