package com.ecommerce.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

@Configuration
public class AuditorConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // Lấy thông tin xác thực từ Context của Spring Security
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }

            Object principal = authentication.getPrincipal();
            
            // Nếu dùng Keycloak (OAuth2 resource server), principal thường là JWT object
            if (principal instanceof Jwt) {
                Jwt jwt = (Jwt) principal;
                // Nếu không lấy được thẻ UID, fallback về system
                String uid = jwt.getClaimAsString("preferred_username");
                if (uid == null) {
                   uid = jwt.getClaimAsString("sub"); 
                }
                return Optional.ofNullable(uid != null ? uid : "system");
            }

            return Optional.of(authentication.getName());
        };
    }
}
