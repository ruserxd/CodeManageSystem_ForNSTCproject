package com.codemangesystem.loginProcess.model_response;

import lombok.Builder;
import lombok.Data;

/**
 * 與登入相關的資料
 */
@Builder
@Data
public class LoginINFO {
    private String userAccount;
    private String userPassword;
}
