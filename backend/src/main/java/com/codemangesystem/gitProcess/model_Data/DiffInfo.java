package com.codemangesystem.gitProcess.model_Data;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Long diffInfoId;

    @Column(name = "author")
    private String author;

    @Column(name = "author_email")
    private String authorEmail;

    @Column(name = "commit_message")
    private String commitMessage;

    @Column(name = "timestamp")
    private long timestamp;

    @Column(name = "commit_time")
    private Date commitTime;

    @Column(name = "diff_code", columnDefinition = "TEXT")   // 將列類型為 TEXT
    private String diffCode;

    @JsonIgnore
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "method_id")
    private Method method;
}
