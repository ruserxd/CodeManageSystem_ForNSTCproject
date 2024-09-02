package com.example.codemangesystem.repository;

import com.example.codemangesystem.model.DiffInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiffInfoRepository extends JpaRepository<DiffInfo, Long> {
}