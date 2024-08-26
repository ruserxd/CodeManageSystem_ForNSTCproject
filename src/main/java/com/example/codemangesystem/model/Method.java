package com.example.codemangesystem.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
public class Method {
    private String methodName;
    List<DiffInfo> diffInfoList;
}
