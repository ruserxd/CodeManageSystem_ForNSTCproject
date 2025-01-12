package com.codemangesystem.springboot_runner;

import com.codemangesystem.CodeMangeSystemApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/*若此 測試遇上問題請注意本地端的 .env 檔案與 plugin 的設置*/
@SpringBootTest
@TestPropertySource(properties="")
class CodeMangeSystemApplicationTests {

    @Test
    @DisplayName("正式執行 springboot")
    void contextLoads() {
        assertDoesNotThrow(() -> {
            CodeMangeSystemApplication.main(new String[]{});
        });
    }
}
