package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.UserProfileResponse;
import com.ecommerce.userservice.dto.UserProfileUpdateRequest;
import com.ecommerce.userservice.model.User;
import com.ecommerce.userservice.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.Base64;
import java.io.IOException;
import com.ecommerce.userservice.dto.PasswordUpdateRequest;
import com.ecommerce.userservice.utils.EncryptionUtil;

@Service
public class UserProfileService {

    @Autowired
    private UserRepository userRepository;

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    // Helper method to get the current authenticated username from Keycloak JWT
    private String getCurrentUsername() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return jwt.getClaimAsString("preferred_username");
    }

    public UserProfileResponse getMyProfile() {
        String username = getCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found in local database"));

        return UserProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public UserProfileResponse updateMyProfile(UserProfileUpdateRequest request) {
        String username = getCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found in local database"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        
        // Cập nhật email nếu có thay đổi và chưa bị ai khác dùng
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
               throw new RuntimeException("Email đã được sử dụng bởi người khác");
            }
            user.setEmail(request.getEmail());
        }

        userRepository.save(user);

        return getMyProfile();
    }

    public String uploadAvatar(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Vui lòng gửi kèm file ảnh hợp lệ");
        }

        String username = getCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found in local database"));

        try {
            // Chuyển file ảnh thành chuỗi Base64
            String contentType = file.getContentType();
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            
            // Xây dựng chuỗi Data URI: "data:image/jpeg;base64,....."
            String avatarUrl = "data:" + contentType + ";base64," + base64Image;

            // Lưu trực tiếp vào Database
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            return avatarUrl;
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi đọc file ảnh: " + e.getMessage());
        }
    }

    public void updatePassword(PasswordUpdateRequest request) {
        String username = getCurrentUsername();
        
        String decryptedOldPassword = EncryptionUtil.decrypt(request.getOldPassword());
        String decryptedNewPassword = EncryptionUtil.decrypt(request.getNewPassword());

        // Kiểm tra mật khẩu cũ bằng cách thử get token
        try {
            String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("client_id", clientId);
            map.add("grant_type", "password");
            map.add("username", username);
            map.add("password", decryptedOldPassword);
            HttpEntity<MultiValueMap<String, String>> keycloakRequest = new HttpEntity<>(map, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, keycloakRequest, Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                 throw new RuntimeException("Fake Exception"); // nhảy catch
            }
        } catch (Exception e) {
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        }

        // Đổi pass qua Keycloak Admin
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType("client_credentials")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
        
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentKeycloakUserId = jwt.getClaimAsString("sub");

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(decryptedNewPassword);
        credential.setTemporary(false);

        keycloak.realm(realm).users().get(currentKeycloakUserId).resetPassword(credential);
    }
}
