package com.example.codemangesystem.GitProcess.service;

import com.example.codemangesystem.GitProcess.model_Data.DiffInfo;
import com.example.codemangesystem.GitProcess.model_Data.Files;
import com.example.codemangesystem.GitProcess.model_Data.Method;
import com.example.codemangesystem.GitProcess.model_Data.Project;
import com.example.codemangesystem.GitProcess.repository.ProjectRepository;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class GitDiffAnalyzer {
    private final ProjectRepository projectRepository;

    @Autowired
    public GitDiffAnalyzer(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(GitDiffAnalyzer.class);

    // 讀取每段 commit diff 的資訊並解析成以方法名稱
    public List<Files> analyzeCommits(String url) {
        try {
            // 路徑上該專案的 .git 檔案
            File gitDir = new File(url, ".git");

            // 確保本地端有這個專案
            if (!gitDir.exists() || !gitDir.isDirectory()) {
                logger.error("The specified path does not contain a valid Git repository: " + url);
                return Collections.emptyList();
            }

            // 最後要存入資料庫內的 Project 物件
            Project project = Project.builder()
                    .projectName(url.substring(url.lastIndexOf('/') + 1))
                    .files(new LinkedList<>())
                    .build();

            // 一個 Repository 物件，指向 repoDir 上的 .git 檔案
            Repository repository = new RepositoryBuilder()
                    .setGitDir(gitDir)
                    .build();

            // 開始獲取 commit diff
            try (Git git = new Git(repository)) {
                logger.info("開始獲取 [{}] 上的 commit 的差異資訊", url);

                // 確保至少有一次 commit 紀錄
                if (repository.resolve("HEAD") == null) {
                    logger.error("Repository has no commits (No HEAD). Please make an initial commit.");
                    return Collections.emptyList();
                }

                // 這邊的操作，像是在 terminal 打上 git log 獲取每個 commit 的相關資訊
                Iterable<RevCommit> commits = git.log().call();
                // 獲取兩個版本之間的差異
                for (RevCommit commit : commits) {
                    List<DiffEntry> diffs;
                    RevCommit previousCommit = commit.getParentCount() > 0 ? commit.getParent(0) : null;

                    // 透過 git diff [oldCommit] [newCommit] 找出兩個 commit 差異的資訊
                    // 若未有 previous 可以做比較則設置一個空的 (Iterator over an empty tree (a directory with no files))
                    AbstractTreeIterator oldTree = previousCommit != null ? prepareTreeParser(repository, previousCommit) : new EmptyTreeIterator();
                    AbstractTreeIterator newTree = prepareTreeParser(repository, commit);
                    diffs = git.diff().setOldTree(oldTree).setNewTree(newTree).call();

                    // 完整執行此次 commit 檔案有 diff 的
                    for (DiffEntry diff : diffs) {
                        // 目前的檔案位址、名稱，省去後面存取 diff 的呼叫
                        String filePath = diff.getNewPath();
                        String fileName = new File(diff.getNewPath()).getName();

                        // 專注處理 java 檔案
                        if (diff.getNewPath().endsWith(".java")) {

                            logger.info("嘗試比較 " + diff.getNewPath());
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
                }
            }

            logger.info("成功獲取所有 commit diff 的資訊");

            projectRepository.save(project);
            return project.getFiles();
        } catch (IOException | GitAPIException e) {
            logger.error("分析 commits 出現問題");
            throw new RuntimeException(e);
        }
    }

    // 回傳一個 AbstractTreeIterator
    public static AbstractTreeIterator prepareTreeParser(Repository repository, RevCommit commit) throws IOException {
        // 從 commit's tree 上獲得一個 reference
        RevTree tree = commit.getTree();
        try (ObjectReader reader = repository.newObjectReader()) {
            CanonicalTreeParser treeParser = new CanonicalTreeParser();

            // .reset(ObjectReader reader, AnyObjectId id)，透過 SHA-1 哈希值，使 CanonicalTreeParser 指到指定的樹位置
            treeParser.reset(reader, tree.getId());
            return treeParser;
        }
    }

    // 創立 DiffInfo 並存入修改程式碼的作者、email、時間、commit
    public static DiffInfo takeCommitINFO(RevCommit commit) {
        //從 commit 獲取資訊
        PersonIdent author = commit.getAuthorIdent();
        Date commitTime = author.getWhen();
        String commitMessage = commit.getFullMessage();

        return DiffInfo.builder().author(author.getName()).authorEmail(author.getEmailAddress()).commitMessage(commitMessage).timestamp(commit.getCommitTime()).commitTime(commitTime).build();
    }

    // 獲取某段 commit 的整個檔案內的資料(程式碼)
    public static String getFileContent(Git git, String path, RevCommit commit) throws IOException {
        try (TreeWalk treeWalk = TreeWalk.forPath(git.getRepository(), path, commit.getTree())) {
            if (treeWalk != null) {
                ObjectLoader objectLoader = git.getRepository().open(treeWalk.getObjectId(0));
                byte[] bytes = objectLoader.getBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            }
            return "";
        }
    }

    // 比較新、舊文件所有函式的差異性 List<Pair<方法, 方法差異>>
    public List<Pair<String, String>> compareTwoContent(String newContent, String oldContent) {
        List<Pair<String, String>> differences = new ArrayList<>();

        HashMap<String, String> newMethods = getContentMethod(newContent);
        HashMap<String, String> oldMethods = getContentMethod(oldContent);

        // 新版本與舊版本的對照
        for (Map.Entry<String, String> newMethod : newMethods.entrySet()) {
            //兩段的方法名稱為相同
            String MethodName = newMethod.getKey();

            // 獲取方法的程式碼，舊版本可能為空，因此要避免
            String newMethodBody = newMethod.getValue();
            String oldMethodBody = Objects.requireNonNullElse(oldMethods.get(MethodName), "");

            differences.add(Pair.of(MethodName, generateLikeGitDiff(oldMethodBody, newMethodBody)));
        }

        // 例外: 會出現舊版本有，但新版沒有，代表這個方法被刪減
        for (Map.Entry<String, String> oldMethod : oldMethods.entrySet()) {
            String oldMethodName = oldMethod.getKey();
            if (!newMethods.containsKey(oldMethodName)) {
                String newMethodBody = "";
                String oldMethodBody = oldMethod.getValue();
                differences.add(Pair.of(oldMethodName, generateLikeGitDiff(oldMethodBody, newMethodBody)));
            }
        }

        return differences;
    }

    // 獲取這個文件內的 HashMap<方法, 方法內容>
    public HashMap<String, String> getContentMethod(String Content) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(Content);
            List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
            HashMap<String, String> ContentMethods = new HashMap<>();
            for (MethodDeclaration method : methods) {
                StringBuilder methodContent = new StringBuilder(new StringBuilder());

                List<AnnotationExpr> annotations = method.getAnnotations();
                for (AnnotationExpr annotation : annotations) {
                    methodContent.append(annotation.toString()).append("\n");
                }
                methodContent.append(method.getDeclarationAsString(true, true, true));
                method.getBody().ifPresent(body -> methodContent.append(" ").append(body));
                ContentMethods.put(method.getNameAsString(), methodContent.toString());
            }
            return ContentMethods;
        } catch (ParseProblemException e) {
            // TODO: 解決這項問題 If you need that feature the language level must be configured in the configuration before parsing the source files.
            logger.error("Parser文件時發生錯誤：" + e.getMessage());
            return new HashMap<>();
        }
    }

    // 對比兩個方法，透過 java-diff-utils 去完成
    private static String generateLikeGitDiff(String oldMethod, String newMethod) {
        List<String> oldLines = List.of(oldMethod.split("\n"));
        List<String> newLines = List.of(newMethod.split("\n"));

        Patch<String> patch = DiffUtils.diff(oldLines, newLines);

        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("OldVersionMethod.java", "NewVersionMethod.java", oldLines, patch, 3       //上下文的差異數量
        );
        // 將 --OldVersionMethod.java ++NewVersionMethod.java 刪除因為我們這邊比較的是方法，檔案會一致
        if (unifiedDiff.size() > 2) unifiedDiff.subList(0, 2).clear();

        // 把 list 透過 \n 拆成一個 String
        return String.join("\n", unifiedDiff);
    }

    // 將資料放入 Project
    public void addDiffInfoInToProject(String filePath, String fileName, String methodName, DiffInfo diffInfo, Project project) {

        // 找尋 project 內相對應的 file
        Files file = null;
        for (Files projectFile : project.getFiles()) {
            if (projectFile.getFilePath().equals(filePath)) {
                file = projectFile;
                break;
            }
        }

        // 未找到創立一個新的 file 並放入 project 內
        if (file == null) {
            file = Files.builder().fileName(fileName).filePath(filePath).methods(new LinkedList<>()).project(project).build();
            project.getFiles().add(file);
        }

        List<Method> methods = file.getMethods();

        // 當有找到對應的方法時，加入 diffInfo
        for (Method method : methods) {
            if (method.getMethodName().equals(methodName)) {
                diffInfo.setMethod(method);
                method.getDiffInfoList().add(diffInfo);
                return;
            }
        }

        // 未找到先創立一個新的 method，接著存放 diffInfo，最後將 method 放入 methods 內
        Method newMethod = Method.builder().methodName(methodName).files(file).diffInfoList(new LinkedList<>()).build();
        diffInfo.setMethod(newMethod);

        newMethod.getDiffInfoList().add(diffInfo);
        methods.add(newMethod);
    }
}