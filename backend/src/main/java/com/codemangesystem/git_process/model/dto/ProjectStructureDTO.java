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
public class ProjectStructureDTO {
  private Long projectId;
  private String projectName;
  private String headRevstr;
  private List<FileStructureDTO> files;
}