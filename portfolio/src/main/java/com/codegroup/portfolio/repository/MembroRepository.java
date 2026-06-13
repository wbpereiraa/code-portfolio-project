package com.codegroup.portfolio.repository;

import com.codegroup.portfolio.entity.Membro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembroRepository extends JpaRepository<Membro, Long> {
}
