package com.example.codemangesystem;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.stereotype.Service;

@Service
public class GitCloner {
    private static final Logger logger = LoggerFactory.getLogger(GitCloner.class);
    private static final String DEFAULT_BRANCH = "main";

    // 將儲存庫複製到本地資料夾，並按照 commit 時間做分類，並回傳最終的路徑
    // 可能丟出 GitAPIException(Git操作錯誤) 和 IOException(檔案操作錯誤)
    public String cloneRepository(String repoUrl) throws GitAPIException, IOException {
        String repoName = getRepoNameFromUrl(repoUrl);
        String localPath = "backend/cloneCode/" + repoName;
        // 先嘗試複製儲存庫至臨時的資料夾 -> 取得 commit 的時間 -> 移動資料夾至最終路徑 -> 回傳最終路徑
        // 將 Git 物件命名為 ignored ，因為在這個特定的 try 區塊中，實際上並不需要直接使用這個物件
        try (Git ignored = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(localPath))
                .call()) {
            // 取得最新提交的時間戳
            String timestamp = getCommitTimestamp(localPath);
            String finalPath = "backend/cloneCode/" + repoName + "_" + timestamp;

            // 如果本地資料夾已經存在，直接回傳路徑
            if (isRepositoryClonedLocally(finalPath)) {
                logger.info("Repository already exists at: {}", finalPath);
                return finalPath;
            }

            Path sourcePath = Paths.get(localPath);
            Path targetPath = Paths.get(finalPath);
            Files.move(sourcePath, targetPath);

            logger.info("Repository cloned to: {}", finalPath);
            return finalPath;
        } finally {
            // 最後刪除臨時的資料夾
            deleteDirectory(new File(localPath));
        }
    }

    // 從儲存庫 URL 中取得專案名稱
    private String getRepoNameFromUrl(String repoUrl) {
        // 將網址透過 "/" 分開
        String[] parts = repoUrl.split("/");
        // 抓取最後面的專案名稱
        String repoNameWithExtension = parts[parts.length - 1];
        // .git 的部分換成 ""
        return repoNameWithExtension.replace(".git", "");
    }

    // 取得最新提交的時間戳
    private String getCommitTimestamp(String localPath) throws IOException {
        // 先將本地儲存庫打開 -> 取得儲存庫物件 -> 取得 HEAD 的 commit ID -> 透過 RevWalk 取得 RevCommit -> 轉換成 Instant -> 格式化時間
        try (Git git = Git.open(new File(localPath))) {
            Repository repository = git.getRepository();
            // 取得 HEAD 的 commit ID
            ObjectId headCommitId = repository.resolve(DEFAULT_BRANCH);
            try (RevWalk revWalk = new RevWalk(repository)) {
                // RevCommit 包含了提交的作者、訊息、時間戳等資訊
                RevCommit commit = revWalk.parseCommit(headCommitId);
                // Git 儲存庫中的提交時間戳是 UTC 時間 (從 1970-01-01T00:00:00Z 開始的秒數)，避免掉不同時區的問題
                // 取得 commit 時間並轉換成 Instant(UTC時間)
                Instant commitTime = Instant.ofEpochSecond(commit.getCommitTime());
                // 將 Instant 轉換成 Zone 系統預設時區的時間，並格式化成 yyyyMMdd_HHmmss 字串
                return commitTime.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            }
        }
    }

    // 刪除指定的資料夾
    private void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        // 刪除目錄內的所有檔案
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        // 刪除目錄
        boolean judge = directoryToBeDeleted.delete();
        if (!judge) {
            // 刪除失敗，進行錯誤處理
            logger.warn("Failed to delete directory: {}", directoryToBeDeleted.getAbsolutePath());
        }
    }

    // 檢查指定路徑是否為已存在 Git 儲存庫
    private boolean isRepositoryClonedLocally(String path) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        // try-with-resources 結束時，自動關閉 Repository
        try (Repository repository = builder
                .setGitDir(new File(path, ".git"))
                .readEnvironment()
                .findGitDir()
                .build()) {
            return repository != null && repository.getDirectory().exists();
        } catch (IOException e) {
            return false;
        }
    }


    // TODO: 這裡的程式碼需要進行重構，以便更好地分離關注點
    // TODO: 這行以下皆需補上註解
    public List<CommitDiffInfo> getCommitDiffs(String repoPath) throws IOException, GitAPIException {
        logger.info("Opening repository: {}", repoPath);
        List<CommitDiffInfo> diffList = new ArrayList<>();

        try (Git git = Git.open(new File(repoPath));
             RevWalk revWalk = new RevWalk(git.getRepository())) {

            ObjectId head = git.getRepository().resolve(DEFAULT_BRANCH);
            RevCommit commit = revWalk.parseCommit(head);

            while (commit != null) {
                RevCommit parent = commit.getParentCount() > 0 ? revWalk.parseCommit(commit.getParent(0)) : null;

                if (parent != null) {
                    handleNonInitialCommit(git, git.getRepository(), revWalk, commit, parent, diffList);
                } else {
                    handleInitialCommit(git.getRepository(), commit, diffList);
                }

                commit = parent;
            }
        }

        return diffList;
    }

    private void handleNonInitialCommit(Git git, Repository repository, RevWalk revWalk, RevCommit commit,
                                        RevCommit parent, List<CommitDiffInfo> diffList) throws IOException, GitAPIException {
        git.diff()
                .setOldTree(prepareTreeParser(repository, parent.getTree(), revWalk))
                .setNewTree(prepareTreeParser(repository, commit.getTree(), revWalk))
                .call()
                .stream()
                .filter(entry -> entry.getNewPath().endsWith(".java"))
                .forEach(entry -> {
                    String originalCode;
                    try {
                        originalCode = entry.getChangeType() != DiffEntry.ChangeType.ADD
                                ? new String(repository.open(entry.getOldId().toObjectId()).getBytes(), StandardCharsets.UTF_8)
                                : "";
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                         DiffFormatter diffFormatter = new DiffFormatter(outputStream)) {

                        diffFormatter.setRepository(repository);
                        diffFormatter.setContext(0);
                        diffFormatter.format(entry);
                        String diffOutput = outputStream.toString(StandardCharsets.UTF_8);

                        diffList.add(new CommitDiffInfo(
                                entry.getNewPath(),
                                commit.getAuthorIdent().getName(),
                                commit.getCommitTime(),
                                originalCode,
                                diffOutput,
                                commit.getName()
                        ));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void handleInitialCommit(Repository repository, RevCommit commit, List<CommitDiffInfo> diffList) throws IOException {

        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(commit.getTree());
            treeWalk.setRecursive(true);

            while (treeWalk.next()) {
                if (treeWalk.getPathString().endsWith(".java")) {
                    String content = new String(repository.open(treeWalk.getObjectId(0)).getBytes(), StandardCharsets.UTF_8);

                    diffList.add(new CommitDiffInfo(
                            treeWalk.getPathString(),
                            commit.getAuthorIdent().getName(),
                            commit.getCommitTime(),
                            "",
                            content,
                            commit.getName()
                    ));
                }
            }
        }
    }

    private AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId treeId, RevWalk revWalk) throws IOException {
        RevTree tree = revWalk.parseTree(treeId);
        CanonicalTreeParser treeParser = new CanonicalTreeParser();

        try (ObjectReader reader = repository.newObjectReader()) {
            treeParser.reset(reader, tree.getId());
        }

        return treeParser;
    }

}
