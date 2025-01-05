package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_DataBase.*;
import com.codemangesystem.gitProcess.model_Git.GitResult;
import com.codemangesystem.gitProcess.model_Git.GitStatus;
import com.codemangesystem.gitProcess.repository.PersonalRepository;
import com.codemangesystem.gitProcess.repository.ProjectRepository;
import com.codemangesystem.loginProcess.model_user.MyUser;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

// TODO: 單純方法間的比較，那變數, 初始化該如何處理

/**
 * 獲取 Git 每段 commit 的方法差異
 */
@Slf4j
@Service
public class GitDiffAnalyzer {
    private final ProjectRepository projectRepository;
    private final PersonalRepository personalRepository;

    @Autowired
    public GitDiffAnalyzer(ProjectRepository projectRepository, PersonalRepository personalRepository) {
        this.projectRepository = projectRepository;
        this.personalRepository = personalRepository;
    }

    /**
     * 第一次 clone 下來時，讀取每段 commit diff 的資訊並解析成以方法名稱
     * 作為查詢的條件
     *
     * @Transactional 確保資料的一致性
     */
    @Transactional
    public GitResult analyzeAllCommits(String repoPath, MyUser user) throws GitAPIException, IOException {
        try {
            // 路徑上該專案的 .git 檔案
            File gitDir = new File(repoPath, ".git");

            // 確保本地端有這個專案
            if (!gitDir.exists() || !gitDir.isDirectory()) {
                log.error("The specified path does not contain a valid Git repository: {}", repoPath);
                return GitResult.builder()
                                .message("分析時，本地端沒有此檔案")
                                .status(GitStatus.ANALYSIS_FAILED)
                                .build();
            }

            // 一個 Repository 物件，指向 repoDir 上的 .git 檔案
            Repository repository = new RepositoryBuilder().setGitDir(gitDir)
                                                           .build();

            // 確保至少有一次 commit 紀錄
            if (repository.resolve("HEAD") == null) {
                log.error("Repository has no commits (No HEAD). Please make an initial commit.");
                return GitResult.builder()
                                .message("分析時，此資料沒有 commit 的紀錄")
                                .status(GitStatus.ANALYSIS_FAILED)
                                .build();
            }

            // 最後要存入資料庫內的 Project 物件，並透過 getHeadName 將 Head 的 SHA-1 存入
            Project project = Project.builder()
                                     .projectName(repoPath.substring(repoPath.lastIndexOf('/') + 1))
                                     .files(new ArrayList<>())
                                     .headRevstr(getHeadSHA1(repository))
                                     .build();

            // Head 的 headRevstr
            String firstCommitSHA = "";

            // Git 打開 repository
            try (Git git = new Git(repository)) {
                log.info("開始獲取 {} 上的 commit 的差異資訊", repoPath);

                // 這邊的操作，像是在 terminal 打上 git log 獲取每個 commit 的相關資訊
                Iterable<RevCommit> commits = git.log()
                                                 .call();
                int counterDiffence = 0;
                // 獲取兩個版本之間的差異
                for (RevCommit commit : commits) {
                    if (counterDiffence == 0)
                        firstCommitSHA = commit.getName();
                    String message = commit.getFullMessage();
                    log.info("本次的 Commit Message : {}", message);

                    // 因為 commit 最後一次指向最一開始，所以會出現沒有父節點的情況
                    RevCommit previousCommit = commit.getParentCount() > 0 ? commit.getParent(0) : null;

                    List<DiffEntry> diffs = getCommitDiffList(commit, git, repository, previousCommit);

                    setCommitDiff(diffs, project, git, commit, previousCommit);
                    counterDiffence++;
                }
                log.info("共有 {} 個差異", counterDiffence);
            }

            log.info("成功獲取所有 commit diff 的資訊");
            log.info("將資料存入 Project 資料庫內");
            projectRepository.save(project);

            log.info("將資料存入 personal_repository 資料庫內");
            PersonalINFO personalINFO = PersonalINFO.builder()
                                                    .user(user)
                                                    .project(project)
                                                    .headRevstr(firstCommitSHA)
                                                    .build();
            personalRepository.save(personalINFO);

            log.info("成功將資料分類完成");
            // 這不代表是錯誤，可能是專案非 Java 檔案
            if (project.getFiles() == null || project.getFiles().isEmpty()) {
                log.warn("No files were analyzed in the repository: {}", repoPath);
                return GitResult.builder()
                                .status(GitStatus.CLONE_SUCCESS)
                                .message("在 " + repoPath + " 未出現可分析方法，可能是沒有方法可以分類")
                                .build();
            }

            return GitResult.builder()
                            .status(GitStatus.CLONE_SUCCESS)
                            .message("成功將資料分類完成")
                            .build();
        } catch (IOException error) {
            log.error("分析 commits 出現問題 {}", error.getMessage());
            return GitResult.builder()
                            .status(GitStatus.ANALYSIS_FAILED)
                            .message("分析時，發生 " + error.getMessage())
                            .build();
        } catch (GitAPIException error) {
            log.error("嘗試使用 Git 出現問題 {}", error.getMessage());
            return GitResult.builder()
                            .status(GitStatus.ANALYSIS_FAILED)
                            .message("分析時，發生 " + error.getMessage())
                            .build();
        }
    }

    /**
     * 如果執行 pull 只需要分析部分的 commit 即可
     */
    @Transactional
    public GitResult analyzePartCommits(String repoPath, String oldHeadRevstr) throws IOException {
        try {
            log.info("部分分析 {} {}", repoPath, oldHeadRevstr);

            // 路徑上該專案的 .git 檔案
            File gitDir = new File(repoPath, ".git");

            // 確保本地端有這個專案
            if (!gitDir.exists() || !gitDir.isDirectory()) {
                log.error("The specified path does not contain a valid Git repository at {}", repoPath);
                return GitResult.builder()
                                .message("部分分析時，本地端沒有此檔案")
                                .status(GitStatus.ANALYSIS_FAILED)
                                .build();
            }

            // 一個 Repository 物件，指向 repoDir 上的 .git 檔案
            Repository repository = new RepositoryBuilder()
                    .setGitDir(gitDir)
                    .build();
            String projectName = repoPath.substring(repoPath.lastIndexOf('/') + 1);
            Project project = projectRepository.findByProjectName(projectName).orElse(null);
            if (project != null) {
                log.warn("成功獲取 {}", projectName);
            } else {
                log.warn("獲取 {} 失敗", projectName);
                return GitResult.builder()
                                .message("資料庫沒有此檔案")
                                .status(GitStatus.ANALYSIS_FAILED)
                                .build();
            }

            // update user project git head
            project.setHeadRevstr(getHeadSHA1(repository));
            projectRepository.save(project);

            // Git 打開 repository
            try (Git git = new Git(repository)) {
                log.info("開始獲取 [{}] 上的 commit 的差異資訊", repoPath);

                // 這邊的操作，像是在 terminal 打上 git log 獲取每個 commit 的相關資訊
                Iterable<RevCommit> commits = git.log()
                                                 .call();

                // 獲取兩個版本之間的差異
                for (RevCommit commit : commits) {
                    String message = commit.getFullMessage();
                    String name = commit.getName();
                    log.info("{} {}", message, name);

                    // 當 commit 走過，break 發生
                    if (commit.getName().equals(oldHeadRevstr)) {
                        break;
                    }

                    // 因為 commit 最後一次指向最一開始，所以會出現沒有父節點的情況
                    RevCommit previousCommit = commit.getParentCount() > 0 ? commit.getParent(0) : null;

                    List<DiffEntry> diffs = getCommitDiffList(commit, git, repository, previousCommit);

                    setCommitDiff(diffs, project, git, commit, previousCommit);
                }
            }

            return GitResult.builder()
                            .status(GitStatus.PULL_SUCCESS)
                            .message("成功 Pull 並更新資料")
                            .build();
        } catch (IOException e) {
            log.error("分析 part commits 出現問題 {}", e.getMessage());
            return GitResult.builder()
                            .status(GitStatus.ANALYSIS_FAILED)
                            .message("部分分析時，發生 " + e)
                            .build();
        } catch (GitAPIException e) {
            log.error("分析 part commits 時，嘗試使用 Git 出現問題 {}", e.getMessage());
            return GitResult.builder()
                            .status(GitStatus.ANALYSIS_FAILED)
                            .message("部分分析時，發生 " + e)
                            .build();
        }
    }

    /* 獲取新舊的 commit 差異資訊
     * */
    public List<DiffEntry> getCommitDiffList(RevCommit commit, Git git, Repository repository, RevCommit previousCommit) throws IOException, IllegalStateException {
        try {
            /* 透過 git diff [oldCommit] [newCommit] 找出兩個 commit 差異的資訊
             * 若未有 previous 可以做比較則設置一個空的 (Iterator over an empty tree (a directory with no files))
             * */
            AbstractTreeIterator oldTree = previousCommit != null ? prepareTreeParser(repository, previousCommit) : new EmptyTreeIterator();
            AbstractTreeIterator newTree = prepareTreeParser(repository, commit);

            return git.diff()
                      .setOldTree(oldTree)
                      .setNewTree(newTree)
                      .call();
        } catch (IOException e) {
            throw new IOException(e);
        } catch (GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 獲取 新版本, 舊版本的差異，並將 diff 資料放入 project 內
     * diffs -> 新版本, 舊版本的差異資訊
     */
    public void setCommitDiff(List<DiffEntry> diffs, Project project, Git git, RevCommit commit, RevCommit previousCommit) throws IOException {
        try {
            // 完整執行此次 commit 檔案有 diff 的
            for (DiffEntry diff : diffs) {
                // 目前的檔案位址、名稱，省去後面存取 diff 的呼叫
                String filePath = diff.getNewPath();
                String fileName = new File(diff.getNewPath()).getName();

                // 專注處理 java 檔案
                if (diff.getNewPath()
                        .endsWith(".java")) {

                    log.info("嘗試比較 {}", diff.getNewPath());

                    // 以 String 抓出此次 commit 版與前一次的內容
                    String newContent = getFileContent(git, diff.getNewPath(), commit);
                    String oldContent = previousCommit != null ? getFileContent(git, diff.getNewPath(), previousCommit) : "";

                    // 獲取本次 commit 的方法差異
                    List<Pair<String, String>> result = compareTwoContent(newContent, oldContent);

                    // <方法名稱, 方法差異>
                    for (Pair<String, String> methodDiff : result) {
                        // 有 diff 我們在去做存取
                        if (!Objects.equals(methodDiff.getValue(), "")) {
                            DiffInfo diffInfo = takeCommitINFO(commit);
                            diffInfo.setDiffCode(methodDiff.getValue());
                            addDiffInfoInToProject(filePath, fileName, methodDiff.getKey(), diffInfo, project);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("當獲取 {} 出現 {}", commit.getId().getName(), e.getMessage());
            throw new IOException(e);
        }
    }

    // 獲取 Head 的 SHA-1
    public String getHeadSHA1(Repository repo) throws IOException {
        String headRevstr;
        try {
            // Eclipse
            // Parse a git revision string and return an object id
            // string form of the SHA-1, in lower case hexadecimal
            ObjectId commit = repo.resolve(Constants.HEAD);
            headRevstr = commit.getName();
            log.info("得到了，Git Commit Head 的 SHA-1 值 {}", headRevstr);
        } catch (IOException error) {
            log.error("獲取 Head 的 SHA-1 出錯誤 ", error);
            throw new IOException(error);
        }
        return headRevstr;
    }

    // 回傳一個 AbstractTreeIterator
    public static AbstractTreeIterator prepareTreeParser(Repository repository, RevCommit commit) throws IOException {
        // 從 commits tree 上獲得一個 reference
        RevTree tree = commit.getTree();
        try (ObjectReader reader = repository.newObjectReader()) {
            CanonicalTreeParser treeParser = new CanonicalTreeParser();

            // .reset(ObjectReader reader, AnyObjectId id)，透過 SHA-1 哈希值，使 CanonicalTreeParser 指到指定的樹位置
            treeParser.reset(reader, tree.getId());
            return treeParser;
        } catch (IOException e) {
            log.error("Git 語法樹上出現問題: ", e);
        }
        log.error("失敗回傳 null");
        return null;
    }

    // 創立 DiffInfo 並存入修改程式碼的作者、email、時間、commit
    public static DiffInfo takeCommitINFO(RevCommit commit) {
        //從 commit 獲取資訊
        PersonIdent author = commit.getAuthorIdent();
        Date commitTime = author.getWhen();
        String commitMessage = commit.getFullMessage();
        String headRevstr = commit.getName();
        int timeStamp = commit.getCommitTime();

        return DiffInfo.builder()
                       .author(author.getName())
                       .authorEmail(author.getEmailAddress())
                       .commitMessage(commitMessage)
                       .timestamp(timeStamp)
                       .commitTime(commitTime)
                       .headRevstr(headRevstr)
                       .build();
    }

    // 獲取某段 commit 的整個檔案內的資料(程式碼)
    public static String getFileContent(Git git, String path, RevCommit commit) throws IOException {
        try (TreeWalk treeWalk = TreeWalk.forPath(git.getRepository(), path, commit.getTree())) {
            if (treeWalk != null) {
                ObjectLoader objectLoader = git.getRepository()
                                               .open(treeWalk.getObjectId(0));
                byte[] bytes = objectLoader.getBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            }
            return "";
        }
    }

    // 比較新、舊文件所有函式的差異性 List<Pair<方法, 方法差異>>
    public List<Pair<String, String>> compareTwoContent(String newContent, String oldContent) {
        List<Pair<String, String>> differences = new ArrayList<>();

        Map<String, String> newMethods = getMethodByContent(newContent);
        Map<String, String> oldMethods = getMethodByContent(oldContent);

        // 新版本與舊版本的對照
        for (Map.Entry<String, String> newMethod : newMethods.entrySet()) {
            //兩段的方法名稱為相同
            String methodName = newMethod.getKey();

            /* 獲取方法的程式碼，舊版本可能為空，因此要避免
             *  如果舊的方法是空的我們會把他設為空字串，表示新增
             *  若非空的就抓取方法內的程式碼
             * */
            String newMethodBody = newMethod.getValue();
            String oldMethodBody = Objects.requireNonNullElse(oldMethods.get(methodName), "");

            differences.add(Pair.of(methodName, generateGitDiff(oldMethodBody, newMethodBody)));
        }

        // 例外: 會出現舊版本有，但新版沒有，代表這個方法被刪減
        for (Map.Entry<String, String> oldMethod : oldMethods.entrySet()) {
            String oldMethodName = oldMethod.getKey();
            if (!newMethods.containsKey(oldMethodName)) {
                String newMethodBody = "";
                String oldMethodBody = oldMethod.getValue();
                differences.add(Pair.of(oldMethodName, generateGitDiff(oldMethodBody, newMethodBody)));
            }
        }

        return differences;
    }

    /*
     * 獲取這個文件內的 HashMap<方法, 方法內容>
     * */
    public Map<String, String> getMethodByContent(String content) {
        try {
            /* 不使用 staticJavaParser(Java 3.0.0)
             * 為了避免掉版本不相容的問題
             * 自訂 JavaParser 設定
             * */
            final ParserConfiguration parserConfiguration = new ParserConfiguration();
            parserConfiguration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);

            JavaParser javaParser = new JavaParser(parserConfiguration);

            // 獲取程式碼的語法樹的頭節點
            CompilationUnit cuNode = javaParser.parse(content)
                                               .getResult()
                                               .filter(result -> result.findCompilationUnit()
                                                                       .isPresent())
                                               .flatMap(Node::findCompilationUnit)
                                               .orElse(null);

            log.info("本次的語法樹結構");
            log.info(String.valueOf(cuNode));
            // 確保 cu 不為 null
            assert cuNode != null;

            // 找出所有的方法資訊，並存入 list 內
            List<MethodDeclaration> methods = cuNode.findAll(MethodDeclaration.class);

            // 將獲得的資訊分為 Key: 方法名稱 Value: 該方法內容
            Map<String, String> contentMethods = new HashMap<>();
            for (MethodDeclaration method : methods) {
                // 運用 StringBuilder 因為我們將大量對文件做相對應處理
                StringBuilder methodContent = new StringBuilder(new StringBuilder());

                // 獲得註解的部分，e.g., @Service, @Controller etc.
                List<AnnotationExpr> annotations = method.getAnnotations();
                for (AnnotationExpr annotation : annotations) {
                    methodContent.append(annotation.toString())
                                 .append('\n');
                }

                // 獲得方法的 (回傳類型, 名稱, 參數 etc.)
                methodContent.append(method.getDeclarationAsString(true, true, true));

                // 獲得方法的內容 { 方法內容 }
                method.getBody()
                      .ifPresent(body -> methodContent.append(' ')
                                                      .append(body));

                contentMethods.put(method.getNameAsString(), methodContent.toString());
            }
            return contentMethods;
        } catch (ParseProblemException e) {
            log.error("Parser文件時發生錯誤：{}", e.getMessage());
            return new HashMap<>();
        }
    }

    // 對比兩個方法，透過 java-diff-utils 去完成
    public static String generateGitDiff(String oldMethod, String newMethod) {
        // difflib 前面幾行不用
        final int nonSelectedLines = 2;

        // 把方法每行轉 list
        List<String> oldLines = List.of(oldMethod.split("\n"));
        List<String> newLines = List.of(newMethod.split("\n"));

        // java diff 使用
        Patch<String> patch = DiffUtils.diff(oldLines, newLines);

        // 3 代表上下文非差異的程式碼行數
        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("OldVersionMethod.java",
                "NewVersionMethod.java", oldLines, patch, 3);

        // 將 --OldVersionMethod.java ++NewVersionMethod.java 刪除因為我們這邊比較的是方法，檔案會一致
        if (unifiedDiff.size() > nonSelectedLines) {
            unifiedDiff.subList(0, nonSelectedLines)
                       .clear();
        }
        // 把 list 透過 \n 拆成一個 String
        return String.join("\n", unifiedDiff);
    }

    // 將資料放入 Project
    public void addDiffInfoInToProject(String filePath, String fileName, String methodName, DiffInfo diffInfo, Project project) {
        // 找尋 project 內相對應的 file
        Files file = null;
        for (Files projectFile : project.getFiles()) {
            if (projectFile.getFilePath()
                           .equals(filePath)) {
                file = projectFile;
                break;
            }
        }

        // 未找到創立一個新的 file 並放入 project 內
        if (file == null) {
            log.info("創立新的 file {}", fileName);
            file = Files.builder()
                        .fileName(fileName)
                        .filePath(filePath)
                        .methods(new ArrayList<>())
                        .project(project)
                        .build();
            project.getFiles()
                   .add(file);
        }

        List<Method> methods = file.getMethods();

        // 當有找到對應的方法時，加入 diffInfo
        for (Method method : methods) {
            if (method.getMethodName()
                      .equals(methodName)) {
                diffInfo.setMethod(method);
                method.getDiffInfoList()
                      .add(diffInfo);
                return;
            }
        }

        log.info("創立新的 method {}", methodName);
        // 未找到先創立一個新的 method，接著存放 diffInfo，最後將 method 放入 methods 內
        Method newMethod = Method.builder()
                                 .methodName(methodName)
                                 .files(file)
                                 .diffInfoList(new ArrayList<>())
                                 .build();
        log.info("method : {}", newMethod);
        diffInfo.setMethod(newMethod);

        newMethod.getDiffInfoList()
                 .add(diffInfo);
        methods.add(newMethod);
    }
}