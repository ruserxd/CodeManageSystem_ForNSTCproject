package com.example.codemangesystem.LoginProcess.model_response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RegisterResponse {
    private boolean success;
    private String message;
}
