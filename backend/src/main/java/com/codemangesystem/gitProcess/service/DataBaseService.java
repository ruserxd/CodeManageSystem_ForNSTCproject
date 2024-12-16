package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_DataBase.PersonalINFO;
import com.codemangesystem.gitProcess.model_DataBase.Project;
import com.codemangesystem.gitProcess.repository.PersonalRepository;
import com.codemangesystem.gitProcess.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 獲取 ProjectRepository 的相關資料，(新增、獲取、刪除)
 */
@Slf4j
@Service
public class DataBaseService {
    private static final String CLONE_LOCAL_BASE_PATH = "src/cloneCode/";

    private final ProjectRepository projectRepository;
    private final PersonalRepository personalRepository;

    public DataBaseService(ProjectRepository projectRepository, PersonalRepository personalRepository) {
        this.projectRepository = projectRepository;
        this.personalRepository = personalRepository;
    }

    /**
     * 獲取目前 User 內所有的 ProjectName
     */
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
     * 透過 ProjectName , userId 刪除 personalRepository 使用者 Project 的相關資料
     * 且若無使用者使用該 Project 本地端的也進行刪除
     */
    public String deleteDataByProjectName(String projectName, String userId) {
        try {
            PersonalINFO personalINFO = personalRepository.findProjectByUserIdAndProjectName(projectName, Long.parseLong(userId))
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
        Repository repo = new FileRepository(repoPath);
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
