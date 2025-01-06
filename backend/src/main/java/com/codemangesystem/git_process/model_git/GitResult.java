package com.codemangesystem.git_process.model_git;

import lombok.Builder;
import lombok.Data;

/**
 * clone 結束後的物件回傳結果
 */
@Builder
@Data
public class GitResult {
    private final GitStatus status;
    private String message;

    public boolean isPullFailed() {
        return this.status != GitStatus.PULL_SUCCESS;
    }
}
