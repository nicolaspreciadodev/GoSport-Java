package com.gosport.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.gosport.demo.model.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    
    // Buscar por email (login)
    User findByEmail(String email);
    
    // Buscar por nombre o email (panel admin) - SIN PAGINACIÓN
    List<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
    
    // ⭐ NUEVO: Buscar con paginación
    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String name, String email, Pageable pageable);
    
    // ⭐ NUEVO: Obtener todos con paginación
    Page<User> findAll(Pageable pageable);
}