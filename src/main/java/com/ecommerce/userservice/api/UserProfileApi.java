package com.ecommerce.userservice.api;

import com.ecommerce.userservice.dto.UserProfileResponse;
import com.ecommerce.userservice.dto.UserProfileUpdateRequest;
import com.ecommerce.userservice.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Collections;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecommerce.userservice.utils.EncryptionUtil;

@RestController
@RequestMapping("/api/users/profile")
public class UserProfileApi {

    @Autowired
    private UserProfileService userProfileService;

    // Lấy thông tin cá nhân
    @GetMapping
    public ResponseEntity<Map<String, String>> getProfile() {
        try {
            UserProfileResponse profile = userProfileService.getMyProfile();
            String json = new ObjectMapper().writeValueAsString(profile);
            return ResponseEntity.ok(Collections.singletonMap("payload", EncryptionUtil.encrypt(json)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // Cập nhật thông tin cá nhân
    @PutMapping
    public ResponseEntity<Map<String, String>> updateProfile(@RequestBody Map<String, String> requestMap) {
        try {
            String decrypted = EncryptionUtil.decrypt(requestMap.get("payload"));
            UserProfileUpdateRequest request = new ObjectMapper().readValue(decrypted, UserProfileUpdateRequest.class);
            UserProfileResponse updatedProfile = userProfileService.updateMyProfile(request);
            String json = new ObjectMapper().writeValueAsString(updatedProfile);
            return ResponseEntity.ok(Collections.singletonMap("payload", EncryptionUtil.encrypt(json)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // Upload ảnh đại diện (Base64)
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String newAvatarUrl = userProfileService.uploadAvatar(file);
            return ResponseEntity.ok(Collections.singletonMap("avatarUrl", newAvatarUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    // Đổi mật khẩu
    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody com.ecommerce.userservice.dto.PasswordUpdateRequest request) {
        try {
            userProfileService.updatePassword(request);
            return ResponseEntity.ok(Collections.singletonMap("message", "Đổi mật khẩu thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }
}
