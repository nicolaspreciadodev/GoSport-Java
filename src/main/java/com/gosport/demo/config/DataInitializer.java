package com.gosport.demo.config;

import com.gosport.demo.model.User;
import com.gosport.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDatabase(UserRepository repo, PasswordEncoder passwordEncoder) {
        return args -> {
            // Verificar si ya existe el usuario admin
            if (repo.findByEmail("admin@gosport.com") == null) {
                User admin = new User();
                admin.setName("Administrador GoSport");
                admin.setEmail("admin@gosport.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRol("ADMIN");
                admin.setTelefono("3001234567");
                admin.setTipoDocumento("CC");
                admin.setNumeroIdentificacion("1234567890");
                admin.setGenero("Otro");
                admin.setCreatedAt(LocalDateTime.now());
                admin.setUpdatedAt(LocalDateTime.now());
                
                repo.save(admin);
                System.out.println("✅ Usuario ADMIN creado exitosamente");
                System.out.println("   Email: admin@gosport.com");
                System.out.println("   Contraseña: admin123");
            } else {
                System.out.println("ℹ️ Usuario ADMIN ya existe");
            }
        };
    }
}