package com.codemangesystem.gitProcess.repository;

import com.codemangesystem.gitProcess.model_DataBase.PersonalINFO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalRepository extends JpaRepository<PersonalINFO, Long> {
    @Query("SELECT p.project.projectId FROM PersonalINFO p WHERE p.user.userId = :userId")
    List<Long> findProjectIdByUserId(@Param("userId") Long userId);

    @Query("SELECT p.projectName FROM Project p JOIN PersonalINFO pi ON p.projectId = pi.project.projectId WHERE pi.user.userId = :userId")
    List<String> findProjectNameByUserId(@Param("userId") Long userId);

    /**
     * 查詢是否仍有該 Project 的使用者
     */
    @Query("SELECT pi.user.userId FROM PersonalINFO pi  WHERE pi.project.projectName = :projectName")
    List<Long> findProjectIdByProjectName(@Param("projectName") String projectName);

    /**
     * 查詢 userId, ProjectName
     */
    @Query("SELECT pi FROM PersonalINFO pi WHERE pi.project.projectName = :projectName AND pi.user.userId = :userId")
    Optional<PersonalINFO> findProjectByUserIdAndProjectName(@Param("projectName") String projectName, @Param("userId") Long userId);
}