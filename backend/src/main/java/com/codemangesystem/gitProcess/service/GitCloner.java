package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_Git.GitResult;
import com.codemangesystem.gitProcess.model_Git.GitStatus;
import com.codemangesystem.gitProcess.model_Repo.RepoINFO;
import com.codemangesystem.gitProcess.repository.ProjectRepository;
import com.codemangesystem.loginProcess.model_user.MyUser;
import com.codemangesystem.loginProcess.repository.MyUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 處理有關 Git clone 的操作
 */
// TODO: 遇上分支上的問題
@Slf4j
@Service
public class GitCloner {
    // clone 存放的檔案位置
    private static final String CLONE_LOCAL_BASE_PATH = "src/cloneCode/";

    private final GitDiffAnalyzer gitDiffAnalyzer;
    private final MyUserRepository myUserRepository;
    private final ProjectRepository projectRepository;

    public GitCloner(GitDiffAnalyzer gitDiffAnalyzer, MyUserRepository myUserRepository, ProjectRepository projectRepository) {
        this.gitDiffAnalyzer = gitDiffAnalyzer;
        this.myUserRepository = myUserRepository;
        this.projectRepository = projectRepository;
    }

    // TODO: 當出現同一個使用者要 clone 相同檔案的狀況處理
    // TODO: 使用者 GitHub 的權限

    /**
     * 判斷儲存庫是否需要 clone 到本地資料夾，並回傳最終儲存庫存放的路徑
     */
    public GitResult cloneRepository(String repoUrl, String commitId, Long userId) throws GitAPIException, IOException {
        log.info("Clone by {} {} {}", repoUrl, commitId, userId);
        RepoINFO repoINFO = RepoINFO.builder()
                                    .repoName(GitFunction.getRepoNameFromUrl(repoUrl))
                                    .localPath(CLONE_LOCAL_BASE_PATH + GitFunction.getRepoNameFromUrl(repoUrl))
                                    .build();

        MyUser user = myUserRepository.findByUserId(userId)
                                      .orElse(null);

        log.info("當前 repoINFO path : {}  name : {}", repoINFO.localPath, repoINFO.repoName);
        try {
            // 如果 user 資料庫內已經存在， 直接回傳 GitResult
            List<String> projectNames = projectRepository.findProjectNameByUserId(userId);
            for (String projectName : projectNames) {
                if (projectName.equals(repoINFO.repoName)) {
                    log.info("Repository already exists at: {}", repoINFO.localPath);
                    return GitResult.builder()
                                    .message("此帳戶已經有 clone 過 " + projectName)
                                    .status(GitStatus.CLONE_FAILED)
                                    .build();
                }
            }

            log.info("Cloning to {}", repoUrl);

            //TODO: 若本地端已經有存儲庫，那不再次進行 clone
            CloneCommand command = Git.cloneRepository()
                                      .setURI(repoUrl)
                                      .setDirectory(new File(repoINFO.localPath));
            // 未來會用到的使用者資訊加入
            // UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider(login, password);
            // clone.setCredentialsProvider(user);
            // clone.call()

            // 將資料 clone 下來，try 達到 close
            // 只是要透過 Git 物件將資料 clone 下來
            // clone 成功接續將資料分類存入資料庫內
            try (Git git = command.call()) {

                // 當有指定的 commitId
                if (!commitId.equals("HEAD")) {
                    checkToCommitId(git, commitId);
                }

                log.info("成功 clone: {}", repoINFO.localPath);
                log.info("嘗試分類 -> gitDiffAnalyzer");

                // 執行分析專案
                return gitDiffAnalyzer.analyzeAllCommits(repoINFO.localPath, user);
            }
        } catch (GitAPIException | RevisionSyntaxException | IOException e) {
            log.error("Failed clone to {}", repoUrl, e);
            return GitResult.builder()
                            .status(GitStatus.CLONE_FAILED)
                            .message("Clone 發生 " + e)
                            .build();
        }
    }

    // 切換到指定的 commitId，遇上問題會直接拋出例外
    public void checkToCommitId(Git git, String commitId) throws RevisionSyntaxException, IOException, GitAPIException {
        ObjectId specifyCommit = git.getRepository()
                                    .resolve(commitId);
        // 指定的 commitId 不存在
        if (specifyCommit == null) {
            log.error("Commit {} not found in repository", commitId);
            throw new IllegalArgumentException("指定的 commitId 不存在");
        }

        git.checkout()
           .setName(specifyCommit.getName())
           .call();
        log.info("成功 checked out commit: {}", commitId);
    }
}
