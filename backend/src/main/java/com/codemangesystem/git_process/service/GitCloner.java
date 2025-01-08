package com.codemangesystem.git_process.service;

import com.codemangesystem.git_process.model_database.PersonalINFO;
import com.codemangesystem.git_process.model_database.Project;
import com.codemangesystem.git_process.model_git.GitResult;
import com.codemangesystem.git_process.model_git.GitStatus;
import com.codemangesystem.git_process.model_repo.RepositoryINFO;
import com.codemangesystem.git_process.repository.PersonalRepository;
import com.codemangesystem.git_process.repository.ProjectRepository;
import com.codemangesystem.login_process.model_user.MyUser;
import com.codemangesystem.login_process.repository.MyUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * 處理有關 Git clone 的操作
 */
// TODO: 遇上分支上的問題
// TODO: Git 原本的 diff
@Slf4j
@Service
public class GitCloner {
    // clone 存放的檔案位置
    private static final String CLONE_LOCAL_BASE_PATH = "src/cloneCode/";

    private final GitDiffAnalyzer gitDiffAnalyzer;
    private final MyUserRepository myUserRepository;
    private final GitPuller gitPuller;
    private final PersonalRepository personalRepository;
    private final ProjectRepository projectRepository;

    public GitCloner(GitDiffAnalyzer gitDiffAnalyzer, MyUserRepository myUserRepository, GitPuller gitPuller, PersonalRepository personalRepository, ProjectRepository projectRepository) {
        this.gitDiffAnalyzer = gitDiffAnalyzer;
        this.myUserRepository = myUserRepository;
        this.gitPuller = gitPuller;
        this.personalRepository = personalRepository;
        this.projectRepository = projectRepository;
    }

    // TODO: 使用者 GitHub 的權限

    /**
     * 判斷儲存庫是否需要 clone 到本地資料夾，並回傳最終儲存庫存放的路徑
     */
    public GitResult cloneRepository(String repoUrl, String commitId, Long userId) throws GitAPIException, IOException {
        log.info("Clone by {} {} {}", repoUrl, commitId, userId);
        RepositoryINFO repoINFO = RepositoryINFO.builder()
                                                .repoName(GitFunction.getRepoNameFromUrl(repoUrl))
                                                .localPath(CLONE_LOCAL_BASE_PATH + GitFunction.getRepoNameFromUrl(repoUrl))
                                                .build();

        MyUser user = myUserRepository.findByUserId(userId)
                                      .orElse(null);

        log.info("當前 repoINFO path : {}  name : {}", repoINFO.localPath, repoINFO.repoName);
        try {
            // 如果 user 資料庫內已經存在， 直接回傳 GitResult
            if (GitFunction.isUserCloned(userId, repoINFO, personalRepository)) {
                return GitResult.builder()
                                .message("此帳戶已經有 clone 過 " + repoUrl)
                                .status(GitStatus.CLONE_FAILED)
                                .build();
            }

            log.info("Cloning to {}", repoUrl);

            // 當本地端有該儲存庫的處理
            if (GitFunction.isInLocalGitDirectory(repoINFO.localPath)) {
                return pullAndUpdateDataBase(repoINFO, user, commitId);
            }

            /*
             未來會用到的使用者資訊加入
             UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider(login, password);
             clone.setCredentialsProvider(user);
             clone.call()
            */

            CloneCommand command = Git.cloneRepository()
                                      .setURI(repoUrl)
                                      .setDirectory(new File(repoINFO.localPath));
            /*
             將資料 clone 下來，try 達到 close
             只是要透過 Git 物件將資料 clone 下來
             clone 成功接續將資料分類存入資料庫內
            */
            log.info("Try to clone {}", repoUrl);
            Git git = command.call();

            // 當有指定的 commitId
            if (!commitId.equals("HEAD")) {
                log.info("Check to {}", commitId);
                checkToCommitId(git, commitId);
            }

            log.info("成功 clone: {}", repoINFO.localPath);
            log.info("嘗試分類 -> gitDiffAnalyzer");

            // 避免多個 try-catch 的出現，手動關閉 git
            git.close();

            // 執行分析專案
            return gitDiffAnalyzer.analyzeAllCommits(repoINFO.localPath, user);

        } catch (GitAPIException | RevisionSyntaxException | IOException e) {
            log.error("Failed clone to {}", repoUrl, e);
            return GitResult.builder()
                            .status(GitStatus.CLONE_FAILED)
                            .message("Clone 發生 " + e.getMessage())
                            .build();
        }
    }

    /**
     * clone 時若本地端有該 Repository 執行此
     */
    public GitResult pullAndUpdateDataBase(RepositoryINFO repoINFO, MyUser user, String commitId) {
        log.info("這項專案已經有人 Clone 過並存放於 {}", repoINFO.localPath);
        log.info("改執行 pull");

        // 執行 puller 並拿取一個 GitResult
        GitResult result = gitPuller.pullLocalRepository(repoINFO);
        if (result.isPullFailed()) {
            log.error("本地端有該存儲庫，但 pull 失敗無法更新");
            return result;
        }
        log.info("本地端有該存儲庫，pull 成功");

        Project project = projectRepository.findByProjectName(repoINFO.repoName)
                                           .orElse(null);
        if (project == null) {
            log.warn("獲取 {} project 失敗", repoINFO.repoName);
            return GitResult.builder()
                            .status(GitStatus.DATABASE_FAILED)
                            .message("資料庫獲取 project 時失敗")
                            .build();
        }
        log.info("成功獲取 {}", repoINFO.repoName);

        PersonalINFO personalINFO = PersonalINFO.builder()
                                                .user(user)
                                                .project(project)
                                                .build();

        // pull 完後，進行資料庫的更新
        // 加入 HeadRevstr
        try (Repository repo = new FileRepository(repoINFO.localPath + "/.git")) {
            ObjectId objectId;

            log.info("獲取 SHA1 by {}", commitId);
            if (Objects.equals(commitId, "HEAD")) {
                objectId = repo.resolve(Constants.HEAD);
            } else {
                objectId = repo.resolve(commitId);
            }
            log.info("objectId : {}", objectId);
            if (objectId == null) {
                log.error("因為 objectId {}, 無法解析 commit ID: {}", null, commitId);
                return GitResult.builder()
                                .status(GitStatus.CLONE_FAILED)
                                .message("無法獲取正確的 commit SHA1")
                                .build();
            }

            String headRevstr = objectId.getName();
            personalINFO.setHeadRevstr(headRevstr);
            personalRepository.save(personalINFO);
            result.setMessage("因為本地端有該存儲庫，因此改為 Pull 並成功 Pull 更新資料");

            return result;

        } catch (IOException e) {
            log.error("讀取 repository 時發生錯誤: {}", e.getMessage());
            return GitResult.builder()
                            .status(GitStatus.CLONE_FAILED)
                            .message("讀取 repository 時發生錯誤: " + e.getMessage())
                            .build();
        }
    }


    /**
     * 切換到指定的 commitId
     */
    public void checkToCommitId(Git git, String commitId) throws IOException, GitAPIException, RevisionSyntaxException {
        ObjectId specifyCommit = git.getRepository()
                                    .resolve(commitId);
        // 指定的 commitId 不存在
        if (specifyCommit == null) {
            log.error("Commit {} not found in repository", commitId);
            throw new RevisionSyntaxException("");
        }

        git.checkout()
           .setName(specifyCommit.getName())
           .call();
        log.info("成功 checked out commit: {}", commitId);
    }
}
