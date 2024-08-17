package com.example.codemangesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class FileProcessor {
    private static final Logger logger = LoggerFactory.getLogger(FileProcessor.class);

    private final List<Code> codeList = new ArrayList<>();
    // 遍歷每個資料夾的 .java 檔案
    public void ProcessFiles(String repositoryPath) {
        Path path = Paths.get(repositoryPath);
        logger.info(path.toString());
        try (Stream<Path> paths = Files.walk(path)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        String fileName = p.getFileName().toString();
                        System.out.println(fileName);
                        Code code = categorizeCode(p.toFile());
                        if (code != null) {
                            codeList.add(code);
                        }
                    });
        } catch (IOException e) {
            logger.error("Failed to read {}", repositoryPath, e);
        }
    }

    // 將資料夾內的程式碼做分類並存到
    public Code categorizeCode(File file) {
        // TODO: 將資料夾內的程式碼，透過 JavaParser 分類
        return null;
    }

}
