package com.example.codemangesystem.GitProcess.service;

import com.example.codemangesystem.GitProcess.model_Data.Files;
import com.example.codemangesystem.GitProcess.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GetDataBse {
    private final ProjectRepository projectRepository;

    private static final Logger logger = LoggerFactory.getLogger(GetDataBse.class);

    @Autowired
    private GetDataBse(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // 獲取目前資料庫內的所有 ProjectName
    public List<String> getAllProjectNames() {
        return projectRepository.findAllProjectNames();
    }

    // 透過 ProjectName 獲取有關的資料
    public List<Files> getFilesByProjectName(String ProjectName) {
        try {
            return projectRepository.findByProjectName(ProjectName).getFiles();
        } catch (Exception error) {

            // 如果在 jpa 的部分執行時發生錯誤，回傳一個空的陣列，避免後續可能出現 null 的情況
            logger.error(error.getMessage());
            return new ArrayList<>();
        } finally {
            logger.info("完成獲得 " + ProjectName + " Data");
        }
    }
}
