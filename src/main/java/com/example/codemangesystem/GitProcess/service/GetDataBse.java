package com.example.codemangesystem.GitProcess.service;

import com.example.codemangesystem.GitProcess.model_Data.Files;
import com.example.codemangesystem.GitProcess.model_Data.Project;
import com.example.codemangesystem.GitProcess.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 獲取 ProjectRepository 的相關資料，(新增、獲取、刪除)
 */
@Service
public class GetDataBse {
    private final ProjectRepository projectRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(GetDataBse.class);

    @Autowired
    private GetDataBse(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // 獲取目前資料庫內的所有 ProjectName
    public List<String> getAllProjectNames() {
        return projectRepository.findAllProjectNames();
    }

    // 透過 ProjectName 獲取有關的資料
    public List<Files> getFilesByProjectName(String projectName) {
        try {
            return projectRepository.findByProjectName(projectName).getFiles();
        } catch (Exception error) {

            // 如果在 jpa 的部分執行時發生錯誤，回傳一個空的陣列，避免後續可能出現 null 的情況
            LOGGER.error(error.getMessage());
            return new ArrayList<>();
        } finally {
            LOGGER.info("完成獲得 " + projectName + " Data");
        }
    }

    // 刪除 Project 的相關資料
    public String deleteData(String projectName) {
        try {
            Project project = projectRepository.findByProjectName(projectName);

            // 沒找到的情況
            if (project == null) {
                LOGGER.warn("No project found with name: " + projectName);
                return "No project found to delete";
            }

            projectRepository.delete(project);
            return "Success delete";
        } catch (Exception e) {
            LOGGER.error("delete 發生 : " + e);
            return "Failed delete";
        }
    }
}
