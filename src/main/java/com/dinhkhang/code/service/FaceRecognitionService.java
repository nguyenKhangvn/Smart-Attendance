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
 * Service nâng cao cho nhận diện khuôn mặt
 * - Lưu ảnh profile lần đầu
 * - Verify khuôn mặt cho các lần điểm danh sau
 * - Tiết kiệm Cloudinary storage
 */
@Service
public class FaceRecognitionService {

    @Autowired
    private Cloudinary cloudinary;

    @Value("${cloudinary.folder}")
    private String folder;

    /**
     * Upload ảnh profile khuôn mặt (lần đầu tiên)
     * Chỉ lưu 1 ảnh profile cho mỗi sinh viên
     */
    public String uploadProfileFace(String base64Image, Long studentId) throws IOException {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            return null;
        }

        try {
            // Loại bỏ prefix nếu có
            String imageData = base64Image;
            if (base64Image.contains(",")) {
                imageData = base64Image.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(imageData);

            // Tạo public_id cho ảnh profile: smart-attendance/faces/profile/student_{id}
            String publicId = String.format("%s/profile/student_%d", folder, studentId);

            Map uploadResult = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", folder + "/profile",
                    "resource_type", "image",
                    "format", "jpg",
                    "transformation", ObjectUtils.asMap(
                            "width", 400,
                            "height", 400,
                            "crop", "fill",
                            "gravity", "face", // Focus vào khuôn mặt
                            "quality", "auto:good")));

            return (String) uploadResult.get("secure_url");

        } catch (Exception e) {
            throw new IOException("Failed to upload profile face: " + e.getMessage(), e);
        }
    }

    /**
     * Upload ảnh điểm danh (tạm thời để verify)
     * Ảnh này sẽ bị xóa sau khi verify xong
     */
    public String uploadTempAttendanceFace(String base64Image, Long studentId, Long sessionId) throws IOException {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            return null;
        }

        try {
            String imageData = base64Image;
            if (base64Image.contains(",")) {
                imageData = base64Image.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(imageData);

            // Tạo public_id tạm thời:
            // smart-attendance/faces/temp/session_{sessionId}_student_{studentId}_{timestamp}
            String publicId = String.format("%s/temp/session_%d_student_%d_%d",
                    folder, sessionId, studentId, System.currentTimeMillis());

            Map uploadResult = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", folder + "/temp",
                    "resource_type", "image",
                    "format", "jpg",
                    "transformation", ObjectUtils.asMap(
                            "width", 400,
                            "height", 400,
                            "crop", "fill",
                            "gravity", "face",
                            "quality", "auto:good")));

            return (String) uploadResult.get("secure_url");

        } catch (Exception e) {
            throw new IOException("Failed to upload temp attendance face: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa ảnh tạm thời sau khi verify
     */
    public boolean deleteTempFace(String publicId) {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equals(result.get("result"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verify khuôn mặt sử dụng thư viện face-api.js (client-side)
     * 
     * @param profileImageUrl    URL ảnh profile
     * @param attendanceImageUrl URL ảnh điểm danh
     * @return JSON response từ face-api.js comparison
     */
    public String verifyFaceWithFaceApi(String profileImageUrl, String attendanceImageUrl) {
        // Placeholder - sẽ được gọi từ JavaScript
        // Trả về cấu trúc JSON để frontend xử lý
        return String.format("{\"profileImage\":\"%s\",\"attendanceImage\":\"%s\",\"action\":\"compare\"}",
                profileImageUrl, attendanceImageUrl);
    }

    /**
     * Verify khuôn mặt đơn giản (không dùng AI)
     * Chỉ kiểm tra xem có khuôn mặt trong ảnh không
     * 
     * @param profileImageUrl    URL ảnh profile
     * @param attendanceImageUrl URL ảnh điểm danh
     * @return true nếu cả hai ảnh đều có metadata hợp lệ
     */
    public boolean simpleFaceVerification(String profileImageUrl, String attendanceImageUrl) {
        // Dummy implementation - kiểm tra URL hợp lệ
        if (profileImageUrl == null || attendanceImageUrl == null) {
            return false;
        }

        // Kiểm tra URL format
        boolean validProfile = profileImageUrl.contains("cloudinary") && profileImageUrl.contains("image/upload");
        boolean validAttendance = attendanceImageUrl.contains("cloudinary")
                && attendanceImageUrl.contains("image/upload");

        return validProfile && validAttendance;
    }

    /**
     * Lấy public_id từ Cloudinary URL
     */
    public String extractPublicIdFromUrl(String cloudinaryUrl) {
        if (cloudinaryUrl == null || !cloudinaryUrl.contains("cloudinary.com")) {
            return null;
        }

        try {
            // URL format:
            // https://res.cloudinary.com/{cloud_name}/image/upload/v{version}/{public_id}.{format}
            String[] parts = cloudinaryUrl.split("/");
            String fileNameWithExt = parts[parts.length - 1];
            String publicIdWithVersion = parts[parts.length - 2] + "/" + fileNameWithExt.split("\\.")[0];
            return publicIdWithVersion;
        } catch (Exception e) {
            return null;
        }
    }
}
