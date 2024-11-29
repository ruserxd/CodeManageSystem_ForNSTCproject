package com.codemangesystem.gitProcess.repository;

import com.codemangesystem.gitProcess.model_Data.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // 找尋所有的 projectName
    @Query("SELECT p.projectName FROM Project p WHERE p.user.userId = :userId")
    List<String> findProjectNameByUserId(Long userId);

    // 透過 projectName 找尋 Project 物件
    Project findByProjectName(String projectName);

    // 透過 projectName 找尋 headRevStr
    @Query("SELECT p.headRevstr FROM Project p WHERE p.projectName = :projectName")
    String findHeadRevstrByProjectName(@Param("projectName") String projectName);
}