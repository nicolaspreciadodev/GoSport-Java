package com.gosport.demo.service;

import com.gosport.demo.model.Reserva;
import com.gosport.demo.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;

    // ====================================
    // CRUD BÁSICO
    // ====================================
    
    public Reserva guardar(Reserva reserva) {
        return reservaRepository.save(reserva);
    }

    public Optional<Reserva> obtenerPorId(Long id) {
        return reservaRepository.findById(id);
    }

    public List<Reserva> obtenerTodas() {
        return reservaRepository.findAll();
    }

    public Page<Reserva> obtenerTodasPaginadas(Pageable pageable) {
        return reservaRepository.findAll(pageable);
    }

    public void eliminar(Long id) {
        reservaRepository.deleteById(id);
    }

    // ====================================
    // CONSULTAS ESPECÍFICAS
    // ====================================
    
    public List<Reserva> obtenerPorUsuario(Long usuarioId) {
        return reservaRepository.findByUsuarioId(usuarioId);
    }

    public List<Reserva> obtenerPorCancha(Long canchaId) {
        return reservaRepository.findByCanchaId(canchaId);
    }

    public List<Reserva> obtenerPorFecha(LocalDate fecha) {
        return reservaRepository.findByFecha(fecha);
    }

    public List<Reserva> obtenerPorEstado(Reserva.EstadoReserva estado) {
        return reservaRepository.findByEstado(estado);
    }

    public List<Reserva> obtenerPorCanchaYFecha(Long canchaId, LocalDate fecha) {
        return reservaRepository.findByCanchaIdAndFecha(canchaId, fecha);
    }

    public List<Reserva> obtenerProximasPorUsuario(Long usuarioId) {
        LocalDate hoy = LocalDate.now();
        return reservaRepository.findByUsuarioIdAndFechaGreaterThanEqual(usuarioId, hoy);
    }

    // ====================================
    // VALIDACIONES
    // ====================================
    
    public boolean validarDisponibilidad(Reserva nuevaReserva) {
        List<Reserva> reservasExistentes = reservaRepository
            .findByCanchaIdAndFecha(
                nuevaReserva.getCancha().getId(), 
                nuevaReserva.getFecha()
            );

        LocalTime nuevaHoraInicio = nuevaReserva.getHoraInicio();
        LocalTime nuevaHoraFin = nuevaReserva.getHoraFin();

        for (Reserva reserva : reservasExistentes) {
            // Ignorar reservas canceladas
            if (reserva.getEstado() == Reserva.EstadoReserva.CANCELADA) {
                continue;
            }

            // Si estamos editando, ignorar la reserva actual
            if (nuevaReserva.getId() != null && 
                reserva.getId().equals(nuevaReserva.getId())) {
                continue;
            }

            LocalTime horaInicio = reserva.getHoraInicio();
            LocalTime horaFin = reserva.getHoraFin();

            // Verificar solapamiento
            boolean seSolapan = 
                (nuevaHoraInicio.isBefore(horaFin) && nuevaHoraFin.isAfter(horaInicio)) ||
                (nuevaHoraInicio.equals(horaInicio)) ||
                (nuevaHoraFin.equals(horaFin));

            if (seSolapan) {
                return false;
            }
        }

        return true;
    }

    // ====================================
    // HORARIOS OCUPADOS (para el frontend)
    // ====================================
    
    public List<Map<String, String>> obtenerHorariosOcupados(Long canchaId, LocalDate fecha) {
        List<Reserva> reservas = reservaRepository.findByCanchaIdAndFecha(canchaId, fecha);
        
        return reservas.stream()
            .filter(r -> r.getEstado() != Reserva.EstadoReserva.CANCELADA)
            .map(r -> {
                Map<String, String> horario = new HashMap<>();
                horario.put("inicio", r.getHoraInicio().toString());
                horario.put("fin", r.getHoraFin().toString());
                return horario;
            })
            .collect(Collectors.toList());
    }

    // ====================================
    // ESTADÍSTICAS
    // ====================================
    
    public long contarTotalReservas() {
        return reservaRepository.count();
    }

    public long contarPorEstado(Reserva.EstadoReserva estado) {
        return reservaRepository.countByEstado(estado);
    }

    public long contarReservasHoy() {
        LocalDate hoy = LocalDate.now();
        return reservaRepository.countByFecha(hoy);
    }

    public BigDecimal calcularIngresosMes(int mes, int anio) {
        return reservaRepository.calcularIngresosMensual(mes, anio);
    }

    public BigDecimal calcularIngresosTotales() {
        return reservaRepository.calcularIngresosTotales();
    }

    public List<Map<String, Object>> obtenerReservasPorMes(int anio) {
        return reservaRepository.contarReservasPorMes(anio);
    }

    // ====================================
    // FILTROS AVANZADOS
    // ====================================
    
    public Page<Reserva> buscarConFiltros(
            String estado,
            Long canchaId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Pageable pageable) {
        
        if (estado != null && canchaId != null && fechaDesde != null && fechaHasta != null) {
            return reservaRepository.findByEstadoAndCanchaIdAndFechaBetween(
                Reserva.EstadoReserva.valueOf(estado), 
                canchaId, 
                fechaDesde, 
                fechaHasta, 
                pageable
            );
        } else if (estado != null && canchaId != null) {
            return reservaRepository.findByEstadoAndCanchaId(
                Reserva.EstadoReserva.valueOf(estado), 
                canchaId, 
                pageable
            );
        } else if (estado != null) {
            return reservaRepository.findByEstado(
                Reserva.EstadoReserva.valueOf(estado), 
                pageable
            );
        } else if (canchaId != null) {
            return reservaRepository.findByCanchaId(canchaId, pageable);
        } else if (fechaDesde != null && fechaHasta != null) {
            return reservaRepository.findByFechaBetween(fechaDesde, fechaHasta, pageable);
        } else {
            return reservaRepository.findAll(pageable);
        }
    }

    // ====================================
    // CAMBIO DE ESTADO
    // ====================================
    
    public Reserva cambiarEstado(Long reservaId, Reserva.EstadoReserva nuevoEstado, String modificadoPor) {
        Reserva reserva = obtenerPorId(reservaId)
            .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        
        reserva.setEstado(nuevoEstado);
        reserva.setUpdatedBy(modificadoPor);
        
        return guardar(reserva);
    }

    // ====================================
    // COMPLETAR RESERVAS AUTOMÁTICAMENTE
    // ====================================
    
    @Transactional
    public void completarReservasVencidas() {
        LocalDate ayer = LocalDate.now().minusDays(1);
        
        List<Reserva> reservasVencidas = reservaRepository
            .findByFechaBeforeAndEstadoIn(
                ayer, 
                List.of(
                    Reserva.EstadoReserva.PENDIENTE, 
                    Reserva.EstadoReserva.CONFIRMADA
                )
            );
        
        for (Reserva reserva : reservasVencidas) {
            reserva.setEstado(Reserva.EstadoReserva.COMPLETADA);
            reserva.setUpdatedBy("SYSTEM");
        }
        
        reservaRepository.saveAll(reservasVencidas);
    }
}