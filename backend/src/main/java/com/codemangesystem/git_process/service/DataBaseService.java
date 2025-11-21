package com.codemangesystem.git_process.service;

import com.codemangesystem.git_process.model.database.DiffInfo;
import com.codemangesystem.git_process.model.database.Files;
import com.codemangesystem.git_process.model.database.PersonalINFO;
import com.codemangesystem.git_process.model.database.Project;
import com.codemangesystem.git_process.model.dto.FileStructureDTO;
import com.codemangesystem.git_process.model.dto.MethodStructureDTO;
import com.codemangesystem.git_process.model.dto.ProjectStructureDTO;
import com.codemangesystem.git_process.repository.DiffInfoRepository;
import com.codemangesystem.git_process.repository.FileRepository;
import com.codemangesystem.git_process.repository.PersonalRepository;
import com.codemangesystem.git_process.repository.ProjectRepository;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 獲取 ProjectRepository 的相關資料，(新增、獲取、刪除)
 */
@Slf4j
@Service
public class DataBaseService {

  private static final String CLONE_LOCAL_BASE_PATH = "src/cloneCode/";

  private final ProjectRepository projectRepository;
  private final PersonalRepository personalRepository;
  private final DiffInfoRepository diffInfoRepository;
  private final FileRepository fileRepository;

  public DataBaseService(ProjectRepository projectRepository,
      PersonalRepository personalRepository, DiffInfoRepository diffInfoRepository,
      FileRepository fileRepository) {
    this.projectRepository = projectRepository;
    this.personalRepository = personalRepository;
    this.diffInfoRepository = diffInfoRepository;
    this.fileRepository = fileRepository;
  }

  /**
   * 獲取 Project 結構（不包含 DiffInfo）- 分步查詢避免 MultipleBagFetchException
   */
  @Transactional(readOnly = true)
  public ProjectStructureDTO getProjectStructure(String projectName) {
    // 載入 Project 和 Files
    Optional<Project> projectOpt = projectRepository.findByProjectNameWithFiles(projectName);

    if (projectOpt.isEmpty()) {
      log.warn("未找到專案: {}", projectName);
      return null;
    }

    Project project = projectOpt.get();

    // 批量載入 Files 的 Methods
    List<Long> fileIds = project.getFiles().stream()
        .map(Files::getFilesId)
        .collect(Collectors.toList());

    if (!fileIds.isEmpty()) {
      List<Files> filesWithMethods = fileRepository.findByFilesIdInWithMethods(fileIds);

      // 將載入的 Methods 對應回 Project 的 Files
      Map<Long, Files> fileMap = filesWithMethods.stream()
          .collect(Collectors.toMap(Files::getFilesId, f -> f));

      project.getFiles().forEach(file -> {
        Files fileWithMethods = fileMap.get(file.getFilesId());
        if (fileWithMethods != null) {
          file.setMethods(fileWithMethods.getMethods());
        }
      });
    }

    log.info("獲取專案結構: {}", projectName);

    // 轉換為 DTO
    return ProjectStructureDTO.builder()
        .projectId(project.getProjectId())
        .projectName(project.getProjectName())
        .headRevstr(project.getHeadRevstr())
        .files(project.getFiles().stream()
            .map(file -> FileStructureDTO.builder()
                .filesId(file.getFilesId())
                .fileName(file.getFileName())
                .filePath(file.getFilePath())
                .methods(file.getMethods() != null ? file.getMethods().stream()
                    .map(method -> MethodStructureDTO.builder()
                        .methodId(method.getMethodId())
                        .methodName(method.getMethodName())
                        .diffCount(0)  // 因為是 LAZY，設為 0
                        .build())
                    .collect(Collectors.toList()) : Collections.emptyList())
                .build())
            .collect(Collectors.toList()))
        .build();
  }

  /**
   * 根據 methodId 獲取 DiffInfo 列表
   */
  @Transactional(readOnly = true)
  public List<DiffInfo> getDiffInfoByMethodId(Long methodId) {
    log.info("獲取 Method {} 的 DiffInfo", methodId);
    return diffInfoRepository.findByMethodId(methodId);
  }

  /**
   * 獲取目前 User 內所有的 ProjectName
   */
  @Transactional(readOnly = true)
  public List<String> getUserProjects(String userId) {
    List<Long> projectId = personalRepository.findProjectIdByUserId(Long.valueOf(userId));
    List<String> userProjectNames = new ArrayList<>();
    for (Long id : projectId) {
      userProjectNames.add(projectRepository.findProjectNameByProjectId(id));
    }
    log.info("{} 獲得 {}", userId, userProjectNames);
    return userProjectNames;
  }

  /**
   * 透過 ProjectName 獲取有關的資料
   */
  @Transactional(readOnly = true)
  public Project getProjectByProjectName(String projectName) {
    Optional<Project> project = projectRepository.findByProjectName(projectName);
    if (project.isPresent()) {
      log.info("完成獲得 {}'s Data", projectName);
    } else {
      log.warn("未完成獲得 {}'s Data", projectName);
    }
    return project.orElse(null);
  }

  /**
   * 透過 ProjectName , userId 刪除 personalRepository 使用者 Project 的相關資料 且若無使用者使用該 Project 本地端的也進行刪除
   */
  public String deleteDataByProjectName(String projectName, String userId) {
    try {
      PersonalINFO personalINFO = personalRepository.findProjectByUserIdAndProjectName(projectName,
              Long.parseLong(userId))
          .orElse(null);

      // 沒找到相對應的 ProjectName 情況
      if (personalINFO == null) {
        log.warn("No personalINFO found with name: {}", projectName);
        return "No personalINFO found to delete";
      }

      Project project = personalINFO.getProject();

      // 刪除 user 與該 project 的關係
      personalRepository.delete(personalINFO);

      // 刪除資料夾的條件必須為沒有任何一個使用者需要這份檔案
      List<Long> userIds = personalRepository.findProjectIdByProjectName(projectName);
      log.info("目前使用此專案的有 {}", userIds);
      if (userIds.isEmpty()) {
        // 刪除 Project 資料表的
        projectRepository.delete(project);

        // 刪除本地端的資料夾
        deleteGitRepository(CLONE_LOCAL_BASE_PATH + projectName);
        log.info("刪除資料夾 {}", projectName);
      }

      return "Success delete";
    } catch (IOException e) {
      log.error("delete 發生 : {}", String.valueOf(e));
      return "Failed delete";
    }
  }

  /**
   * 透過 projectName 獲得資料庫內的 HeadRevstr
   */
  public String getHeadRevstr(String projectName) {
    return projectRepository.findHeadRevstrByProjectName(projectName);
  }

  /*
   * 刪除指定位置的資料夾*/
  public void deleteGitRepository(String repoPath) throws IOException {
    Repository repo = new org.eclipse.jgit.internal.storage.file.FileRepository(repoPath);
    Git git = new Git(repo);
    git.getRepository().close();

    // 關閉 JGit
    repo.close();
    git.close();

    // JGit 的 Bug
    // false 時使用 malloc()+read()
    // 重新設置 JGit 在 JVM 中如何處理 Git 文件的緩存
    // 造成的原因
    // 問題在於 JVM 的 garbage collection 必須確認映射段不再使用後，才能調用 munmap() 釋放內存映射，這可能造成無法及時釋放。
    // 解決來自 : https://stackoverflow.com/questions/19191727/pack-file-from-git-repo-cant-be-deleted-using-file-delete-method
    WindowCacheConfig config = new WindowCacheConfig();
    config.setPackedGitMMAP(false);
    WindowCache.reconfigure(config);
    
    File file = new File(repoPath);
    FileUtils.deleteDirectory(file);
  }
}
