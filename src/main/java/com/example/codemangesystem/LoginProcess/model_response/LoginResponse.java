package com.example.codemangesystem.LoginProcess.model_response;

import com.example.codemangesystem.LoginProcess.model_user.MyUser;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LoginResponse {
    boolean success;
    String message;
    MyUser myUser;
}
