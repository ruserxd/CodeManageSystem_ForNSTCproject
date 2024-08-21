package com.example.codemangesystem;

import com.example.codemangesystem.model.Code;
import com.example.codemangesystem.service.FileProcessor;
import com.example.codemangesystem.service.GitCloner;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api")
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private final GitCloner gitCloner;
    private final FileProcessor fileProcessor;

    @Autowired
    public ApiController(GitCloner gitCloner, FileProcessor fileProcessor) {
        this.gitCloner = gitCloner;
        this.fileProcessor = fileProcessor;
    }

    // 負責分類存儲庫的 api
    @PostMapping("/fetch-repo/categorize")
    public ResponseEntity<List<Code>> categorizeCode(@RequestParam("Path") String Path) {
        logger.info("Try to get data from {}", Path);
        return new ResponseEntity<List<Code>>(fileProcessor.ProcessFiles(Path), HttpStatus.OK);
    }

    // 負責 clone 存儲庫的 api
    @PostMapping("/fetch-repo")
    public ResponseEntity<?> fetchRepo(@RequestParam("url") String url) {
        try {
            String clonePath = gitCloner.cloneRepository(url);
            return ResponseEntity.ok(clonePath);
        } catch (GitAPIException | IOException e) {
            logger.error("Error cloning or accessing repository: ", e);
            return ResponseEntity.status(500).body("複製或存取儲存庫時發生錯誤。請檢查 URL 是否正確。");
        }
    }
}
