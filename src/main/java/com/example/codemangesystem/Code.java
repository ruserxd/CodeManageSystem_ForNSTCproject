package com.example.codemangesystem;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

// 用 lombok 減少程式碼的撰寫、增加可讀性
@Builder
@Getter
@Setter
public class Code {
    private final String fileName;
    private Map<String, List<DiffInfo>> functions;
}