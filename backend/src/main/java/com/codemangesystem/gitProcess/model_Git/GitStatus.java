package com.codemangesystem.gitProcess.model_Git;

/**
 * 格式化回傳的 clone 狀態
 */
public enum GitStatus {
    CLONE_SUCCESS,
    PULL_SUCCESS,
    PULL_FAILED,
    CLONE_FAILED,
    ANALYSIS_FAILED,
    ERROR
}
