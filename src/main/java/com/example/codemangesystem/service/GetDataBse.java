package com.example.codemangesystem.service;

import com.example.codemangesystem.model.Files;
import com.example.codemangesystem.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetDataBse {
    private final ProjectRepository projectRepository;

    @Autowired
    private GetDataBse(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<String> getAllProjectNames() {
        return projectRepository.findAllProjectNames();
    }

    public List<Files> getFilesByProjectName(String ProjectName) {
        return projectRepository.findByProjectName(ProjectName).getFiles();
    }
}
