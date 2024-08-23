package com.example.codemangesystem.service;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GitDiffAnalyzer {
    private final File repoDir;

    public GitDiffAnalyzer(File repoDir) {
        this.repoDir = repoDir;
    }

    // 讀取每段 diff 的資訊並解析方法名稱
    public void analyzeCommits() {
        try {
            // 建立一個 repository 物件，指向 repoDir 上的 .git 檔案
            Repository repository = new RepositoryBuilder()
                    .setGitDir(new File(repoDir, ".git"))
                    .build();

            try (Git git = new Git(repository)) {
                // 這邊的操作，像是在 terminal 打上 git log 獲取每個 commit 的相關資訊
                Iterable<RevCommit> commits = git.log().call();

                int commitCounter = 1;
                // 獲取兩個版本之間的差異
                for (RevCommit commit : commits) {
                    System.out.println("CommitTime: " + commitCounter++ + "\n\n");
                    // 獲取這段 commit 的相關資訊
                    takeCommitINFO(commit);

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

                    // 執行此次 commit 對比出的每個檔案的 diff
                    for (DiffEntry diff : diffs) {
                        // 如果你想進一步處理 Java 文件
                        if (diff.getNewPath().endsWith(".java")) {
                            System.out.println("---------------------");

                            System.out.println("File: " + diff.getNewPath() + "\nFileName: " + new File(diff.getNewPath()).getName());
                            // diff列出 檔案x 有差異，在 commit 的 檔案x，以 String 抓出(新舊版)內容
                            String newContent = getFileContent(git, diff.getNewPath(), commit);
                            String oldContent = previousCommit != null
                                    ? getFileContent(git, diff.getNewPath(), previousCommit)
                                    : "";
                            List<Pair<String, String>> result = compareTwoContent(newContent, oldContent);
                            for (Pair<String, String> methodDiff : result) {
                                System.out.println("Diff: " + methodDiff.getKey());
                            }
                        }
                    }

                    System.out.println("---------------------");
                }
            }
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
    public static void takeCommitINFO(RevCommit commit) {
        PersonIdent author = commit.getAuthorIdent();
        Date commitTime = author.getWhen();
        String commitMessage = commit.getFullMessage();
        System.out.println("Author: " + author.getName());
        System.out.println("AuthorEmail: " + author.getEmailAddress());
        System.out.println("CommitTime: " + commitTime);
        System.out.println("Commit: \n" + commitMessage);
    }

    // 獲取整個 File 內的文件
    public static String getFileContent(Git git, String path, RevCommit commit) throws IOException {
        try (TreeWalk treeWalk = TreeWalk.forPath(git.getRepository(), path, commit.getTree())) {
            if (treeWalk != null) {
                ObjectLoader objectLoader = git.getRepository().open(treeWalk.getObjectId(0));
                byte[] bytes = objectLoader.getBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            } else {
                return "";
            }
        }
    }

    // 獲取這個文件內的 List<方法,方法的內容>
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

            System.out.println(methodContent);
        }
        return ContentMethods;
    }

    // 比較兩個文件，各有函式的差異性
    public List<Pair<String, String>> compareTwoContent(String newContent, String oldContent) {
        List<Pair<String, String>> differences = new ArrayList<>();

        System.out.println("NewMethods: ");
        HashMap<String, String> newMethods = getContentMethod(newContent);
        System.out.println("OldMethods: ");
        HashMap<String, String> oldMethods = getContentMethod(oldContent);

        for (Map.Entry<String, String> newMethod : newMethods.entrySet()) {
            String newMethodName = newMethod.getKey();

            String newMethodBody = newMethod.getValue();

            String oldMethodBody = Objects.requireNonNullElse(oldMethods.get(newMethodName), "");

            differences.add(Pair.of(newMethodName, generateLikeGitDiff(oldMethodBody, newMethodBody)));

        }

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


        String diffString = String.join("\n", unifiedDiff);
        System.out.println(diffString);
        return diffString;
    }

    // 主程式，用來測試這個類別的可行性
    public static void main(String[] args) {
        Path repoPath = Paths.get("src/cloneCode/JavaSpringBootLearning");
        File repoDir = repoPath.toFile();

        if (!repoDir.exists() || !repoDir.isDirectory()) {
            System.err.println("Path does not exist or is not a directory: " + repoDir.getAbsolutePath());
            return;
        }

        GitDiffAnalyzer analyzer = new GitDiffAnalyzer(repoDir);
        analyzer.analyzeCommits();
    }
}
