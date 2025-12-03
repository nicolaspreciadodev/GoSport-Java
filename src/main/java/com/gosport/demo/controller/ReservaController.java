package com.gosport.demo.controller;

import com.gosport.demo.model.Reserva;
import com.gosport.demo.model.Cancha;
import com.gosport.demo.model.User;
import com.gosport.demo.service.ReservaService;
import com.gosport.demo.repository.CanchaRepository;
import com.gosport.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reservas")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private CanchaRepository canchaRepository;

    @Autowired
    private UserRepository userRepository;

    // ====================================
    // FORMULARIO DE RESERVA
    // ====================================
    @GetMapping("/nueva/{canchaId}")
    public String mostrarFormularioReserva(
            @PathVariable Long canchaId,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            Cancha cancha = canchaRepository.findById(canchaId)
                .orElseThrow(() -> new RuntimeException("Cancha no encontrada"));
            
            model.addAttribute("cancha", cancha);
            return "reservas/formulario-reserva";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/canchas";
        }
    }

    // ====================================
    // CREAR RESERVA
    // ====================================
    @PostMapping("/crear")
    public String crearReserva(
            @RequestParam Long canchaId,
            @RequestParam String fecha,
            @RequestParam String horaInicio,
            @RequestParam Double duracion,
            @RequestParam BigDecimal precioTotal,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Obtener usuario autenticado
            User usuario = userRepository.findByEmail(authentication.getName());
            if (usuario == null) {
                throw new RuntimeException("Usuario no encontrado");
            }
            
            // Obtener cancha
            Cancha cancha = canchaRepository.findById(canchaId)
                .orElseThrow(() -> new RuntimeException("Cancha no encontrada"));
            
            // Crear reserva
            Reserva reserva = new Reserva();
            reserva.setUsuario(usuario);
            reserva.setCancha(cancha);
            reserva.setFecha(LocalDate.parse(fecha));
            reserva.setHoraInicio(LocalTime.parse(horaInicio));
            reserva.setDuracion(duracion);
            reserva.setPrecioTotal(precioTotal);
            reserva.setEstado(Reserva.EstadoReserva.PENDIENTE);
            reserva.setCreatedBy(usuario.getEmail());
            
            // Validar disponibilidad
            if (!reservaService.validarDisponibilidad(reserva)) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "El horario seleccionado no está disponible. Por favor elige otro.");
                return "redirect:/reservas/nueva/" + canchaId;
            }
            
            // Guardar reserva
            Reserva reservaGuardada = reservaService.guardar(reserva);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "¡Reserva creada exitosamente! Código: " + reservaGuardada.getCodigoReserva());
            return "redirect:/reservas/detalle/" + reservaGuardada.getId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error al crear la reserva: " + e.getMessage());
            return "redirect:/canchas/" + canchaId;
        }
    }

    // ====================================
    // MIS RESERVAS (Usuario)
    // ====================================
    @GetMapping("/mis-reservas")
    public String misReservas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            Authentication authentication,
            Model model) {
        
        try {
            User usuario = userRepository.findByEmail(authentication.getName());
            if (usuario == null) {
                throw new RuntimeException("Usuario no encontrado");
            }
            
            // Obtener todas las reservas del usuario
            List<Reserva> todasReservas = reservaService.obtenerPorUsuario(usuario.getId());
            
            // Aplicar filtros si existen
            if (estado != null && !estado.isEmpty()) {
                todasReservas = todasReservas.stream()
                    .filter(r -> r.getEstado().name().equals(estado))
                    .toList();
            }
            
            // Separar en próximas y historial
            List<Reserva> reservasProximas = todasReservas.stream()
                .filter(Reserva::esProxima)
                .filter(Reserva::estaActiva)
                .toList();
            
            List<Reserva> reservasHistorial = todasReservas.stream()
                .filter(r -> !r.esProxima() || !r.estaActiva())
                .toList();
            
            // Calcular estadísticas
            long totalReservas = todasReservas.size();
            long pendientes = todasReservas.stream()
                .filter(r -> r.getEstado() == Reserva.EstadoReserva.PENDIENTE)
                .count();
            long confirmadas = todasReservas.stream()
                .filter(r -> r.getEstado() == Reserva.EstadoReserva.CONFIRMADA)
                .count();
            BigDecimal totalGastado = todasReservas.stream()
                .map(Reserva::getPrecioTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            model.addAttribute("reservasProximas", reservasProximas);
            model.addAttribute("reservasHistorial", reservasHistorial);
            model.addAttribute("totalReservas", totalReservas);
            model.addAttribute("reservasPendientes", pendientes);
            model.addAttribute("reservasConfirmadas", confirmadas);
            model.addAttribute("totalGastado", totalGastado);
            model.addAttribute("estadoFiltro", estado);
            model.addAttribute("fechaDesde", desde);
            model.addAttribute("fechaHasta", hasta);
            
            return "reservas/mis-reservas";
            
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "reservas/mis-reservas";
        }
    }

    // ====================================
    // DETALLE DE RESERVA
    // ====================================
    @GetMapping("/detalle/{id}")
    public String detalleReserva(
            @PathVariable Long id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            Reserva reserva = reservaService.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
            
            // Verificar que la reserva pertenece al usuario autenticado o es admin
            User usuario = userRepository.findByEmail(authentication.getName());
            if (usuario == null) {
                throw new RuntimeException("Usuario no encontrado");
            }
            
            if (!reserva.getUsuario().getId().equals(usuario.getId()) && 
                !usuario.getRol().equals("ADMIN")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "No tienes permiso para ver esta reserva");
                return "redirect:/reservas/mis-reservas";
            }
            
            model.addAttribute("reserva", reserva);
            return "reservas/detalle-reserva";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reservas/mis-reservas";
        }
    }

    // ====================================
    // CANCELAR RESERVA
    // ====================================
    @PostMapping("/cancelar/{id}")
    public String cancelarReserva(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            Reserva reserva = reservaService.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
            
            // Verificar permisos
            User usuario = userRepository.findByEmail(authentication.getName());
            if (usuario == null) {
                throw new RuntimeException("Usuario no encontrado");
            }
            
            if (!reserva.getUsuario().getId().equals(usuario.getId()) && 
                !usuario.getRol().equals("ADMIN")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "No tienes permiso para cancelar esta reserva");
                return "redirect:/reservas/mis-reservas";
            }
            
            // Verificar que puede ser cancelada
            if (!reserva.puedeSerCancelada()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Esta reserva no puede ser cancelada");
                return "redirect:/reservas/detalle/" + id;
            }
            
            // Cancelar
            reserva.setEstado(Reserva.EstadoReserva.CANCELADA);
            reserva.setUpdatedBy(usuario.getEmail());
            reservaService.guardar(reserva);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Reserva cancelada exitosamente");
            return "redirect:/reservas/detalle/" + id;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error al cancelar la reserva: " + e.getMessage());
            return "redirect:/reservas/mis-reservas";
        }
    }

    // ====================================
    // API: HORARIOS OCUPADOS
    // ====================================
    @GetMapping("/api/horarios-ocupados")
    @ResponseBody
    public List<Map<String, String>> obtenerHorariosOcupados(
            @RequestParam Long canchaId,
            @RequestParam String fecha) {
        
        LocalDate fechaReserva = LocalDate.parse(fecha);
        return reservaService.obtenerHorariosOcupados(canchaId, fechaReserva);
    }
}