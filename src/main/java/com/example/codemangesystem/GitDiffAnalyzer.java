package com.example.codemangesystem;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class GitDiffAnalyzer {
    private final File repoDir;

    public GitDiffAnalyzer(File repoDir) {
        this.repoDir = repoDir;
    }

    // 讀取每段 diff 的資訊
    public void analyzeCommits() {
        try (Git git = Git.open(repoDir)) {
            Repository repository = git.getRepository();
            Iterable<RevCommit> commits = git.log().call();

            for (RevCommit commit : commits) {
                System.out.println("Commit: " + commit.getName());
                System.out.println("Timestamp: " + commit.getCommitTime());
                System.out.println("Diff:");

                ObjectId commitId = commit.getId();
                try (RevWalk revWalk = new RevWalk(repository)) {
                    RevCommit parent = commit.getParentCount() > 0 ? revWalk.parseCommit(commit.getParent(0)) : null;
                    AbstractTreeIterator newTree = prepareTreeParser(repository, commitId);
                    AbstractTreeIterator oldTree = parent != null ? prepareTreeParser(repository, parent.getId()) : new CanonicalTreeParser();

                    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                         DiffFormatter formatter = new DiffFormatter(out)) {
                        formatter.setRepository(repository);
                        List<DiffEntry> diffs = formatter.scan(oldTree, newTree);

                        for (DiffEntry entry : diffs) {
                            formatter.format(entry);
                            System.out.println(out.toString(StandardCharsets.UTF_8));
                            out.reset();
                        }
                    }
                }
            }
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(objectId);
            ObjectId treeId = commit.getTree().getId();
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, treeId);
            }
            return treeParser;
        }
    }

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
