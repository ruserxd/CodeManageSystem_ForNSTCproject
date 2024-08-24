package com.example.codemangesystem.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
@Builder
public class DiffInfo {
    private final String author;
    private final String authorEmail;
    private final String commitMessage;
    private final long timestamp;
    private final Date commitTime;
    private String diffCode;
}
