package com.gosport.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
@Data
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ⭐ CAMBIADO: FetchType.LAZY → FetchType.EAGER
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    // ⭐ CAMBIADO: FetchType.LAZY → FetchType.EAGER
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cancha_id", nullable = false)
    private Cancha cancha;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(nullable = false)
    private Double duracion; // En horas (0.5, 1, 1.5, 2, etc.)

    @Column(name = "precio_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    @Column(name = "codigo_reserva", unique = true)
    private String codigoReserva;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // Generar código único de reserva
        if (codigoReserva == null) {
            codigoReserva = "RES-" + System.currentTimeMillis();
        }
        
        // Calcular hora fin
        if (horaInicio != null && duracion != null) {
            int minutos = (int) (duracion * 60);
            horaFin = horaInicio.plusMinutes(minutos);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Enum para Estados
    public enum EstadoReserva {
        PENDIENTE("Pendiente"),
        CONFIRMADA("Confirmada"),
        CANCELADA("Cancelada"),
        COMPLETADA("Completada");

        private final String displayName;

        EstadoReserva(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Métodos de utilidad
    public boolean puedeSerCancelada() {
        return estado == EstadoReserva.PENDIENTE || estado == EstadoReserva.CONFIRMADA;
    }

    public boolean estaActiva() {
        return estado == EstadoReserva.PENDIENTE || estado == EstadoReserva.CONFIRMADA;
    }

    public boolean esProxima() {
        LocalDate hoy = LocalDate.now();
        return fecha.isAfter(hoy) || fecha.isEqual(hoy);
    }
}