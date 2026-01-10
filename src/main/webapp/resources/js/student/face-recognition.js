/**
 * Face Recognition using face-api.js
 * Client-side face verification for Smart Attendance
 */

class FaceRecognition {
  constructor() {
    this.modelsLoaded = false;
    this.faceMatcher = null;
  }

  /**
   * Load face-api.js models
   */
  async loadModels() {
    if (this.modelsLoaded) return;

    try {
      // Load models from CDN
      await Promise.all([
        faceapi.nets.tinyFaceDetector.loadFromUri("/models"),
        faceapi.nets.faceLandmark68Net.loadFromUri("/models"),
        faceapi.nets.faceRecognitionNet.loadFromUri("/models"),
        faceapi.nets.ssdMobilenetv1.loadFromUri("/models"),
      ]);

      this.modelsLoaded = true;
      console.log("Face recognition models loaded successfully");
    } catch (error) {
      console.error("Error loading face recognition models:", error);
      throw error;
    }
  }

  /**
   * Create face matcher from profile image
   */
  async createFaceMatcher(profileImageUrl) {
    try {
      // Load profile image
      const img = await faceapi.fetchImage(profileImageUrl);

      // Detect face and create descriptor
      const detection = await faceapi
        .detectSingleFace(img, new faceapi.TinyFaceDetectorOptions())
        .withFaceLandmarks()
        .withFaceDescriptor();

      if (!detection) {
        throw new Error("No face detected in profile image");
      }

      // Create face matcher with the profile face
      this.faceMatcher = new faceapi.FaceMatcher(detection.descriptor, 0.6); // 0.6 = threshold

      return true;
    } catch (error) {
      console.error("Error creating face matcher:", error);
      return false;
    }
  }

  /**
   * Verify face against profile
   */
  async verifyFace(attendanceImageUrl) {
    try {
      if (!this.faceMatcher) {
        throw new Error("Face matcher not initialized");
      }

      // Load attendance image
      const img = await faceapi.fetchImage(attendanceImageUrl);

      // Detect face in attendance image
      const detection = await faceapi
        .detectSingleFace(img, new faceapi.TinyFaceDetectorOptions())
        .withFaceLandmarks()
        .withFaceDescriptor();

      if (!detection) {
        return {
          verified: false,
          confidence: 0,
          error: "No face detected in attendance image",
        };
      }

      // Find best match
      const bestMatch = this.faceMatcher.findBestMatch(detection.descriptor);

      return {
        verified: bestMatch.label !== "unknown",
        confidence: bestMatch.distance,
        label: bestMatch.label,
        error: null,
      };
    } catch (error) {
      console.error("Error verifying face:", error);
      return {
        verified: false,
        confidence: 0,
        error: error.message,
      };
    }
  }

  /**
   * Full verification process
   */
  async verifyAttendanceFace(profileImageUrl, attendanceImageUrl) {
    try {
      // Load models if not loaded
      await this.loadModels();

      // Create face matcher from profile
      const matcherCreated = await this.createFaceMatcher(profileImageUrl);
      if (!matcherCreated) {
        return {
          success: false,
          message: "Không thể tạo face matcher từ ảnh profile",
        };
      }

      // Verify attendance face
      const result = await this.verifyFace(attendanceImageUrl);

      if (result.error) {
        return {
          success: false,
          message: result.error,
        };
      }

      return {
        success: result.verified,
        confidence: result.confidence,
        message: result.verified
          ? `Xác thực khuôn mặt thành công (độ tin cậy: ${(
              1 - result.confidence
            ).toFixed(2)})`
          : `Xác thực khuôn mặt thất bại (độ tin cậy: ${(
              1 - result.confidence
            ).toFixed(2)})`,
      };
    } catch (error) {
      console.error("Face verification failed:", error);
      return {
        success: false,
        message: "Lỗi xác thực khuôn mặt: " + error.message,
      };
    }
  }

  /**
   * Check if face-api.js is available
   */
  isAvailable() {
    return typeof faceapi !== "undefined";
  }
}

// Global instance
const faceRecognition = new FaceRecognition();

// Export for use in other scripts
window.FaceRecognition = FaceRecognition;
window.faceRecognition = faceRecognition;
