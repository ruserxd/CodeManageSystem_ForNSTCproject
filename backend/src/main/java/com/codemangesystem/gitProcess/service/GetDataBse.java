package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_Data.Files;
import com.codemangesystem.gitProcess.model_Data.Project;
import com.codemangesystem.gitProcess.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.internal.storage.file.WindowCache;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 獲取 ProjectRepository 的相關資料，(新增、獲取、刪除)
 */
@Slf4j
@Service
public class GetDataBse {
    private static final String CLONE_LOCAL_BASE_PATH = "src/cloneCode/";

    private final ProjectRepository projectRepository;

    @Autowired
    private GetDataBse(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * 獲取目前資料庫內的所有 ProjectName
     */
    public List<String> getAllProjectNames() {
        return projectRepository.findAllProjectNames();
    }

    /**
     * 透過 ProjectName 獲取有關的資料
     */
    public List<Files> getFilesByProjectName(String projectName) {
        try {
            return projectRepository.findByProjectName(projectName)
                                    .getFiles();
        } catch (Exception error) {
            // 如果在 jpa 的部分執行時發生錯誤，回傳一個空的陣列，避免後續可能出現 null 的情況
            log.error(error.getMessage());
            return new ArrayList<>();
        } finally {
            log.info("完成獲得 {}'s Data", projectName);
        }
    }

    /**
     * 透過 ProjectName 刪除 Project 的相關資料
     */
    public String deleteDataByProjectName(String projectName) {
        try {
            Project project = projectRepository.findByProjectName(projectName);

            // 沒找到相對應的 ProjectName 情況
            if (project == null) {
                log.warn("No project found with name: {}", projectName);
                return "No project found to delete";
            }

            projectRepository.delete(project);
            deleteGitRepository(CLONE_LOCAL_BASE_PATH + projectName);
            log.info("刪除資料夾 {}", projectName);
            return "Success delete";
        } catch (Exception e) {
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
