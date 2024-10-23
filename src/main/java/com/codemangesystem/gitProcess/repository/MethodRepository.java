package com.codemangesystem.gitProcess.repository;

import com.codemangesystem.gitProcess.model_Data.Method;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MethodRepository extends JpaRepository<Method, Long> {
}
