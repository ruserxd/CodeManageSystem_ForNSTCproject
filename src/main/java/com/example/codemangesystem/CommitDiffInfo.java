package com.example.codemangesystem;

// CommitDiffInfo 類別用來儲存 commit 的差異資訊 (檔名、作者、時間戳、原始程式碼、差異、commitId、儲存新增和移除的程式碼)
public class CommitDiffInfo {
    private final String filename;
    private final String author;
    private final long timestamp;
    private final String originalCode;
    private final String diff;
    private final String commitId;

    // 以下兩個用來儲存新增和移除的程式碼
    private String addedLines;
    private String removedLines;

    public CommitDiffInfo(String filename, String author, long timestamp, String originalCode, String diff, String commitId) {
        this.filename = filename;
        this.author = author;
        this.timestamp = timestamp;
        this.originalCode = originalCode;
        this.diff = diff;
        this.commitId = commitId;
        categorizeDiff();
    }

    // 省去 + -
    private void categorizeDiff() {
        StringBuilder added = new StringBuilder();
        StringBuilder removed = new StringBuilder();

        String[] lines = diff.split("\n");
        for (String line : lines) {
            if (line.startsWith("+") && !line.startsWith("+++")) {
                added.append(line.substring(1)).append("\n");
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                removed.append(line.substring(1)).append("\n");
            }
        }

        this.addedLines = added.toString();
        this.removedLines = removed.toString();
    }

    public String getFilename() {
        return filename;
    }

    public String getAuthor() {
        return author;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getOriginalCode() {
        return originalCode;
    }

    public String getDiff() {
        return diff;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getAddedLines() {
        return addedLines;
    }

    public String getRemovedLines() {
        return removedLines;
    }
    public void setAddedLines(String addedLines) {
        this.addedLines = addedLines;
    }

    public void setRemovedLines(String removedLines) {
        this.removedLines = removedLines;
    }
}