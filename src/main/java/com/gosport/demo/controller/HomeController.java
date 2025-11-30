package com.gosport.demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * Página de inicio pública (landing page)
     */
    @GetMapping("/")
    public String index() {
        return "index"; // Vista: index.html
    }

    /**
     * Página principal para usuarios autenticados
     */
    @GetMapping("/home")
    public String home(Model model, Authentication auth) {
        // Obtener el nombre del usuario autenticado
        String email = auth.getName();
        
        // Obtener los roles del usuario
        String roles = auth.getAuthorities().toString();
        
        model.addAttribute("userEmail", email);
        model.addAttribute("userRoles", roles);
        
        return "home"; // Vista: home.html
    }
}