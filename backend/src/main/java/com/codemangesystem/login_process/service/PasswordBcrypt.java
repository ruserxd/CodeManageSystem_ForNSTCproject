package com.codemangesystem.login_process.service;

import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

/**
 * 對資料進行 Bcrypt 的加密
 */
@Slf4j
@Service
public class PasswordBcrypt {
    /**
     * 建構子
     */
    public PasswordBcrypt() {

    }

    /**
     * 對原始密碼加密並回傳
     */
    public String encryptText(String plainText) {
        String hashed = BCrypt.hashpw(plainText, BCrypt.gensalt());
        log.info("加密後的密碼 {}", hashed);
        return hashed;
    }

    /**
     * 確定輸入的密碼與資料庫的密碼是否相同
     */
    public boolean isPasswordSame(String hashed, String userInputPassword) {
        return BCrypt.checkpw(userInputPassword, hashed);
    }
}
