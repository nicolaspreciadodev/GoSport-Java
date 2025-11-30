package com.gosport.demo.controller;

import com.gosport.demo.model.User;
import com.gosport.demo.service.UserService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ------------------- REGISTRO -------------------

    @GetMapping("/registro")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/registro")
    public String registerUser(
            @RequestParam String nombre,
            @RequestParam String apellidos,
            @RequestParam String telefono,
            @RequestParam(name = "tipo_documento") String tipoDocumento,
            @RequestParam(name = "numero_identificacion") String numeroIdentificacion,
            @RequestParam String genero,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(name = "password_confirmation") String passwordConfirmation,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        // Verificar que las contraseñas coincidan
        if (!password.equals(passwordConfirmation)) {
            model.addAttribute("errorMessage", "Las contraseñas no coinciden.");
            return "register";
        }
        
        try {
            User newUser = new User();
            
            // Asignar campos
            newUser.setName(nombre + " " + apellidos);
            newUser.setEmail(email);
            newUser.setPassword(password); // Se cifrará en el servicio
            newUser.setTelefono(telefono);
            newUser.setTipoDocumento(tipoDocumento);
            newUser.setNumeroIdentificacion(numeroIdentificacion);
            newUser.setGenero(genero);
            newUser.setRol("USER"); // ⭐ Por defecto todos son USER
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());

            // Guardar usuario (la contraseña se cifrará en UserServiceImpl)
            userService.saveUser(newUser);

            // Éxito: Redirigir al login con mensaje
            redirectAttributes.addFlashAttribute("successMessage", 
                "¡Registro exitoso! Ya puedes iniciar sesión.");
            return "redirect:/login";
            
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", 
                "Error: El correo electrónico o número de identificación ya están registrados.");
            return "register";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error interno del servidor.");
            return "register";
        }
    }
}