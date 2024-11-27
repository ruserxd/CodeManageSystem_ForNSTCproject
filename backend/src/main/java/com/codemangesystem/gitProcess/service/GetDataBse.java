package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_Data.Files;
import com.codemangesystem.gitProcess.model_Data.Project;
import com.codemangesystem.gitProcess.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 獲取 ProjectRepository 的相關資料，(新增、獲取、刪除)
 */
@Slf4j
@Service
public class GetDataBse {
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
    public String deleteData(String projectName) {
        try {
            Project project = projectRepository.findByProjectName(projectName);

            // 沒找到相對應的 ProjectName 情況
            if (project == null) {
                log.warn("No project found with name: {}", projectName);
                return "No project found to delete";
            }

            projectRepository.delete(project);
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
}
