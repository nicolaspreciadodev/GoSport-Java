package com.gosport.demo.repository;

import com.gosport.demo.model.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // ====================================
    // CONSULTAS BÁSICAS
    // ====================================
    
    List<Reserva> findByUsuarioId(Long usuarioId);
    
    List<Reserva> findByCanchaId(Long canchaId);
    
    List<Reserva> findByFecha(LocalDate fecha);
    
    List<Reserva> findByEstado(Reserva.EstadoReserva estado);
    
    Page<Reserva> findByEstado(Reserva.EstadoReserva estado, Pageable pageable);
    
    // ====================================
    // CONSULTAS COMBINADAS
    // ====================================
    
    List<Reserva> findByCanchaIdAndFecha(Long canchaId, LocalDate fecha);
    
    List<Reserva> findByUsuarioIdAndFechaGreaterThanEqual(Long usuarioId, LocalDate fecha);
    
    List<Reserva> findByFechaBeforeAndEstadoIn(LocalDate fecha, List<Reserva.EstadoReserva> estados);
    
    Page<Reserva> findByCanchaId(Long canchaId, Pageable pageable);
    
    Page<Reserva> findByFechaBetween(LocalDate fechaDesde, LocalDate fechaHasta, Pageable pageable);
    
    // ====================================
    // FILTROS AVANZADOS
    // ====================================
    
    Page<Reserva> findByEstadoAndCanchaId(
        Reserva.EstadoReserva estado, 
        Long canchaId, 
        Pageable pageable
    );
    
    Page<Reserva> findByEstadoAndCanchaIdAndFechaBetween(
        Reserva.EstadoReserva estado,
        Long canchaId,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        Pageable pageable
    );
    
    // ====================================
    // VALIDACIONES
    // ====================================
    
    @Query("SELECT COUNT(r) > 0 FROM Reserva r WHERE r.cancha.id = :canchaId " +
           "AND r.fecha = :fecha " +
           "AND r.estado != 'CANCELADA' " +
           "AND ((r.horaInicio < :horaFin AND r.horaFin > :horaInicio))")
    boolean existeSolapamiento(
        @Param("canchaId") Long canchaId,
        @Param("fecha") LocalDate fecha,
        @Param("horaInicio") java.time.LocalTime horaInicio,
        @Param("horaFin") java.time.LocalTime horaFin
    );
    
    // ====================================
    // ESTADÍSTICAS - CONTEOS
    // ====================================
    
    long countByEstado(Reserva.EstadoReserva estado);
    
    long countByFecha(LocalDate fecha);
    
    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.usuario.id = :usuarioId")
    long countByUsuarioId(@Param("usuarioId") Long usuarioId);
    
    // ====================================
    // ESTADÍSTICAS - INGRESOS
    // ====================================
    
    @Query("SELECT COALESCE(SUM(r.precioTotal), 0) FROM Reserva r " +
           "WHERE MONTH(r.fecha) = :mes AND YEAR(r.fecha) = :anio " +
           "AND r.estado != 'CANCELADA'")
    BigDecimal calcularIngresosMensual(
        @Param("mes") int mes, 
        @Param("anio") int anio
    );
    
    @Query("SELECT COALESCE(SUM(r.precioTotal), 0) FROM Reserva r " +
           "WHERE r.estado != 'CANCELADA'")
    BigDecimal calcularIngresosTotales();
    
    @Query("SELECT COALESCE(SUM(r.precioTotal), 0) FROM Reserva r " +
           "WHERE r.usuario.id = :usuarioId AND r.estado != 'CANCELADA'")
    BigDecimal calcularGastosTotalesPorUsuario(@Param("usuarioId") Long usuarioId);
    
    // ====================================
    // REPORTES Y DASHBOARDS
    // ====================================
    
    @Query("SELECT " +
           "MONTH(r.fecha) as mes, " +
           "COUNT(r) as cantidad " +
           "FROM Reserva r " +
           "WHERE YEAR(r.fecha) = :anio " +
           "GROUP BY MONTH(r.fecha) " +
           "ORDER BY MONTH(r.fecha)")
    List<Map<String, Object>> contarReservasPorMes(@Param("anio") int anio);
    
    @Query("SELECT " +
           "c.nombre as cancha, " +
           "COUNT(r) as cantidad " +
           "FROM Reserva r " +
           "JOIN r.cancha c " +
           "WHERE r.estado != 'CANCELADA' " +
           "GROUP BY c.id, c.nombre " +
           "ORDER BY COUNT(r) DESC")
    List<Map<String, Object>> obtenerCanchasMasReservadas();
    
    @Query("SELECT " +
           "r.estado as estado, " +
           "COUNT(r) as cantidad " +
           "FROM Reserva r " +
           "GROUP BY r.estado")
    List<Map<String, Object>> contarPorEstado();
    
    @Query("SELECT " +
           "u.name as usuario, " +
           "COUNT(r) as cantidad, " +
           "SUM(r.precioTotal) as total " +
           "FROM Reserva r " +
           "JOIN r.usuario u " +
           "WHERE r.estado != 'CANCELADA' " +
           "GROUP BY u.id, u.name " +
           "ORDER BY COUNT(r) DESC")
    List<Map<String, Object>> obtenerUsuariosConMasReservas();
    
    // ====================================
    // BÚSQUEDAS
    // ====================================
    
    @Query("SELECT r FROM Reserva r " +
           "WHERE r.codigoReserva = :codigo")
    Reserva findByCodigoReserva(@Param("codigo") String codigo);
    
    @Query("SELECT r FROM Reserva r " +
           "WHERE r.usuario.id = :usuarioId " +
           "AND r.fecha >= :fechaInicio " +
           "AND r.fecha <= :fechaFin " +
           "ORDER BY r.fecha DESC, r.horaInicio DESC")
    List<Reserva> buscarReservasUsuarioEnRango(
        @Param("usuarioId") Long usuarioId,
        @Param("fechaInicio") LocalDate fechaInicio,
        @Param("fechaFin") LocalDate fechaFin
    );
    
    // ====================================
    // DISPONIBILIDAD
    // ====================================
    
    @Query("SELECT r FROM Reserva r " +
           "WHERE r.cancha.id = :canchaId " +
           "AND r.fecha = :fecha " +
           "AND r.estado IN ('PENDIENTE', 'CONFIRMADA') " +
           "ORDER BY r.horaInicio")
    List<Reserva> obtenerReservasActivasPorCanchaYFecha(
        @Param("canchaId") Long canchaId,
        @Param("fecha") LocalDate fecha
    );
    
    // ====================================
    // RECORDATORIOS Y NOTIFICACIONES
    // ====================================
    
    @Query("SELECT r FROM Reserva r " +
           "WHERE r.fecha = :fecha " +
           "AND r.estado = 'CONFIRMADA' " +
           "AND r.horaInicio > :horaActual")
    List<Reserva> obtenerReservasParaRecordarHoy(
        @Param("fecha") LocalDate fecha,
        @Param("horaActual") java.time.LocalTime horaActual
    );
    
    @Query("SELECT r FROM Reserva r " +
           "WHERE r.fecha = :manana " +
           "AND r.estado = 'CONFIRMADA'")
    List<Reserva> obtenerReservasParaManana(@Param("manana") LocalDate manana);
}