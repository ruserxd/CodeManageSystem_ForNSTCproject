package com.example.codemangesystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "project")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "project_id")
    private Long project_id;

    @Column(name = "project_name")
    private String projectName;

    @OneToMany(
            mappedBy = "project",
            cascade = CascadeType.PERSIST,
            fetch = FetchType.EAGER
    )
    private List<Files> files;
}
