package com.example.codemangesystem.service;

import com.example.codemangesystem.model.Files;
import com.example.codemangesystem.model.DiffInfo;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

// 資料的處理，read 每個.java，並將存到 Code 中，主要以 function 檢查為目標
@Service
public class FileProcessor {
    private static final Logger logger = LoggerFactory.getLogger(FileProcessor.class);

    // 遍歷每個資料夾的 .java 檔案，並回傳處理過的 List<Code>
    public List<Files> ProcessFiles(String repositoryPath) {
        List<Files> codeList = new ArrayList<>();
        Path path = Paths.get(repositoryPath);
        logger.info(path.toString());

        try (Stream<Path> paths = java.nio.file.Files.walk(path)) {
            paths.filter(java.nio.file.Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        String fileName = p.getFileName().toString();
                        System.out.println(fileName);
                        Files code = categorizeCode(p.toString());
                        if (code != null) {
                            codeList.add(code);
                        }
                    });
        } catch (IOException e) {
            logger.error("Failed to read {}", repositoryPath, e);
        }

        return codeList;
    }

    // 將資料夾內的程式碼做分類並回傳 Code
    public Files categorizeCode(String filePath) {
        return Files.builder()
                .fileName(Paths.get(filePath).getFileName().toString())
                .methods(findMethods(filePath))
                .build();
    }

    // 找尋這段程式碼的方法有哪些，並對其程式碼做處理(放入目前程式碼、比較版本差異)，最後回傳 Map<String, List<DiffInfo>>
    public Map<String, List<DiffInfo>> findMethods(String filePath) {
        Map<String, List<DiffInfo>> functions = new HashMap<>();

        try {
            // 載入 java 檔案，並分析檔案，生成 CompilationUnit (AST 的根節點)
            FileInputStream in = new FileInputStream(filePath);
            CompilationUnit cu = StaticJavaParser.parse(in);

            // 查找所有檔案的方法聲明
            List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);

            // 每個方法遍歷一次
            for (MethodDeclaration method : methods) {
                String methodName = method.getNameAsString();

                // 對於內部程式碼的處理
                // 將 Optional<Statement> 轉換為 Optional<String>，如果有值則轉換為其 toString() 結果
                String methodBody = method.getBody().map(Objects::toString).orElse(null);

                // 創立 DiffInfo 物件
                DiffInfo diffCode = DiffInfo.builder()
                        .diffCode(methodBody)
                        .build();

                // 將 DiffInfo 放置於對應的 key(methodName) 內，若沒有對應的 key 會創立一個 [key, newList] 進去 map 內
                functions.computeIfAbsent(methodName, newList -> new ArrayList<>()).add(diffCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return functions;
    }

}
