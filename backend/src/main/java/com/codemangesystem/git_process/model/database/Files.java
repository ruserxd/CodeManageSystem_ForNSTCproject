package com.codemangesystem.git_process.model.database;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Files 代表著整個專案的各別 .java 檔案
 * 接著以一對多的方式對應 Method (File 的每個 method)
 */
@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"methods"})
@Table(name = "files")
public class Files {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "files_id")
    private Long filesId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @JsonIgnore
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "project_id")
    private Project project;

    @OneToMany(
            mappedBy = "files",
            cascade = CascadeType.PERSIST,
            fetch = FetchType.EAGER,
            orphanRemoval = true
    )
    private List<Method> methods;
}