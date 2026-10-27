package com.udea.reservas.plataformareservas.usuarios.repository;

import com.udea.reservas.plataformareservas.usuarios.model.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    // Las tres operaciones siguientes son UPDATE atómicos en la base de datos:
    // varias solicitudes simultáneas no pierden intentos por una lectura obsoleta.

    @Modifying
    @Query(
        "update User u set u.failedLoginAttempts = u.failedLoginAttempts + 1 " +
        "where lower(u.email) = lower(:email)"
    )
    int incrementFailedLoginAttempts(@Param("email") String email);

    @Modifying
    @Query(
        "update User u set u.lockedUntil = :until, u.failedLoginAttempts = 0 " +
        "where lower(u.email) = lower(:email) and u.failedLoginAttempts >= :max"
    )
    int lockWhenFailedAttemptsReach(
        @Param("email") String email,
        @Param("max") int max,
        @Param("until") LocalDateTime until
    );

    @Modifying
    @Query(
        "update User u set u.failedLoginAttempts = 0, u.lockedUntil = null " +
        "where lower(u.email) = lower(:email) " +
        "and (u.failedLoginAttempts > 0 or u.lockedUntil is not null)"
    )
    int clearFailedLoginAttempts(@Param("email") String email);
}
