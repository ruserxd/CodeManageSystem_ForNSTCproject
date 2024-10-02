package com.example.codemangesystem.LoginProcess.model_response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LoginINFO {
    private String userAccount;
    private String userPassword;
}
