package com.codemangesystem.config;

import com.codemangesystem.gitProcess.repository.ProjectRepository;
import com.codemangesystem.loginProcess.model_user.MyUser;
import com.codemangesystem.loginProcess.model_user.UserAuthority;
import com.codemangesystem.loginProcess.repository.MyUserRepository;
import com.codemangesystem.loginProcess.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class MyPostConstructTest {
    private ProjectRepository projectRepository;
    private UserService userService;
    private MyUserRepository myUserRepository;
    private MyPostConstruct myPostConstruct;

    @BeforeEach
    void initial() {
        userService = Mockito.mock(UserService.class);
        projectRepository = Mockito.mock(ProjectRepository.class);
        myUserRepository = Mockito.mock(MyUserRepository.class);
        myPostConstruct = new MyPostConstruct(projectRepository, userService, myUserRepository);
    }

    @Test
    @DisplayName("測試是否正常刪除資料夾")
    void deleteCloneFileTest() {
        try {
            myPostConstruct.deleteCloneFile();
        } catch (IOException e) {
            log.error("測試 deleteCloneFileTest() 發生 {}", e.getMessage());
        }

        File directory = new File(MyPostConstruct.path);
        assertTrue(directory.exists(), "發生錯誤，此路徑應該存在一個資料夾");
        assertTrue(directory.isDirectory(), "發生錯誤，此路徑應該為一個資料夾");

        // 目錄下會存在一個 .gitKeep 因此預設為 1
        assertTrue(directory.list().length == 1, "發生錯誤，此資料夾不該存在文件");
    }

    @Test
    @DisplayName("測試 deleteProjectDatBase()")
    void deleteProjectDatBaseTest() {
        myPostConstruct.deleteProjectDatBase();
    }

    @Test
    @DisplayName("測試當沒有超級帳號的處理")
    void doNotAddSuperAccountTest() {
        Mockito.when(myUserRepository.findByUserAccount("123")).thenReturn(Optional.empty());
        myPostConstruct.addSuperAccount();
        Mockito.verify(userService).addSuperAccount();
    }

    @Test
    @DisplayName("測試當有超級帳號的處理")
    void addSuperAccountTest() {
        MyUser myUser = MyUser.builder()
                              .userEmail("zz@gmail.com")
                              .userName("Admin")
                              .userAccount("123")
                              .userPassword("123")
                              .userAuthority(UserAuthority.ADMIN)
                              .build();
        Mockito.when(myUserRepository.findByUserAccount("123")).thenReturn(Optional.ofNullable(myUser));
        myPostConstruct.addSuperAccount();
        Mockito.verify(userService, Mockito.never()).addSuperAccount();
    }

    @Test
    @DisplayName("測試 initialSetting()")
    void initialSettingTest() throws IllegalAccessException {
        myPostConstruct.initialSettings();
    }

    @Test
    @DisplayName("測試 initialSetting() 發生 {路徑錯誤}")
    void initialSettingThrowTest() {
        MyPostConstruct.path = "/errorPath";
        IllegalAccessException exception = assertThrows(IllegalAccessException.class, () -> {
            myPostConstruct.initialSettings();
        });
        assertEquals(exception.getMessage(), "路徑錯誤");
        MyPostConstruct.path = "src/cloneCode";
    }
}