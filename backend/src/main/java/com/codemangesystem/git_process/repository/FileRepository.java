package com.codemangesystem.git_process.repository;

import com.codemangesystem.git_process.model.database.Files;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<Files, Long> {

  // 根據 filesId 批量載入 Methods
  @Query("SELECT DISTINCT f FROM Files f " +
      "LEFT JOIN FETCH f.methods " +
      "WHERE f.filesId IN :fileIds")
  List<Files> findByFilesIdInWithMethods(@Param("fileIds") List<Long> fileIds);
}