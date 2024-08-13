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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

// 因為有 @Controller ，所以當 Spring Boot 應用程式啟動時，會自動掃描並載入這個類別
@Controller
public class UrlController {
    private static final Logger logger = LoggerFactory.getLogger(UrlController.class);
    private final GitCloner gitCloner = new GitCloner();

    // 放上 /enter-url 的路由，用來顯示輸入 URL 的表單
    @GetMapping("/enter-url")
    public String showForm() {
        return "enter-url";
    }

    // 當使用者提交表單時，會呼叫這個方法，並將 (http://host:port/enter-url?url=參數值) 的參數值傳入
    @PostMapping("/enter-url")
    public String submitForm(@RequestParam("url") String url, Model model) {
        try {
            String clonePath = gitCloner.cloneRepository(url);

            // 取得指定路徑的 .java 程式碼
            // 使用 try-with-resources 來管理 Files.walk 返回的 Stream<Path> 物件
            StringBuilder code = new StringBuilder();

            // 將 clonePath 路徑下的所有 .java 檔案讀取並加入 code 字串中
            // Stream<Path> 包含了所有檔案和目錄的路徑。
            try (Stream<Path> paths = Files.walk(Paths.get(clonePath))) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .forEach(file -> {
                            try {
                                // 將檔案名稱和內容加入 code 字串中
                                code.append(file.getFileName()).append("\n");
                                code.append(Files.readString(file)).append("\n\n");
                            } catch (IOException e) {
                                logger.error("Error reading file: {}", file, e);
                            }
                        });
            } catch (IOException e) {
                logger.error("Error walking file tree: {}", clonePath, e);
            }

            // 將 code 字串加入 model 中，以便在結果頁面顯示
            model.addAttribute("code", code.toString());

            // 取得 commit 差異資訊
            List<CommitDiffInfo> commitDiffs = gitCloner.getCommitDiffs(clonePath);
            model.addAttribute("diffs", commitDiffs);

        } catch (GitAPIException | IOException e) {
            logger.error("Error cloning or accessing repository: ", e);
            model.addAttribute("errorMessage", "複製或存取儲存庫時發生錯誤。請檢查 URL 是否正確。");
        }
        return "result";
    }

}
