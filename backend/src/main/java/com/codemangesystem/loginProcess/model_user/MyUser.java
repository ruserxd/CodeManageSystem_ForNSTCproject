package com.codemangesystem.loginProcess.model_user;

import com.codemangesystem.gitProcess.model_Data.Files;
import com.codemangesystem.gitProcess.model_Data.Method;
import com.codemangesystem.gitProcess.model_Data.Project;
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
@Table(name = "user")
public class MyUser {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "user_account")
    private String userAccount;

    @Column(name = "user_password")
    private String userPassword;

    @Column(name = "user_authority")
    private UserAuthority userAuthority;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.PERSIST,
            fetch = FetchType.EAGER,
            orphanRemoval = true
    )
    private List<Project> projects;
}