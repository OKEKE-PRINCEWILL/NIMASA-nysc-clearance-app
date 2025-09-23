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
