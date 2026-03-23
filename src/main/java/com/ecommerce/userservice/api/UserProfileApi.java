package com.ecommerce.userservice.api;

import com.ecommerce.userservice.dto.UserProfileResponse;
import com.ecommerce.userservice.dto.UserProfileUpdateRequest;
import com.ecommerce.userservice.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Collections;

@RestController
@RequestMapping("/api/users/profile")
public class UserProfileApi {

    @Autowired
    private UserProfileService userProfileService;

    // Lấy thông tin cá nhân
    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile() {
        return ResponseEntity.ok(userProfileService.getMyProfile());
    }

    // Cập nhật thông tin cá nhân
    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody UserProfileUpdateRequest request) {
        try {
            UserProfileResponse updatedProfile = userProfileService.updateMyProfile(request);
            return ResponseEntity.ok(updatedProfile);
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
