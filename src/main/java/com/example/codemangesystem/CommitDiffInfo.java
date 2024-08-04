package com.example.codemangesystem;

// CommitDiffInfo 類別用來儲存 commit 的差異資訊 (檔名、作者、時間戳、原始程式碼、差異)
public class CommitDiffInfo {
    private String filename;
    private String author;
    private long timestamp;
    private String originalCode;
    private String diff;

    public CommitDiffInfo(String filename, String author, long timestamp, String originalCode, String diff) {
        this.filename = filename;
        this.author = author;
        this.timestamp = timestamp;
        this.originalCode = originalCode;
        this.diff = diff;
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

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setOriginalCode(String originalCode) {
        this.originalCode = originalCode;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }
}