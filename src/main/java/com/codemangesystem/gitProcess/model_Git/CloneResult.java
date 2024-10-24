package com.codemangesystem.gitProcess.model_Git;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CloneResult {
    private final CloneStatus status;
    private final String message;
}
