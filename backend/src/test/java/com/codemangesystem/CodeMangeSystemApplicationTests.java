package com.codemangesystem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class CodeMangeSystemApplicationTests {

    @Test
    @DisplayName("正式執行 springboot")
    void contextLoads() {
        assertDoesNotThrow(() -> {
            CodeMangeSystemApplication.main(new String[]{});
        });
    }
}
