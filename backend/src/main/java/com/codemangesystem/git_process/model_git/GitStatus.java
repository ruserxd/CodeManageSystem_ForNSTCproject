package com.codemangesystem.git_process.model_git;

/**
 * Git 操作後的狀態
 */
public enum GitStatus {
    CLONE_SUCCESS,
    PULL_SUCCESS,
    PULL_FAILED,
    CLONE_FAILED,
    DATABASE_FAILED,
    ANALYSIS_FAILED
}
