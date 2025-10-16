package com.codemangesystem.git_process.repository;

import com.codemangesystem.git_process.model.database.DiffInfo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiffInfoRepository extends JpaRepository<DiffInfo, Long> {

  // 根據 methodId 獲取所有 DiffInfo
  @Query("SELECT d FROM DiffInfo d WHERE d.method.methodId = :methodId ORDER BY d.commitTime DESC")
  List<DiffInfo> findByMethodId(@Param("methodId") Long methodId);
}