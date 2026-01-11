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
     * Debug method: Phân tích ảnh Base64 để detect vấn đề
     */
    public String analyzeBase64Image(String base64Image) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            return "EMPTY_IMAGE";
        }

        try {
            String imageData = base64Image;
            String mimeType = "unknown";

            if (base64Image.contains(",")) {
                String[] parts = base64Image.split(",");
                if (parts.length > 0) {
                    mimeType = parts[0];
                    if (parts.length > 1) {
                        imageData = parts[1];
                    }
                }
            }

            byte[] imageBytes = Base64.getDecoder().decode(imageData);

            StringBuilder analysis = new StringBuilder();
            analysis.append("MIME: ").append(mimeType).append(" | ");
            analysis.append("Size: ").append(imageBytes.length).append(" bytes | ");

            if (imageBytes.length >= 4) {
                analysis.append("Header: [")
                        .append(String.format("%02X", imageBytes[0])).append(" ")
                        .append(String.format("%02X", imageBytes[1])).append(" ")
                        .append(String.format("%02X", imageBytes[2])).append(" ")
                        .append(String.format("%02X", imageBytes[3])).append("] | ");

                boolean isJpeg = imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8
                        && imageBytes[2] == (byte) 0xFF;
                boolean isPng = imageBytes[0] == (byte) 0x89 && imageBytes[1] == (byte) 0x50 &&
                        imageBytes[2] == (byte) 0x4E && imageBytes[3] == (byte) 0x47;

                if (isJpeg)
                    analysis.append("Format: JPEG | ");
                else if (isPng)
                    analysis.append("Format: PNG | ");
                else
                    analysis.append("Format: UNKNOWN | ");
            }

            double entropy = calculateEntropy(imageBytes);
            analysis.append("Entropy: ").append(String.format("%.3f", entropy));

            if (entropy < 3.0) {
                analysis.append("  LOW ENTROPY - Possible solid color/black image");
            }

            return analysis.toString();

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private double calculateEntropy(byte[] data) {
        int[] freq = new int[256];
        for (byte b : data) {
            freq[b & 0xFF]++;
        }

        double entropy = 0.0;
        int total = data.length;
        for (int count : freq) {
            if (count > 0) {
                double p = (double) count / total;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
    }

    /**
     * Upload ảnh profile khuôn mặt (lần đầu tiên)
     */
    public String uploadProfileFace(String base64Image, Long studentId) throws IOException {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            return null;
        }

        try {
            String imageData = base64Image;
            if (base64Image.contains(",")) {
                imageData = base64Image.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(imageData);

            String publicId = String.format("%s/profile/student_%d", folder, studentId);

            // ===== ĐÃ SỬA: Thêm .generate() =====
            Map uploadResult = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", folder + "/profile",
                    "resource_type", "image",
                    "format", "jpg",
                    "transformation", new Transformation()
                            .width(400)
                            .height(400)
                            .crop("fill")
                            .gravity("face")
                            .quality("auto")
                            .generate() // <--- QUAN TRỌNG: Chuyển object thành string
            ));

            return (String) uploadResult.get("secure_url");

        } catch (Exception e) {
            throw new IOException("Failed to upload profile face: " + e.getMessage(), e);
        }
    }

    /**
     * Upload ảnh điểm danh (tạm thời để verify)
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

            String publicId = String.format("%s/temp/session_%d_student_%d_%d",
                    folder, sessionId, studentId, System.currentTimeMillis());

            // ===== ĐÃ SỬA: Thêm .generate() =====
            Map uploadResult = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", folder + "/temp",
                    "resource_type", "image",
                    "format", "jpg",
                    "transformation", new Transformation()
                            .width(400)
                            .height(400)
                            .crop("fill")
                            .gravity("face")
                            .quality("auto")
                            .generate() // <--- QUAN TRỌNG
            ));

            return (String) uploadResult.get("secure_url");

        } catch (Exception e) {
            throw new IOException("Failed to upload temp attendance face: " + e.getMessage(), e);
        }
    }

    public boolean deleteTempFace(String publicId) {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equals(result.get("result"));
        } catch (Exception e) {
            return false;
        }
    }

    public String verifyFaceWithFaceApi(String profileImageUrl, String attendanceImageUrl) {
        return String.format("{\"profileImage\":\"%s\",\"attendanceImage\":\"%s\",\"action\":\"compare\"}",
                profileImageUrl, attendanceImageUrl);
    }

    public boolean simpleFaceVerification(String profileImageUrl, String attendanceImageUrl) {
        if (profileImageUrl == null || attendanceImageUrl == null) {
            return false;
        }
        boolean validProfile = profileImageUrl.contains("cloudinary") && profileImageUrl.contains("image/upload");
        boolean validAttendance = attendanceImageUrl.contains("cloudinary")
                && attendanceImageUrl.contains("image/upload");

        if (!validProfile || !validAttendance) {
            return false;
        }
        return !profileImageUrl.contains("error") && !attendanceImageUrl.contains("error");
    }

    public String extractPublicIdFromUrl(String cloudinaryUrl) {
        if (cloudinaryUrl == null || !cloudinaryUrl.contains("cloudinary.com")) {
            return null;
        }
        try {
            String[] parts = cloudinaryUrl.split("/");
            String fileNameWithExt = parts[parts.length - 1];
            String publicIdWithVersion = parts[parts.length - 2] + "/" + fileNameWithExt.split("\\.")[0];
            return publicIdWithVersion;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateBase64Image(String base64Image) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            return false;
        }
        try {
            String imageData = base64Image;
            if (base64Image.contains(",")) {
                imageData = base64Image.split(",")[1];
            }
            byte[] imageBytes = Base64.getDecoder().decode(imageData);

            // Check size < 1KB
            if (imageBytes.length < 1000) {
                System.err.println(" Base64 image too small: " + imageBytes.length + " bytes");
                return false;
            }
            // Check headers
            if (imageBytes.length < 4) {
                return false;
            }
            boolean isJpeg = imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8
                    && imageBytes[2] == (byte) 0xFF;
            boolean isPng = imageBytes[0] == (byte) 0x89 && imageBytes[1] == (byte) 0x50 && imageBytes[2] == (byte) 0x4E
                    && imageBytes[3] == (byte) 0x47;

            if (!isJpeg && !isPng) {
                System.err.println(" Invalid image format in Base64");
                return false;
            }
            // Check entropy
            double entropy = calculateEntropy(imageBytes);
            if (entropy < 3.0) {
                System.err.println(" Low entropy image detected: " + String.format("%.3f", entropy));
                return false;
            }
            return true;
        } catch (Exception e) {
            System.err.println(" Error validating Base64 image: " + e.getMessage());
            return false;
        }
    }
}