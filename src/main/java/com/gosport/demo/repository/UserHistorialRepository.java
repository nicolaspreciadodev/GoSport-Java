package com.gosport.demo.repository;

import com.gosport.demo.model.UserHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserHistorialRepository extends JpaRepository<UserHistorial, Long> {
    
    // Buscar historial de un usuario específico
    List<UserHistorial> findByUserIdOrderByFechaModificacionDesc(Long userId);
    
    // ⭐ NUEVO: Eliminar historial por userId (para poder eliminar el usuario)
    @Modifying
    @Transactional
    @Query("DELETE FROM UserHistorial h WHERE h.userId = :userId")
    void deleteByUserId(Long userId);
}