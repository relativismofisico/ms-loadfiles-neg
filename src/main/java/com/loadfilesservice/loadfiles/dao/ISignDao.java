package com.loadfilesservice.loadfiles.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loadfilesservice.loadfiles.entity.Sign;

public interface ISignDao extends JpaRepository<Sign, Long>{
	
	@Query("SELECT s FROM Sign s WHERE s.company = :company AND s.state = 1")
    Optional<Sign> findActiveSignByCompany(@Param("company") Long company);

    @Query("SELECT s FROM Sign s WHERE s.user = :user AND s.state = 1")
    Optional<Sign> findActiveSignByUser(@Param("user") Long user);

}
