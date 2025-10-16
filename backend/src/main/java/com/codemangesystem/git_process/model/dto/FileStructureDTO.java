package com.codemangesystem.git_process.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileStructureDTO {
  private Long filesId;
  private String fileName;
  private String filePath;
  private List<MethodStructureDTO> methods;
}