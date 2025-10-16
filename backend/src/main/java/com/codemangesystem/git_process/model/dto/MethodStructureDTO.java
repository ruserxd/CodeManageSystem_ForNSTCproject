package com.codemangesystem.git_process.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethodStructureDTO {
  private Long methodId;
  private String methodName;
  private Integer diffCount;
}