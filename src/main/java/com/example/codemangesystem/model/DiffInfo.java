package com.example.codemangesystem.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
public class DiffInfo {
    private final String author;
    private final String authorEmail;
    private final String commit;
    private final long timestamp;
    private List<String> annotations;
    private String diffCode;
}
