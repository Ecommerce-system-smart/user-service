package com.ecommerce.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable()) // Tắt CSRF để gọi API post không bị chặn
//                .authorizeHttpRequests(auth -> auth
//                        .anyRequest().permitAll() // Cho phép tất cả request (để test cho dễ)
//                );
//
//        return http.build();
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Tắt CSRF nếu cần
                .authorizeHttpRequests(auth -> auth
                        // --- THÊM DÒNG NÀY ---
                        .requestMatchers("/actuator/**").permitAll() // Mở cửa cho Prometheus vào
                        // ---------------------
                        .anyRequest().authenticated() // Các cái khác vẫn bắt đăng nhập
                )
                // ... các cấu hình oauth2ResourceServer khác giữ nguyên
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    // Bean này giúp chuyển đổi thông tin từ Token của Keycloak sang User của Spring
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // Keycloak thường để tên đăng nhập ở field "preferred_username"
        // Nếu không có dòng này, Spring sẽ lấy ID (dạng UUID) làm username
        converter.setPrincipalClaimName("sid");

        return converter;
    }
}