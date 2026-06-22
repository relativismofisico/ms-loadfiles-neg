package com.loadfilesservice.loadfiles.infraestrutura.persistence;

import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.Sign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio JPA para la entidad Sign. */
public interface ISignDao extends JpaRepository<Sign, Long> {

    /** Busca la firma activa de una empresa. */
    @Query("SELECT s FROM Sign s WHERE s.company = :company AND s.state = 1")
    Optional<Sign> findActiveSignByCompany(@Param("company") Long company);

    /** Busca la firma activa de un usuario. */
    @Query("SELECT s FROM Sign s WHERE s.user = :user AND s.state = 1")
    Optional<Sign> findActiveSignByUser(@Param("user") Long user);

}
