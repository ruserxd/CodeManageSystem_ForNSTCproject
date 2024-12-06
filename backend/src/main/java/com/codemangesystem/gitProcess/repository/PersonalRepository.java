package com.codemangesystem.gitProcess.repository;

import com.codemangesystem.gitProcess.model_DataBase.PersonalINFO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonalRepository extends JpaRepository<PersonalINFO, Long> {
    @Query("SELECT p.project.projectId FROM PersonalINFO p WHERE p.user.userId = :userId")
    List<Long> findProjectIdByUserId(@Param("userId") Long userId);

    @Query("SELECT p.projectName FROM Project p JOIN PersonalINFO pi ON p.projectId = pi.project.projectId WHERE pi.user.userId = :userId")
    List<String> findProjectNameByUserId(@Param("userId") Long userId);
}