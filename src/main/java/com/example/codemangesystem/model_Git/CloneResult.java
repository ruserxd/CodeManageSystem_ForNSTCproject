package com.example.codemangesystem.model_Git;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CloneResult {
    private final CloneStatus status;
    private final String path;
}
