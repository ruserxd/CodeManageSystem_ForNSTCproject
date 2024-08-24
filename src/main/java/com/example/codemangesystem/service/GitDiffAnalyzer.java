package com.example.codemangesystem.service;

import com.example.codemangesystem.model.DiffInfo;
import com.example.codemangesystem.model.Files;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
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
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class GitDiffAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(GitDiffAnalyzer.class);
    // 讀取每段 commit diff 的資訊並解析成以方法名稱
    public List<Files> analyzeCommits(String url) {
        try {
            File repoDir = new File(url);
            HashMap<String, Files> project = new HashMap<>();
            // 建立一個 repository 物件，指向 repoDir 上的 .git 檔案
            Repository repository = new RepositoryBuilder()
                    .setGitDir(new java.io.File(repoDir, ".git"))
                    .build();

            try (Git git = new Git(repository)) {

                // 這邊的操作，像是在 terminal 打上 git log 獲取每個 commit 的相關資訊
                Iterable<RevCommit> commits = git.log().call();
                // 獲取兩個版本之間的差異
                for (RevCommit commit : commits) {
                    List<DiffEntry> diffs;
                    RevCommit previousCommit = commit.getParentCount() > 0 ? commit.getParent(0) : null;

                    // 透過 git diff [oldCommit] [newCommit] 找出兩個 commit 差異的資訊
                    // 若未有 previous 可以做比較則設置一個空的 (Iterator over an empty tree (a directory with no files))
                    AbstractTreeIterator oldTree = previousCommit != null
                            ? prepareTreeParser(repository, previousCommit)
                            : new EmptyTreeIterator();
                    AbstractTreeIterator newTree = prepareTreeParser(repository, commit);
                    diffs = git.diff()
                            .setOldTree(oldTree)
                            .setNewTree(newTree)
                            .call();

                    // 完整執行此次 commit 檔案有 diff 的
                    for (DiffEntry diff : diffs) {
                        // 目前的檔案位址、名稱，省去後面存取 diff 的呼叫
                        String filePath = diff.getNewPath();
                        String fileName = new java.io.File(diff.getNewPath()).getName();

                        // 專注處理 java 檔案
                        if (diff.getNewPath().endsWith(".java")) {

                            // 以 String 抓出此次 commit 版與前一次的內容
                            String newContent = getFileContent(git, diff.getNewPath(), commit);
                            String oldContent = previousCommit != null
                                    ? getFileContent(git, diff.getNewPath(), previousCommit)
                                    : "";

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
            logger.info("Successful get all project diff");
            return new ArrayList<>(project.values());
        } catch (IOException | GitAPIException e) {
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

    // 獲取修改程式碼的作者、email、時間、commit
    public static DiffInfo takeCommitINFO(RevCommit commit) {
        PersonIdent author = commit.getAuthorIdent();
        Date commitTime = author.getWhen();
        String commitMessage = commit.getFullMessage();
        return DiffInfo.builder()
                .author(author.getName())
                .authorEmail(author.getEmailAddress())
                .commitMessage(commitMessage)
                .timestamp(commit.getCommitTime())
                .commitTime(commitTime)
                .build();
    }

    // 獲取整個檔案內的文件
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

    // 比較兩個文件所有函式的差異性 List<Pair<方法, 方法差異>>
    public List<Pair<String, String>> compareTwoContent(String newContent, String oldContent) {
        List<Pair<String, String>> differences = new ArrayList<>();

        HashMap<String, String> newMethods = getContentMethod(newContent);
        HashMap<String, String> oldMethods = getContentMethod(oldContent);

        // 新版本與舊版本的對照
        for (Map.Entry<String, String> newMethod : newMethods.entrySet()) {
            String newMethodName = newMethod.getKey();

            String newMethodBody = newMethod.getValue();

            String oldMethodBody = Objects.requireNonNullElse(oldMethods.get(newMethodName), "");

            differences.add(Pair.of(newMethodName, generateLikeGitDiff(oldMethodBody, newMethodBody)));
        }

        // 例外: 會出現舊版本有，但新版沒有，代表被刪減
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

    // 獲取這個文件內的 List<方法, 方法內容>
    public HashMap<String, String> getContentMethod(String Content) {
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
    }

    private static String generateLikeGitDiff(String oldMethod, String newMethod) {
        List<String> oldLines = List.of(oldMethod.split("\n"));
        List<String> newLines = List.of(newMethod.split("\n"));

        Patch<String> patch = DiffUtils.diff(oldLines, newLines);

        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
                "OldVersionMethod.java",
                "NewVersionMethod.java",
                oldLines,
                patch,
                3       //上下文的差異數量
        );
        // 將 --OldVersionMethod.java ++NewVersionMethod.java 刪除因為我們這邊比較的是方法，檔案會一致
        if (unifiedDiff.size() > 2)
            unifiedDiff.subList(0, 2).clear();

        // 把 list 透過 \n 拆成一個 String
        return String.join("\n", unifiedDiff);
    }

    public void addDiffInfoInToProject(String filePath, String fileName, String methodName, DiffInfo diffInfo, HashMap<String, Files> project) {
        // 檢查在 project 中 filePath 是否有對應的 Code，否的話創立一個 Code
        // 取出 Code 資料，將 diffInfo 存入 methods[methodName].add(diffInfo)
        Files file = project.get(filePath) != null
                ? project.get(filePath)
                : Files.builder()
                    .fileName(fileName)
                    .filePath(filePath)
                    .methods(new HashMap<>())
                    .build();
        project.put(filePath, file);

        Map<String, List<DiffInfo>> methods = file.getMethods();

        List<DiffInfo> diffInfos = methods.computeIfAbsent(methodName, k -> new ArrayList<>());

        diffInfos.add(diffInfo);
    }

    public static void outProject(List<Files> project) {
        for (Files files: project) {
            System.out.println(files.getFilePath() + " " +  files.getFileName());
            for (List<DiffInfo> diffInfoList: files.getMethods().values()) {
                for (DiffInfo diffInfo: diffInfoList) {
                    System.out.println(ReflectionToStringBuilder.toString(diffInfo));
                }
            }
        }
    }
    // 主程式，用來測試這個類別的可行性
    public static void main(String[] args) {
        GitDiffAnalyzer analyzer = new GitDiffAnalyzer();
        List<Files> tmp = analyzer.analyzeCommits("src/cloneCode/JavaSpringBootLearning");
        outProject(tmp);
    }
}