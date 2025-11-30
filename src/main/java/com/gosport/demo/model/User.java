package com.gosport.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "phone", length = 20)
    private String telefono;

    @Column(name = "document_type", length = 5)
    private String tipoDocumento;

    @Column(name = "identification_number", length = 50, unique = true)
    private String numeroIdentificacion;

    @Column(name = "gender", length = 20)
    private String genero;
    
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String rol; // Valores: "USER", "ADMIN"

    // ⭐ NUEVO: Campo para activar/desactivar usuarios
    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "remember_token")
    private String rememberToken;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ⭐ NUEVO: Información de auditoría
    @Column(name = "updated_by")
    private String updatedBy; // Email del admin que hizo el último cambio
}