package com.codemangesystem.git_process.model.database;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Method 代表 file 的其中一個方法 接著以一對多的方式對應 DiffINFO (method 每個版本的差異)
 */
@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"diffInfoList"})
@Table(name = "method")
public class Method {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "method_id")
  private Long methodId;

  @Column(name = "method_name")
  private String methodName;

  @JsonIgnore
  @JsonBackReference
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "files_id")
  private Files files;

  @OneToMany(
      mappedBy = "method",
      cascade = CascadeType.ALL,
      fetch = FetchType.EAGER,
      orphanRemoval = true
  )
  private List<DiffInfo> diffInfoList;
}
