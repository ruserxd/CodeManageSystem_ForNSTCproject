package com.codemangesystem.gitProcess.model_Repo;

import lombok.Builder;
import lombok.Setter;

/**
 * 本地的 Git Repository 相關資料
 */
@Setter
@Builder
public class RepositoryINFO {
    public String repoName;
    public String localPath;
}
