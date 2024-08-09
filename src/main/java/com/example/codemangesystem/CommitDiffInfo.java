package com.example.codemangesystem;

// CommitDiffInfo 類別用來儲存 commit 的差異資訊 (檔名、作者、時間戳、原始程式碼、差異、commitId)
public class CommitDiffInfo {
    private final String filename;
    private final String author;
    private final long timestamp;
    private final String originalCode;
    private final String diff;
    private final String commitId;

    public CommitDiffInfo(String filename, String author, long timestamp, String originalCode, String diff, String commitId) {
        this.filename = filename;
        this.author = author;
        this.timestamp = timestamp;
        this.originalCode = originalCode;
        this.diff = diff;
        this.commitId = commitId;
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

}