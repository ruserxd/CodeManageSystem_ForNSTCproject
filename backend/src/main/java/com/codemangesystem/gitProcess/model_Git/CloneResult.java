package com.codemangesystem.gitProcess.model_Git;

import lombok.Builder;
import lombok.Data;

/**
 * clone 結束後的物件回傳結果
 */
@Builder
@Data
public class CloneResult {
    private final CloneStatus status;
    private final String message;
}
