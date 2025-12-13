package com.gosport.demo.controller;

import com.gosport.demo.model.User;
import com.gosport.demo.model.UserHistorial;
import com.gosport.demo.repository.UserRepository;
import com.gosport.demo.repository.UserHistorialRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
// Importación adicional necesaria para el manejo de excepciones de integridad de datos
import org.springframework.dao.DataIntegrityViolationException;


@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final UserHistorialRepository historialRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.gosport.demo.service.PdfExportService pdfExportService;

    public AdminController(UserRepository userRepository, 
                            UserHistorialRepository historialRepository,
                            PasswordEncoder passwordEncoder,
                            com.gosport.demo.service.PdfExportService pdfExportService) {
        this.userRepository = userRepository;
        this.historialRepository = historialRepository;
        this.passwordEncoder = passwordEncoder;
        this.pdfExportService = pdfExportService;
    }

    // ===============================
    // 1. LISTAR USUARIOS CON PAGINACIÓN
    // ===============================
    @GetMapping("/usuarios")
    public String listarUsuarios(
            Model model,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> usuariosPage;
        
        if (search != null && !search.trim().isEmpty()) {
            usuariosPage = userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                search, search, pageable);
            model.addAttribute("search", search);
        } else {
            usuariosPage = userRepository.findAll(pageable);
        }
        
        model.addAttribute("usuarios", usuariosPage.getContent());
        model.addAttribute("totalUsuarios", usuariosPage.getTotalElements());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usuariosPage.getTotalPages());
        model.addAttribute("pageSize", size);
        
        return "admin/usuarios";
    }

    // ===============================
    // 2. CREAR NUEVO USUARIO
    // ===============================
    @GetMapping("/usuarios/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        User usuario = new User();
        usuario.setActivo(true); // Por defecto activo
        model.addAttribute("usuario", usuario);
        model.addAttribute("accion", "Crear");
        return "admin/form-usuario";
    }

    // ===============================
    // 3. GUARDAR USUARIO (CON AUDITORÍA) ⭐ MODIFICADO
    // ===============================
    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(
            @ModelAttribute User usuario,
            @RequestParam(value = "passwordPlain", required = false) String passwordPlain,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            String adminEmail = auth.getName();
            User usuarioAnterior = null;
            
            if (usuario.getId() == null) {
                // ============ CREAR NUEVO ============
                usuario.setCreatedAt(LocalDateTime.now());
                usuario.setActivo(true); // Se asegura que esté activo al crear
                
                if (passwordPlain != null && !passwordPlain.isEmpty()) {
                    usuario.setPassword(passwordEncoder.encode(passwordPlain));
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage", "La contraseña es obligatoria.");
                    return "redirect:/admin/usuarios/nuevo";
                }
                
                // CRÍTICO: GUARDAR PRIMERO para obtener el ID y registrar historial
                usuario.setUpdatedAt(LocalDateTime.now());
                usuario.setUpdatedBy(adminEmail);
                User usuarioGuardado = userRepository.save(usuario);
                
                // AHORA SÍ registrar en historial (el usuario ya tiene ID)
                registrarHistorial(null, usuarioGuardado, "CREADO", null, null, null, adminEmail);
                
            } else {
                // ============ EDITAR EXISTENTE ============
                usuarioAnterior = userRepository.findById(usuario.getId()).orElseThrow();
                
                if (passwordPlain != null && !passwordPlain.isEmpty()) {
                    usuario.setPassword(passwordEncoder.encode(passwordPlain));
                    registrarHistorial(usuarioAnterior, usuario, "EDITADO", "password", "***", "***", adminEmail);
                } else {
                    usuario.setPassword(usuarioAnterior.getPassword());
                }
                
                // Detectar cambios antes de guardar
                detectarCambios(usuarioAnterior, usuario, adminEmail);

                // Conservar la fecha de creación original
                usuario.setCreatedAt(usuarioAnterior.getCreatedAt()); 
                usuario.setUpdatedAt(LocalDateTime.now());
                usuario.setUpdatedBy(adminEmail);
                userRepository.save(usuario);
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Usuario guardado correctamente.");
            return "redirect:/admin/usuarios";
            
        } catch (Exception e) {
             e.printStackTrace(); // Es bueno para debug
            redirectAttributes.addFlashAttribute("errorMessage", "Error al guardar el usuario: " + e.getMessage());
            // Si es un error al editar, redirige al listado; si es al crear, puede ser mejor devolver al formulario, pero por simplicidad vamos al listado.
            return "redirect:/admin/usuarios"; 
        }
    }

    // ===============================
    // 4. EDITAR USUARIO
    // ===============================
    @GetMapping("/usuarios/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        User usuario = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        model.addAttribute("usuario", usuario);
        model.addAttribute("accion", "Editar");
        return "admin/form-usuario";
    }

    // ===============================
    // 5. ELIMINAR USUARIO (CON MANEJO DE RESERVAS) ⭐ MODIFICADO
    // ===============================
    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, 
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        try {
            User usuario = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            String adminEmail = auth.getName();
            
            try {
                // Intenta eliminar el historial primero (si el UserHistorial tiene FK a User, debe eliminarse primero)
                // Nota: Asumo que UserHistorialRepository tiene el método deleteByUserId(Long)
                historialRepository.deleteByUserId(id); 
                
                // Intenta eliminar el usuario
                userRepository.deleteById(id);
                
                // Registrar eliminación en historial (aunque ya no se guarda, sirve como log del evento)
                registrarHistorial(usuario, null, "ELIMINADO", null, null, null, adminEmail);
                
                redirectAttributes.addFlashAttribute("successMessage", "Usuario eliminado correctamente.");
            } catch (DataIntegrityViolationException e) {
                // Si la eliminación falla debido a una llave foránea (e.g., reservas asociadas)
                
                // DESACTIVAR en lugar de eliminar
                usuario.setActivo(false);
                usuario.setUpdatedAt(LocalDateTime.now());
                usuario.setUpdatedBy(adminEmail);
                userRepository.save(usuario);
                
                // Registrar el cambio de estado en historial
                registrarHistorial(usuario, usuario, "DESACTIVADO", "activo", "true", "false", adminEmail);
                
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "No se pudo eliminar el usuario porque tiene datos asociados (ej: reservas). El usuario ha sido **DESACTIVADO** en su lugar.");
            }
            
        } catch (Exception e) {
             e.printStackTrace(); // Es bueno para debug
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        
        return "redirect:/admin/usuarios";
    }

    // ===============================
    // 6. CAMBIAR ESTADO (ACTIVAR/DESACTIVAR)
    // ===============================
    @GetMapping("/usuarios/cambiar-estado/{id}")
    public String cambiarEstado(@PathVariable Long id,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            User usuario = userRepository.findById(id).orElseThrow();
            String adminEmail = auth.getName();
            
            boolean nuevoEstado = !usuario.getActivo();
            String accion = nuevoEstado ? "ACTIVADO" : "DESACTIVADO";
            
            registrarHistorial(usuario, usuario, accion, "activo", 
                usuario.getActivo().toString(), String.valueOf(nuevoEstado), adminEmail);
            
            usuario.setActivo(nuevoEstado);
            usuario.setUpdatedAt(LocalDateTime.now());
            usuario.setUpdatedBy(adminEmail);
            userRepository.save(usuario);
            
            String mensaje = nuevoEstado ? "Usuario activado correctamente." : "Usuario desactivado correctamente.";
            redirectAttributes.addFlashAttribute("successMessage", mensaje);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        
        return "redirect:/admin/usuarios";
    }

    // ===============================
    // 7. VER HISTORIAL DE USUARIO
    // ===============================
    @GetMapping("/usuarios/historial/{id}")
    public String verHistorial(@PathVariable Long id, Model model) {
        User usuario = userRepository.findById(id).orElseThrow();
        List<UserHistorial> historial = historialRepository.findByUserIdOrderByFechaModificacionDesc(id);
        
        model.addAttribute("usuario", usuario);
        model.addAttribute("historial", historial);
        
        return "admin/historial-usuario";
    }

    // ===============================
    // 8. EXPORTAR A EXCEL
    // ===============================
    @GetMapping("/usuarios/exportar/excel")
    public void exportarExcel(HttpServletResponse response) throws IOException {
        List<User> usuarios = userRepository.findAll();
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Usuarios");
        
        // Encabezados
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Nombre", "Email", "Teléfono", "Documento", "Rol", "Estado", "Fecha Registro"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
        
        // Datos
        int rowNum = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (User u : usuarios) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(u.getId());
            row.createCell(1).setCellValue(u.getName());
            row.createCell(2).setCellValue(u.getEmail());
            row.createCell(3).setCellValue(u.getTelefono() != null ? u.getTelefono() : "");
            row.createCell(4).setCellValue(u.getTipoDocumento() + " " + u.getNumeroIdentificacion());
            row.createCell(5).setCellValue(u.getRol());
            row.createCell(6).setCellValue(u.getActivo() ? "Activo" : "Inactivo");
            row.createCell(7).setCellValue(u.getCreatedAt().format(formatter));
        }
        
        // Configurar respuesta
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=usuarios.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    // ===============================
    // 9. EXPORTAR A PDF
    // ===============================
    @GetMapping("/usuarios/exportar/pdf")
    public void exportarPdf(HttpServletResponse response) throws Exception {
        List<User> usuarios = userRepository.findAll();
        
        // Generar PDF usando el servicio
        java.io.ByteArrayOutputStream baos = pdfExportService.exportarUsuariosPdf(usuarios);
        
        // Configurar respuesta
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=usuarios-gosports.pdf");
        response.getOutputStream().write(baos.toByteArray());
        response.getOutputStream().flush();
    }

    // ===============================
    // MÉTODOS AUXILIARES ⭐ CORREGIDO
    // ===============================
    private void registrarHistorial(User anterior, User nuevo, String accion, 
                                     String campo, String valorAnterior, String valorNuevo, 
                                     String modificadoPor) {
        UserHistorial historial = new UserHistorial();
        
        // CRÍTICO: Debe obtener el ID del usuario que se está modificando. 
        // Si 'nuevo' es null (en caso de eliminación), usa 'anterior'.
        Long userId = nuevo != null ? nuevo.getId() : anterior.getId();

        // Si el usuario se está creando, el ID se obtiene de 'nuevo', que ya fue guardado.
        if (userId == null) {
            // Este caso NO debería ocurrir después del cambio en guardarUsuario, 
            // pero si sucede, evita un NullPointerException.
            System.err.println("Advertencia: Intentando registrar historial sin ID de usuario.");
            return;
        }

        historial.setUserId(userId);
        historial.setAccion(accion);
        historial.setCampoModificado(campo);
        historial.setValorAnterior(valorAnterior);
        historial.setValorNuevo(valorNuevo);
        historial.setModificadoPor(modificadoPor);
        historial.setFechaModificacion(LocalDateTime.now());
        historialRepository.save(historial);
    }

    private void detectarCambios(User anterior, User nuevo, String adminEmail) {
        if (!anterior.getName().equals(nuevo.getName())) {
            registrarHistorial(anterior, nuevo, "EDITADO", "name", anterior.getName(), nuevo.getName(), adminEmail);
        }
        if (!anterior.getEmail().equals(nuevo.getEmail())) {
            registrarHistorial(anterior, nuevo, "EDITADO", "email", anterior.getEmail(), nuevo.getEmail(), adminEmail);
        }
        if (!anterior.getRol().equals(nuevo.getRol())) {
            registrarHistorial(anterior, nuevo, "EDITADO", "rol", anterior.getRol(), nuevo.getRol(), adminEmail);
        }
        // Nota: El cambio de estado 'activo' se maneja por separado en cambiarEstado, 
        // pero es bueno tener la detección aquí también, en caso de que se cambie 
        // desde el formulario de edición.
        if (!anterior.getActivo().equals(nuevo.getActivo())) {
             registrarHistorial(anterior, nuevo, "EDITADO", "activo", 
                 anterior.getActivo().toString(), nuevo.getActivo().toString(), adminEmail);
        }
        // Puedes añadir más campos (Teléfono, Documento, etc.) aquí si lo deseas
    }
}