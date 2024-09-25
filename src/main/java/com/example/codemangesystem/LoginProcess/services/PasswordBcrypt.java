package com.example.codemangesystem.LoginProcess.services;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PasswordBcrypt {

    private static final Logger logger = LoggerFactory.getLogger(PasswordBcrypt.class);
    public String encryptPassword(String plainText) {
        String encodedPassword = BCrypt.hashpw(plainText, BCrypt.gensalt());
        logger.info("加密後的密碼" + encodedPassword);
        return encodedPassword;
    }

    public boolean decryptPasswordIsSameOrNot(String encodedPassword, String UserInputPassword) {
        return BCrypt.checkpw(UserInputPassword, encodedPassword);
    }
}
