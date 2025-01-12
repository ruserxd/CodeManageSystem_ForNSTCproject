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

    public ApiController(GitCloner gitCloner) {
        this.gitCloner = gitCloner;
    }

    @PostMapping("/fetch-repo")
    public ResponseEntity<?> testAPI(@RequestParam("url") String url) {
        try {

        } catch (GitAPIException | IOException e) {

        }
    }

    void function1() {

    }
}