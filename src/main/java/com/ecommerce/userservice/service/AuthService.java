package com.ecommerce.userservice.service;

import com.ecommerce.userservice.model.User;
import com.ecommerce.userservice.repo.UserRepository;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Map;
import com.ecommerce.userservice.utils.EncryptionUtil;
import com.ecommerce.userservice.dto.LoginRequest;

@Service
public class AuthService {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Autowired
    private UserRepository userRepository;

    // Helper tạo ra connection từ BE tới Keycloak với vai trò Admin
    private Keycloak getKeycloakConfig() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType("client_credentials")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }

    // Hàm tạo tài khoản nhận request từ FE gọi tới
    public void registerUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại trong hệ thống");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã được sử dụng trong hệ thống");
        }

        Keycloak keycloak = getKeycloakConfig();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(true);

        String decryptedPassword = EncryptionUtil.decrypt(password);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(decryptedPassword);

        user.setCredentials(Collections.singletonList(credential));

        // Gọi API của Keycloak
        Response response = keycloak.realm(realm).users().create(user);

        if (response.getStatus() == 201) {
            System.out.println("Tạo user trên Keycloak thành công!");

            // Lưu thông tin người dùng vào Local Database, không lưu mật khẩu.
            User localUser = new User();
            localUser.setUsername(username);
            localUser.setEmail(email);
            // Bạn có thể lấy ID từ header Location, hoặc set null tuỳ độ ưu tiên
            // localUser.setKeycloakId(...);
            userRepository.save(localUser);

            System.out.println("Đã lưu thông tin User vào Database.");
        } else {
            System.out.println("Lỗi khi tạo: " + response.getStatusInfo());
            throw new RuntimeException("Tạo tài khoản thất bại trên Identity System! " + response.getStatusInfo());
        }
    }

    public Map<String, Object> login(LoginRequest request) {
        try {
            String decryptedPassword = EncryptionUtil.decrypt(request.getPassword());

            String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("client_id", "ecommerce-ui");
            map.add("grant_type", "password");
            map.add("username", request.getUsername());
            map.add("password", decryptedPassword);

            HttpEntity<MultiValueMap<String, String>> keycloakRequest = new HttpEntity<>(map, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, keycloakRequest, Map.class);

            return response.getBody();
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            System.err.println("Keycloak Token API Error: " + e.getStatusCode() + " - " + body);
            throw new RuntimeException("Keycloak Error: " + body);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Đăng nhập thất bại: Tài khoản hoặc mật khẩu không chính xác. Detail: " + e.getMessage());
        }
    }
}
