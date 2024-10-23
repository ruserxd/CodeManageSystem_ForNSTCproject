package com.codemangesystem.loginProcess.model_response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LoginINFO {
    private String userAccount;
    private String userPassword;
}
