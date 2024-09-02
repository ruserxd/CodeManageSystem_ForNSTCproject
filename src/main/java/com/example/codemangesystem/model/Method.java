package com.example.codemangesystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

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

    private String methodName;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "files_id", referencedColumnName = "files_id")
    private Files files;

    @OneToMany(mappedBy = "method", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    List<DiffInfo> diffInfoList;
}
