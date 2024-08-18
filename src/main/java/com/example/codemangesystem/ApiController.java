package com.example.codemangesystem;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

    //TODO: 負責將程式碼，做分類，並回傳給前端
    @PostMapping("/fetch-repo/categorize")
    public ResponseEntity<List<Code>> categorizeCode(@RequestParam("Path") String Path) {
        logger.info("Try to get data from {}", Path);
        return new ResponseEntity<List<Code>>(fileProcessor.ProcessFiles(Path), HttpStatus.OK);
    }


    //TODO: 改成只負責將 URL 的程式碼 clone
    @PostMapping("/fetch-repo")
    public ResponseEntity<?> fetchRepo(@RequestParam("url") String url) {
        try {
            String clonePath = gitCloner.cloneRepository(url);

            StringBuilder code = new StringBuilder();

            try (Stream<Path> paths = Files.walk(Paths.get(clonePath))) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .forEach(file -> {
                            try {
                                code.append(file.getFileName()).append("\n");
                                code.append(Files.readString(file)).append("\n\n");
                            } catch (IOException e) {
                                logger.error("Error reading file: {}", file, e);
                            }
                        });
            } catch (IOException e) {
                logger.error("Error walking file tree: {}", clonePath, e);
            }

            List<CommitDiffInfo> commitDiffs = gitCloner.getCommitDiffs(clonePath);

            Map<String, Object> response = new HashMap<>();
            response.put("code", code.toString());
            response.put("diffs", commitDiffs);

            return ResponseEntity.ok(response);

        } catch (GitAPIException | IOException e) {
            logger.error("Error cloning or accessing repository: ", e);
            return ResponseEntity.status(500).body("複製或存取儲存庫時發生錯誤。請檢查 URL 是否正確。");
        }
    }
}
