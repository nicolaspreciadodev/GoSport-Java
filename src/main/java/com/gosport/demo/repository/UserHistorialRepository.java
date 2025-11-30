package com.gosport.demo.repository;

import com.gosport.demo.model.UserHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserHistorialRepository extends JpaRepository<UserHistorial, Long> {
    
    // Buscar historial de un usuario específico
    List<UserHistorial> findByUserIdOrderByFechaModificacionDesc(Long userId);
}