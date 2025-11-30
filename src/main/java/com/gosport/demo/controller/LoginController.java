package com.gosport.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    /**
     * Muestra la vista de login.
     * Spring Security maneja automáticamente el POST a /login.
     */
    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "denied", required = false) String denied,
            @RequestParam(value = "expired", required = false) String expired,
            Model model) {

        if (error != null) {
            model.addAttribute("loginError", "Credenciales incorrectas. Verifique su email y contraseña.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Sesión cerrada correctamente.");
        }
        if (denied != null) {
            model.addAttribute("deniedMessage", "Acceso denegado. No tienes permisos para ver esa página.");
        }
        if (expired != null) {
            model.addAttribute("expiredMessage", "Tu sesión ha expirado. Por favor, inicia sesión de nuevo.");
        }

        return "login";
    }
}