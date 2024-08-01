package com.example.codemangesystem;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Controller
public class UrlController {
    private static final Logger logger = LoggerFactory.getLogger(UrlController.class);
    private final GitCloner gitCloner = new GitCloner();

    @GetMapping("/enter-url")
    public String showForm() {
        return "enter-url"; // 假設你有一個名為 "enter-url.html" 的 Thymeleaf 模板
    }

    @PostMapping("/enter-url")
    public String submitForm(@RequestParam("url") String url, Model model) {
        try {
            String clonePath = gitCloner.cloneRepository(url);

            StringBuilder code = new StringBuilder();
            Files.walk(Paths.get(clonePath))  // 使用 clonePath，其中包含了正確的資料夾名稱
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(file -> {
                        try {
                            code.append(file.getFileName()).append("\n");
                            code.append(Files.readString(file)).append("\n\n");
                        } catch (IOException e) {
                            logger.error("Error reading file: {}", file, e);
                        }
                    });

            model.addAttribute("code", code.toString());
        } catch (GitAPIException | IOException e) { // 捕捉 GitAPIException 和 IOException
            logger.error("Error cloning or accessing repository: ", e);
            model.addAttribute("errorMessage", "複製或存取儲存庫時發生錯誤。請檢查 URL 是否正確。");
        }
        return "result";
    }

}
