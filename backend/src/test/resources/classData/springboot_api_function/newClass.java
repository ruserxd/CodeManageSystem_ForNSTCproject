import com.codemangesystem.git_process.service.GitCloner;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    private final GitCloner gitCloner;
    private static final String LOCAL_BASE_PATH = "src/cloneCode/";

    // 新增一個字串變數
    String string = "Hello, World!";

    public ApiController(GitCloner gitCloner) {
        this.gitCloner = gitCloner;
    }

    @PostMapping("/fetch-repo")
    public ResponseEntity<?> testAPI(@RequestParam("url") String url) {
        try {
            log.info("嘗試抓取 url: {} 的資料", url);
            return ResponseEntity.ok(gitCloner.cloneRepository(url, commitId, Long.valueOf(userId)));
        } catch (GitAPIException | IOException e) {
            log.error("Error cloning or accessing repository: ", e);
            return ResponseEntity.status(500)
                                 .body("clone 或存取儲存庫時發生錯誤。請檢查 URL 是否正確。");
        }
    }

    void function1() {
        // system.out
        System.out.println(string);
    }
}