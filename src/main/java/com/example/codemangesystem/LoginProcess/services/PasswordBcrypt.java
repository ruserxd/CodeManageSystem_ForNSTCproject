package com.example.codemangesystem.LoginProcess.services;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

//對資料進行 Bcrypt 的加密
@Service
public class PasswordBcrypt {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordBcrypt.class);

    // 對原始密碼加密並回傳
    public String encryptPassword(String plainText) {
        String encodedPassword = BCrypt.hashpw(plainText, BCrypt.gensalt());
        LOGGER.info("加密後的密碼" + encodedPassword);
        return encodedPassword;
    }

    // 確定輸入的密碼與資料庫的密碼是否相同

    public boolean decryptPasswordIsSameOrNot(String encodedPassword, String userInputPassword) {
        return BCrypt.checkpw(userInputPassword, encodedPassword);
    }
}
