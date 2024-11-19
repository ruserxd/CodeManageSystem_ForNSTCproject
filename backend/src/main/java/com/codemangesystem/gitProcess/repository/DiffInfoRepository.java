package com.codemangesystem.gitProcess.repository;

import com.codemangesystem.gitProcess.model_Data.DiffInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiffInfoRepository extends JpaRepository<DiffInfo, Long> {
}