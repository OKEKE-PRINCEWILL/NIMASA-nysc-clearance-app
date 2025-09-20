//package com.example.NIMASA.NYSC.Clearance.Form.service;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.UUID;
//
//@Service
//public class SignatureService {
//
//    @Value("${app.signature.upload-dir:signatures}")
//    private String uploadDir;
//
//    @Value("${app.signature.base-url:http://localhost:8080}")
//    private String baseUrl;
//
//    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
//    private static final String[] ALLOWED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif"};
//
//    public String saveSignatureFile(MultipartFile file, String userType, String userName) throws IOException {
//        if (file.isEmpty()) {
//            throw new IllegalArgumentException("Signature file cannot be empty");
//        }
//
//        // Validate file size
//        if (file.getSize() > MAX_FILE_SIZE) {
//            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
//        }
//
//        // Validate file extension
//        String originalFilename = file.getOriginalFilename();
//        if (originalFilename == null || !isValidFileExtension(originalFilename)) {
//            throw new IllegalArgumentException("Invalid file type. Only PNG, JPG, JPEG, and GIF files are allowed");
//        }
//
//        // Create upload directory if it doesn't exist
//        Path uploadPath = Paths.get(uploadDir);
//        if (!Files.exists(uploadPath)) {
//            Files.createDirectories(uploadPath);
//        }
//
//        // Generate unique filename
//        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
//        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
//        String fileExtension = getFileExtension(originalFilename);
//        String filename = String.format("%s_%s_%s_%s%s",
//                userType.toLowerCase(),
//                userName.replaceAll("[^a-zA-Z0-9]", "_"),
//                timestamp,
//                uniqueId,
//                fileExtension);
//
//        // Save file
//        Path filePath = uploadPath.resolve(filename);
//        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
//
//        return filename;
//    }
//
//    public String getSignatureUrl(String filename) {
//        if (filename == null || filename.trim().isEmpty()) {
//            return null;
//        }
//        return baseUrl + "/api/signatures/" + filename;
//    }
//
//    public void deleteSignatureFile(String filename) {
//        if (filename == null || filename.trim().isEmpty()) {
//            return;
//        }
//
//        try {
//            Path filePath = Paths.get(uploadDir).resolve(filename);
//            Files.deleteIfExists(filePath);
//        } catch (IOException e) {
//            // Log error but don't throw exception
//            System.err.println("Failed to delete signature file: " + filename + " - " + e.getMessage());
//        }
//    }
//
//    public byte[] getSignatureFile(String filename) throws IOException {
//        Path filePath = Paths.get(uploadDir).resolve(filename);
//        if (!Files.exists(filePath)) {
//            throw new IOException("Signature file not found: " + filename);
//        }
//        return Files.readAllBytes(filePath);
//    }
//
//    private boolean isValidFileExtension(String filename) {
//        String extension = getFileExtension(filename).toLowerCase();
//        for (String allowedExt : ALLOWED_EXTENSIONS) {
//            if (allowedExt.equals(extension)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private String getFileExtension(String filename) {
//        int lastDotIndex = filename.lastIndexOf('.');
//        return (lastDotIndex == -1) ? "" : filename.substring(lastDotIndex);
//    }
//}

package com.example.NIMASA.NYSC.Clearance.Form.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignatureService {

    private final Cloudinary cloudinary;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif"};

    public String saveSignatureFile(MultipartFile file, String userType, String userName) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Signature file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isValidFileExtension(originalFilename)) {
            throw new IllegalArgumentException("Invalid file type. Only PNG, JPG, JPEG, and GIF are allowed");
        }

        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String publicId = "signatures/" + userType.toLowerCase() + "_" +
                userName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + uniqueId;

        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "public_id", publicId,
                        "overwrite", true,
                        "resource_type", "image"
                )
        );

        return uploadResult.get("secure_url").toString(); // save this in DB
    }

    public void deleteSignatureFile(String publicId) throws IOException {
        if (publicId == null || publicId.trim().isEmpty()) {
            return;
        }
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    private boolean isValidFileExtension(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (allowedExt.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : filename.substring(lastDotIndex);
    }
}
