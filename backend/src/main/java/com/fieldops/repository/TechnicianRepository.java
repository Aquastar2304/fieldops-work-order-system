package com.fieldops.repository;

import com.fieldops.domain.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    Optional<Technician> findByEmployeeNo(String employeeNo);

    boolean existsByEmployeeNo(String employeeNo);
}
