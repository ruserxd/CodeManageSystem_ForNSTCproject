package com.codemangesystem.git_process.model_database;

import com.codemangesystem.login_process.model_user.MyUser;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PersonalINFO 代表著 MyUser 與 Project 間的關係
 * MyUser 有著哪些 Project 以及是甚麼的版本
 */
@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "personal_info")
public class PersonalINFO {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long personal_info_id;

    @JsonIgnore
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private MyUser user;

    @JsonIgnore
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "project_id")
    private Project project;

    @Column(name = "head_revstr")
    private String headRevstr;
}
