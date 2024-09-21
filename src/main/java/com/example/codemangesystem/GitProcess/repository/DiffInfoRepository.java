package com.example.codemangesystem.GitProcess.repository;

import com.example.codemangesystem.GitProcess.model_Data.DiffInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiffInfoRepository extends JpaRepository<DiffInfo, Long> {
}