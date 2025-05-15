package com.codemangesystem.git_process.model.database;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DiffInfo 代表 method 的其中一個版本差異資訊
 */
@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "diff_info")
public class DiffInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "diff_info_id")
  private Long diffInfoId;

  @Column(name = "author")
  private String author;

  @Column(name = "author_email")
  private String authorEmail;

  @Column(name = "commit_message", columnDefinition = "TEXT")
  private String commitMessage;

  @Column(name = "commit_time")
  private Date commitTime;

  @Column(name = "diff_code", columnDefinition = "TEXT")   // 將列類型為 TEXT
  private String diffCode;

  @Column(name = "head_revstr")
  private String headRevstr;

  @JsonIgnore
  @JsonBackReference
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "method_id")
  private Method method;
}
