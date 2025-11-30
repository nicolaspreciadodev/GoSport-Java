package com.gosport.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_historial")
@Data
public class UserHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // ID del usuario modificado

    @Column(name = "accion", nullable = false, length = 50)
    private String accion; // "CREADO", "EDITADO", "ACTIVADO", "DESACTIVADO", "ELIMINADO"

    @Column(name = "campo_modificado", length = 100)
    private String campoModificado; // Ejemplo: "email", "rol", "activo"

    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "TEXT")
    private String valorNuevo;

    @Column(name = "modificado_por", nullable = false)
    private String modificadoPor; // Email del admin que hizo el cambio

    @Column(name = "fecha_modificacion", nullable = false)
    private LocalDateTime fechaModificacion;
}