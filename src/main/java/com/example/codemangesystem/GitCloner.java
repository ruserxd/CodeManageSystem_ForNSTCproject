package com.example.codemangesystem;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.springframework.stereotype.Service;

// 處理有關 Git 的操作
@Service
public class GitCloner {
    private static final Logger logger = LoggerFactory.getLogger(GitCloner.class);
    private static final String DEFAULT_BRANCH = "main";
    private static final String CLONE_LOCAL_BASE_PATH = "src/cloneCode/";

    // 判斷儲存庫是否需要 clone 到本地資料夾，並回傳最終儲存庫存放的路徑
    public String cloneRepository(String repoUrl) throws GitAPIException, IOException {
        String repoName = getRepoNameFromUrl(repoUrl);
        String localPath = CLONE_LOCAL_BASE_PATH + repoName;
        try {
            // 如果本地資料夾已經存在， pull 更新本地端資料並且直接回傳路徑
            if (isRepositoryClonedLocally(localPath)) {
                logger.info("Repository already exists at: {}", localPath);
                renewRepositoryLocally(localPath);
                return localPath;
            }
            logger.info("Cloning to {} ....", repoUrl);
            // 將資料 clone 下來， Git 物件命名為 ignored ，因為在這個特定的 try 區塊中，實際上並不需要直接使用這個物件
            try (Git ignored = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))
                    .call()) {

                logger.info("Repository success cloned to: {}", localPath);
                return localPath;
            }
        } catch (GitAPIException e) {
            logger.error("Failed clone to {}", repoUrl, e);
            throw e;
        }
    }

    // 從儲存庫 URL 中取得專案名稱
    private String getRepoNameFromUrl(String repoUrl) {
        // 將網址透過 "/" 分開並存在陣列內
        String[] parts = repoUrl.split("/");
        // 抓取最後面的專案名稱
        String repoNameWithExtension = parts[parts.length - 1];
        // .git 的部分換成 ""
        return repoNameWithExtension.replace(".git", "");
    }

    // 檢查指定路徑是否為已存在 Git 儲存庫
    private boolean isRepositoryClonedLocally(String path) {
        File gitDir = new File(path, ".git");
        return gitDir.exists();
    }

    // pull 更新本地端資料
    private void renewRepositoryLocally(String repoPath) {
        try (Git git = Git.open(new File(repoPath))) {
            logger.info("Try to pull {}", repoPath);
            PullResult result = git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName(DEFAULT_BRANCH)
                    .call();
            if (result.isSuccessful()) {
                logger.info("Pull successful");
            } else {
                logger.info("Pull failed");
            }
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
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

                        CommitDiffInfo commitDiffInfo = new CommitDiffInfo(
                                entry.getNewPath(),
                                commit.getAuthorIdent().getName(),
                                commit.getCommitTime(),
                                originalCode,
                                diffOutput,
                                commit.getName()
                        );

                        diffList.add(commitDiffInfo);
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

                    CommitDiffInfo commitDiffInfo = new CommitDiffInfo(
                            treeWalk.getPathString(),
                            commit.getAuthorIdent().getName(),
                            commit.getCommitTime(),
                            "",
                            content,
                            commit.getName()
                    );

                    commitDiffInfo.setAddedLines(content);
                    commitDiffInfo.setRemovedLines("");

                    diffList.add(commitDiffInfo);
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
