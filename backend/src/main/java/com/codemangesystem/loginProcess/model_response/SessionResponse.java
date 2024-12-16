package com.codemangesystem.loginProcess.model_response;

import com.codemangesystem.loginProcess.model_user.MyUser;
import lombok.Builder;
import lombok.Data;

/**
 * 登入 api 的回傳資料
 */
@Builder
@Data
public class SessionResponse {
    boolean success;
    String message;
    MyUser myUser;
}
