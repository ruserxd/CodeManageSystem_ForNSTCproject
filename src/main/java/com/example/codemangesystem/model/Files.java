package com.example.codemangesystem.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 用 lombok 減少程式碼的撰寫、增加可讀性
@Builder
@Getter
@Setter
public class Files {
    private final String fileName;
    private final String filePath;
    // key : functionName
    private List<Method> methods;
}