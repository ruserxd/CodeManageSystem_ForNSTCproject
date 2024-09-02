package com.example.codemangesystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

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
    private Long diffInfo_id;

    private String author;
    private String authorEmail;
    private String commitMessage;
    private long timestamp;
    private Date commitTime;

    @Column(name = "diff_code", columnDefinition = "TEXT")   // 將列類型為 TEXT
    private String diffCode;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "method_id", referencedColumnName = "method_id")
    private Method method;
}
