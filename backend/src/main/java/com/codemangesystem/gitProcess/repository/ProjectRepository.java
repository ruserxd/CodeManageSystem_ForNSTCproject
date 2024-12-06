package com.codemangesystem.gitProcess.repository;

import com.codemangesystem.gitProcess.model_DataBase.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    // 透過 projectName 找尋 Project 物件
    Project findByProjectName(String projectName);

    // 透過 projectName 找尋 headRevStr
    @Query("SELECT p.headRevstr FROM Project p WHERE p.projectName = :projectName")
    String findHeadRevstrByProjectName(@Param("projectName") String projectName);

    @Query("SELECT p.projectName FROM Project p WHERE p.projectId = :id")
    String findProjectNameByProjectId(@Param("id") Long id);
}