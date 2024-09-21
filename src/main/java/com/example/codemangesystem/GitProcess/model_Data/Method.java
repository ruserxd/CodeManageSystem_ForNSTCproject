package com.example.codemangesystem.GitProcess.model_Data;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "method")
public class Method {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "method_id")
    private Long method_id;

    @Column(name = "method_name")
    private String methodName;

    @JsonIgnore
    @JsonBackReference
    @ManyToOne(
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    @JoinColumn(name = "files_id")
    private Files files;

    @OneToMany(
            mappedBy = "method",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER
    )
    List<DiffInfo> diffInfoList;
}
