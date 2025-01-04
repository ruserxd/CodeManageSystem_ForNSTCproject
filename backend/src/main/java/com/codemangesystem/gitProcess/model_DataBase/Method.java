package com.codemangesystem.gitProcess.model_DataBase;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Method 代表 file 的其中一個方法
 * 接著以一對多的方式對應 DiffINFO (method 每個版本的差異)
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
    List<DiffInfo> diffInfoList;
}
