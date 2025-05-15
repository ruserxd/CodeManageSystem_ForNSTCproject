package com.codemangesystem.git_process.model.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Project 代表著一整個 GitHub 下來的專案，有對應該 Repository 的相關資料 接著以一對多的方式對應 Files (專案的每個 .java 檔案)
 */
@Data
@ToString(exclude = {"files", "personalInfos"})
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "project")
public class Project {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "project_id")
  private Long projectId;

  @Column(name = "project_name")
  private String projectName;

  @Column(name = "head_revstr")
  private String headRevstr;

  // 為了簡化判斷的 analyze 預設此資料
  @Builder.Default
  @OneToMany(
      mappedBy = "project",
      cascade = CascadeType.ALL,
      fetch = FetchType.EAGER,
      orphanRemoval = true
  )
  private List<Files> files = new ArrayList<>();

  @JsonIgnore
  @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  private List<PersonalINFO> personalInfos;
}
