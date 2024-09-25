package com.example.codemangesystem.LoginProcess.model_user;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LoginINFO {
    private String userEmail;
    private String userPassword;
}
