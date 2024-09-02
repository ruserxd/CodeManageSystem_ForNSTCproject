package com.example.codemangesystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

// 用 lombok 減少程式碼的撰寫、增加可讀性
@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "files")
public class Files {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "files_id")
    private Long files_id;

    private String fileName;
    private String filePath;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @OneToMany(mappedBy = "files", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Method> methods;
}