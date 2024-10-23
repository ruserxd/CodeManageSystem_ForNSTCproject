package com.codemangesystem.loginProcess.model_response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RegisterResponse {
    private boolean success;
    private String message;
}
