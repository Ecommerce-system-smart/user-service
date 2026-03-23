package com.ecommerce.userservice.model;

import com.ecommerce.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "t_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(length = 20)
    private String phone;

    // Lưu ID tài khoản mapping trên Keycloak để tiện truy vấn sau này
    @Column(name = "keycloak_id", length = 36)
    private String keycloakId;

    // CHÚ Ý: Mật khẩu KHÔNG được lưu ở đây vì Keycloak sẽ quản lý bảo mật.
}
