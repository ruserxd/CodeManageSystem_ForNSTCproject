package com.example.codemangesystem.GitProcess.repository;

import com.example.codemangesystem.GitProcess.model_Data.Files;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilesRepository extends JpaRepository<Files, Long> {
}