package com.gosport.demo.controller.Admin;

import com.gosport.demo.model.Reserva;
import com.gosport.demo.service.ReservaService;
import com.gosport.demo.repository.CanchaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/reservas")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReservaController {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private CanchaRepository canchaRepository;

    // ====================================
    // LISTAR TODAS LAS RESERVAS
    // ====================================
    @GetMapping
    public String listarReservas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long canchaId,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            Model model) {
        
        try {
            // Configurar paginación
            Pageable pageable = PageRequest.of(page, size, 
                Sort.by("fecha").descending()
                    .and(Sort.by("horaInicio").descending()));
            
            // Aplicar filtros
            LocalDate fechaDesde = desde != null && !desde.isEmpty() ? 
                LocalDate.parse(desde) : null;
            LocalDate fechaHasta = hasta != null && !hasta.isEmpty() ? 
                LocalDate.parse(hasta) : null;
            
            Page<Reserva> paginaReservas = reservaService.buscarConFiltros(
                estado, canchaId, fechaDesde, fechaHasta, pageable
            );
            
            // Calcular estadísticas
            long totalReservas = reservaService.contarTotalReservas();
            long reservasHoy = reservaService.contarReservasHoy();
            long reservasPendientes = reservaService.contarPorEstado(
                Reserva.EstadoReserva.PENDIENTE
            );
            
            // Calcular ingresos del mes
            LocalDate hoy = LocalDate.now();
            BigDecimal ingresosMes = reservaService.calcularIngresosMes(
                hoy.getMonthValue(), 
                hoy.getYear()
            );
            
            // Agregar atributos al modelo
            model.addAttribute("reservas", paginaReservas.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", paginaReservas.getTotalPages());
            model.addAttribute("totalItems", paginaReservas.getTotalElements());
            model.addAttribute("pageSize", size);
            
            model.addAttribute("totalReservas", totalReservas);
            model.addAttribute("reservasHoy", reservasHoy);
            model.addAttribute("reservasPendientes", reservasPendientes);
            model.addAttribute("ingresosMes", ingresosMes);
            
            // Filtros
            model.addAttribute("estadoFiltro", estado);
            model.addAttribute("canchaIdFiltro", canchaId);
            model.addAttribute("fechaDesde", desde);
            model.addAttribute("fechaHasta", hasta);
            
            // Lista de canchas para el filtro
            model.addAttribute("canchas", canchaRepository.findAll());
            
            return "admin/reservas/lista-reserva";
            
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error al cargar reservas: " + e.getMessage());
            return "admin/reservas/lista-reserva";
        }
    }

    // ====================================
    // CAMBIAR ESTADO DE RESERVA
    // ====================================
    @GetMapping("/cambiar-estado/{id}")
    public String cambiarEstado(
            @PathVariable Long id,
            @RequestParam String estado,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            Reserva.EstadoReserva nuevoEstado = Reserva.EstadoReserva.valueOf(estado);
            
            reservaService.cambiarEstado(id, nuevoEstado, authentication.getName());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Estado de la reserva actualizado a: " + nuevoEstado.getDisplayName());
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Estado inválido: " + estado);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error al cambiar el estado: " + e.getMessage());
        }
        
        return "redirect:/admin/reservas";
    }

    // ====================================
    // ELIMINAR RESERVA
    // ====================================
    @GetMapping("/eliminar/{id}")
    public String eliminarReserva(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        
        try {
            Reserva reserva = reservaService.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
            
            reservaService.eliminar(id);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Reserva eliminada exitosamente");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error al eliminar la reserva: " + e.getMessage());
        }
        
        return "redirect:/admin/reservas";
    }

    // ====================================
    // EXPORTAR A EXCEL
    // ====================================
    @GetMapping("/exportar/excel")
    public void exportarExcel(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long canchaId,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            jakarta.servlet.http.HttpServletResponse response) {
        
        try {
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", 
                "attachment; filename=reservas_" + LocalDate.now() + ".xlsx");
            
            // Aquí implementarías la lógica de exportación con Apache POI
            // Por ahora, solo un placeholder
            
            // List<Reserva> reservas = obtener reservas con filtros...
            // ExcelExporter.exportar(reservas, response.getOutputStream());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====================================
    // EXPORTAR A PDF
    // ====================================
    @GetMapping("/exportar/pdf")
    public void exportarPDF(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long canchaId,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            jakarta.servlet.http.HttpServletResponse response) {
        
        try {
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", 
                "attachment; filename=reservas_" + LocalDate.now() + ".pdf");
            
            // Aquí implementarías la lógica de exportación con iText o similar
            // Por ahora, solo un placeholder
            
            // List<Reserva> reservas = obtener reservas con filtros...
            // PDFExporter.exportar(reservas, response.getOutputStream());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ====================================
    // DASHBOARD DE RESERVAS (OPCIONAL)
    // ====================================
    @GetMapping("/dashboard")
    public String dashboardReservas(Model model) {
        try {
            // Estadísticas generales
            long totalReservas = reservaService.contarTotalReservas();
            long reservasPendientes = reservaService.contarPorEstado(
                Reserva.EstadoReserva.PENDIENTE
            );
            long reservasConfirmadas = reservaService.contarPorEstado(
                Reserva.EstadoReserva.CONFIRMADA
            );
            long reservasCanceladas = reservaService.contarPorEstado(
                Reserva.EstadoReserva.CANCELADA
            );
            
            // Ingresos
            BigDecimal ingresosTotales = reservaService.calcularIngresosTotales();
            LocalDate hoy = LocalDate.now();
            BigDecimal ingresosMes = reservaService.calcularIngresosMes(
                hoy.getMonthValue(), 
                hoy.getYear()
            );
            
            // Agregar al modelo
            model.addAttribute("totalReservas", totalReservas);
            model.addAttribute("reservasPendientes", reservasPendientes);
            model.addAttribute("reservasConfirmadas", reservasConfirmadas);
            model.addAttribute("reservasCanceladas", reservasCanceladas);
            model.addAttribute("ingresosTotales", ingresosTotales);
            model.addAttribute("ingresosMes", ingresosMes);
            
            // Reservas del año para gráficos
            List<java.util.Map<String, Object>> reservasPorMes = 
                reservaService.obtenerReservasPorMes(hoy.getYear());
            model.addAttribute("reservasPorMes", reservasPorMes);
            
            return "admin/reservas/dashboard-reservas";
            
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error al cargar el dashboard: " + e.getMessage());
            return "admin/reservas/dashboard-reservas";
        }
    }
}