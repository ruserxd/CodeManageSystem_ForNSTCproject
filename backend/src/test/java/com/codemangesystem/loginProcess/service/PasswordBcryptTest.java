package com.codemangesystem.loginProcess.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordBcryptTest {
    private PasswordBcrypt passwordBcrypt;

    @BeforeEach
    void setUp() {
        passwordBcrypt = new PasswordBcrypt();
    }

    /** 測試產出的 Hash 能夠驗證原始的密碼為正確 */
    @Test
    @DisplayName("測試 Bcrypt 加密和密碼驗證")
    void test() {
        String password = "password123@";
        String hashed = passwordBcrypt.encryptText(password);

        assertTrue(passwordBcrypt.isPasswordSame(hashed, password));
    }
}