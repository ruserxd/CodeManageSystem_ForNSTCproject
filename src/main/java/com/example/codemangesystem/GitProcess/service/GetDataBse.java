package com.example.codemangesystem.GitProcess.service;

import com.example.codemangesystem.GitProcess.model_Data.Files;
import com.example.codemangesystem.GitProcess.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetDataBse {
    private final ProjectRepository projectRepository;

    private static final Logger logger = LoggerFactory.getLogger(GetDataBse.class);

    @Autowired
    private GetDataBse(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<String> getAllProjectNames() {
        return projectRepository.findAllProjectNames();
    }

    public List<Files> getFilesByProjectName(String ProjectName) {
        try {
            return projectRepository.findByProjectName(ProjectName).getFiles();
        } finally {
            logger.info("完成獲得 " + ProjectName + " Data");
        }
    }
}
