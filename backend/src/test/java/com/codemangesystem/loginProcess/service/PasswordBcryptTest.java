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

    @Test
    @DisplayName("測試 Bcrypt 加密和密碼驗證")
    void test() {
        String password = "password123@";
        String encryptPassword = passwordBcrypt.encryptText(password);

        assertTrue(passwordBcrypt.isPasswordSame(encryptPassword, password));
    }
}