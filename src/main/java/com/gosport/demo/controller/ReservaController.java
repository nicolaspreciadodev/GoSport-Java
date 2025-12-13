package com.gosport.demo.controller;

import com.gosport.demo.model.Reserva;
import com.gosport.demo.model.Cancha;
import com.gosport.demo.model.User;
import com.gosport.demo.service.EmailService;
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
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reservas")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private EmailService emailService;

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
            User usuario = userRepository.findByEmail(authentication.getName());
            if (usuario == null) {
                throw new RuntimeException("Usuario no encontrado");
            }

            Cancha cancha = canchaRepository.findById(canchaId)
                .orElseThrow(() -> new RuntimeException("Cancha no encontrada"));

            Reserva reserva = new Reserva();
            reserva.setUsuario(usuario);
            reserva.setCancha(cancha);
            reserva.setFecha(LocalDate.parse(fecha));
            reserva.setHoraInicio(LocalTime.parse(horaInicio));
            reserva.setDuracion(duracion);
            reserva.setPrecioTotal(precioTotal);
            
            // ⭐ CAMBIO: La reserva se crea directamente como CONFIRMADA (sin pasarela de pago)
            reserva.setEstado(Reserva.EstadoReserva.CONFIRMADA);
            reserva.setCreatedBy(usuario.getEmail());

            if (!reservaService.validarDisponibilidad(reserva)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    "El horario seleccionado no está disponible. Por favor elige otro.");
                return "redirect:/reservas/nueva/" + canchaId;
            }

            Reserva reservaGuardada = reservaService.guardar(reserva);

            // Intentar enviar correo de confirmación
            try {
                emailService.enviarEmailReservaConfirmada(reservaGuardada);
            } catch (Exception e) {
                System.err.println("Error enviando email de confirmación: " + e.getMessage());
            }

            // ⭐ CAMBIO: Redirigir al detalle de la reserva en lugar de a pagos
            redirectAttributes.addFlashAttribute("successMessage",
                "¡Reserva realizada con éxito! Te esperamos en la cancha.");
            return "redirect:/reservas/detalle/" + reservaGuardada.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Error al crear la reserva: " + e.getMessage());
            return "redirect:/reservas/nueva/" + canchaId; 
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

            List<Reserva> todasReservas = reservaService.obtenerPorUsuario(usuario.getId());

            if (estado != null && !estado.isEmpty()) {
                todasReservas = todasReservas.stream()
                    .filter(r -> r.getEstado().name().equals(estado))
                    .toList();
            }

            List<Reserva> reservasProximas = todasReservas.stream()
                .filter(Reserva::esProxima)
                .filter(Reserva::estaActiva)
                .toList();

            List<Reserva> reservasHistorial = todasReservas.stream()
                .filter(r -> !r.esProxima() || !r.estaActiva())
                .toList();

            long totalReservas = todasReservas.size();
            long pendientes = todasReservas.stream()
                .filter(r -> r.getEstado() == Reserva.EstadoReserva.PENDIENTE)
                .count();
            long confirmadas = todasReservas.stream()
                .filter(r -> r.getEstado() == Reserva.EstadoReserva.CONFIRMADA)
                .count();
            BigDecimal totalGastado = todasReservas.stream()
                .filter(r -> r.getEstado() == Reserva.EstadoReserva.CONFIRMADA)
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
    // CANCELAR RESERVA (CORREGIDO)
    // ====================================
    @PostMapping("/cancelar/{id}")
    public String cancelarReserva(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            Reserva reserva = reservaService.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

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

            if (!reserva.puedeSerCancelada()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    "Esta reserva no puede ser cancelada");
                return "redirect:/reservas/detalle/" + id;
            }

            // 1. Realizar la cancelación en BD primero
            reserva.setEstado(Reserva.EstadoReserva.CANCELADA);
            reserva.setUpdatedBy(usuario.getEmail());
            reservaService.guardar(reserva);

            // 2. Intentar enviar correo con manejo de errores específico
            try {
                emailService.enviarEmailReservaCancelada(reserva);
                redirectAttributes.addFlashAttribute("successMessage",
                    "Reserva cancelada exitosamente. Te hemos enviado un email de confirmación.");
            } catch (Exception e) {
                // Si falla el correo, NO fallamos toda la petición. Mostramos éxito con advertencia.
                System.err.println("Error enviando email de cancelación: " + e.getMessage());
                redirectAttributes.addFlashAttribute("successMessage",
                    "Reserva cancelada exitosamente. (Nota: No se pudo enviar el correo de confirmación por un error de conexión)");
            }

            return "redirect:/reservas/detalle/" + id;

        } catch (Exception e) {
            // Este catch solo captura errores graves de lógica o base de datos
            redirectAttributes.addFlashAttribute("errorMessage",
                "Error al cancelar la reserva: " + e.getMessage());
            return "redirect:/reservas/mis-reservas";
        }
    }

    // ====================================
    // API: HORARIOS OCUPADOS (PÚBLICO)
    // ====================================
    @GetMapping("/api/horarios-ocupados")
    @ResponseBody
    public List<Map<String, String>> obtenerHorariosOcupados(
            @RequestParam Long canchaId,
            @RequestParam String fecha) {
        
        try {
            LocalDate fechaReserva = LocalDate.parse(fecha);
            
            List<Reserva> reservas = reservaService.obtenerPorCanchaYFecha(canchaId, fechaReserva)
                .stream()
                .filter(r -> r.getEstado() != Reserva.EstadoReserva.CANCELADA)
                .toList();
            
            return reservas.stream()
                .map(r -> {
                    Map<String, String> horario = new HashMap<>();
                    horario.put("inicio", r.getHoraInicio().toString());
                    horario.put("fin", r.getHoraFin().toString()); 
                    return horario;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}