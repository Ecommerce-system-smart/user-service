package com.ecommerce.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordUpdateRequest {
    private String oldPassword; // Đã mã hoá AES
    private String newPassword; // Đã mã hoá AES
}
