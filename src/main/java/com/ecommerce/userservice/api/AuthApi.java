package com.ecommerce.userservice.api;

import com.ecommerce.userservice.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*") 
public class AuthApi {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");

            // Simple validation
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Username and password are required."));
            }

            authService.registerUser(username, email, password);
            return ResponseEntity.ok(Collections.singletonMap("message", "Đăng ký thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        }
    }
}
