package com.codemangesystem.loginProcess.service;

import com.codemangesystem.loginProcess.model_response.LoginINFO;
import com.codemangesystem.loginProcess.model_response.sessionResponse;
import com.codemangesystem.loginProcess.model_user.MyUser;
import com.codemangesystem.loginProcess.model_user.UserAuthority;
import com.codemangesystem.loginProcess.repository.MyUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class UserServiceTest {
    private UserService userService;
    private PasswordBcrypt passwordBcrypt;
    private MyUserRepository myUserRepository;

    @BeforeEach
    void setUp() {
        myUserRepository = Mockito.mock(MyUserRepository.class);
        passwordBcrypt = Mockito.mock(PasswordBcrypt.class);
        userService = new UserService(myUserRepository, passwordBcrypt);
    }

    @Test
    @DisplayName("測試加入超級帳號")
    void addSuperAccountTest() {
        userService.addSuperAccount();
    }

    @Nested
    @DisplayName("測試 checkUser()")
    class checkUserTest {
        private LoginINFO userINFO;
        private MyUser myUser;

        @BeforeEach
        void setUp_checkUser() {
            this.userINFO = LoginINFO.builder()
                                     .userAccount("123")
                                     .userPassword("123")
                                     .build();
            this.myUser = MyUser.builder()
                                .userEmail("zz@gmail.com")
                                .userName("Admin")
                                .userAccount("123")
                                .userPassword("123")
                                .userAuthority(UserAuthority.ADMIN)
                                .build();
        }

        @Test
        @DisplayName("測試有該使用者但密碼錯誤")
        void passWordWrongTest() {
            // 設置錯誤的密碼
            userINFO.setUserPassword("111111111");

            Mockito.when(myUserRepository.findByUserAccount(userINFO.getUserAccount()))
                   .thenReturn(Optional.ofNullable(myUser));
            Mockito.when(passwordBcrypt.isPasswordSame(userINFO.getUserPassword(), myUser.getUserPassword()))
                   .thenReturn(false);

            sessionResponse result = userService.checkUser(userINFO);
            sessionResponse excepted = sessionResponse.builder()
                                                      .message("Email or Password Wrong")
                                                      .success(false)
                                                      .build();
            assertEquals(excepted, result);
        }

        @Test
        @DisplayName("測試沒有該使用者")
        void emailWrongTest() {
            // 設置錯誤的密碼

            Mockito.when(myUserRepository.findByUserAccount(userINFO.getUserAccount()))
                   .thenReturn(Optional.empty());

            sessionResponse result = userService.checkUser(userINFO);
            sessionResponse excepted = sessionResponse.builder()
                                                      .message("Email or Password Wrong")
                                                      .success(false)
                                                      .build();
            assertEquals(excepted, result);
        }

        @Test
        @DisplayName("測試有該使用者且密碼正確")
        void checkSuccessTest() {

            Mockito.when(myUserRepository.findByUserAccount(userINFO.getUserAccount()))
                   .thenReturn(Optional.ofNullable(myUser));
            Mockito.when(passwordBcrypt.isPasswordSame(userINFO.getUserPassword(), myUser.getUserPassword()))
                   .thenReturn(true);

            sessionResponse result = userService.checkUser(userINFO);
            sessionResponse excepted = sessionResponse.builder()
                                                      .message("Success")
                                                      .success(true)
                                                      .myUser(myUser)
                                                      .build();

            assertEquals(excepted, result);
        }
    }

    @Nested
    @DisplayName("測試 userRegister()")
    class userRegisterTest {
        private MyUser myUser;

        @BeforeEach
        void setUp_userRegister() {
            this.myUser = MyUser.builder()
                                .userEmail("zz@gmail.com")
                                .userName("Admin")
                                .userAccount("123")
                                .userPassword("123")
                                .userAuthority(UserAuthority.ADMIN)
                                .build();
        }

        @Test
        @DisplayName("測試 email 和 account 都存在")
        void accountAndEmailExistTest() {
            Mockito.when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
                   .thenReturn(Optional.ofNullable(myUser));
            Mockito.when( myUserRepository.findByUserEmail(myUser.getUserEmail()))
                    .thenReturn(Optional.ofNullable(myUser));

            sessionResponse result = userService.userRegister(myUser);
            sessionResponse except = sessionResponse.builder()
                                                    .success(false)
                                                    .message("Email, Account is taken")
                                                    .build();
            assertEquals(except, result);
        }

        @Test
        @DisplayName("測試只有 email 存在")
        void emailExistTest() {
            Mockito.when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
                   .thenReturn(Optional.empty());
            Mockito.when( myUserRepository.findByUserEmail(myUser.getUserEmail()))
                   .thenReturn(Optional.ofNullable(myUser));

            sessionResponse result = userService.userRegister(myUser);
            sessionResponse except = sessionResponse.builder()
                                                    .success(false)
                                                    .message("Email is taken")
                                                    .build();
            assertEquals(except, result);
        }

        @Test
        @DisplayName("測試只有 account 存在")
        void AccountExistTest() {
            Mockito.when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
                   .thenReturn(Optional.ofNullable(myUser));
            Mockito.when( myUserRepository.findByUserEmail(myUser.getUserEmail()))
                   .thenReturn(Optional.empty());

            sessionResponse result = userService.userRegister(myUser);
            sessionResponse except = sessionResponse.builder()
                                                    .success(false)
                                                    .message("Account is taken")
                                                    .build();
            assertEquals(except, result);
        }

        @Test
        @DisplayName("測試成功建立帳號")
        void successRegisterTest() {
            Mockito.when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
                   .thenReturn(Optional.empty());
            Mockito.when( myUserRepository.findByUserEmail(myUser.getUserEmail()))
                   .thenReturn(Optional.empty());

            sessionResponse result = userService.userRegister(myUser);
            sessionResponse except = sessionResponse.builder()
                                                    .success(true)
                                                    .message("Success register")
                                                    .build();
            assertEquals(except, result);
        }

        @Test
        @DisplayName("測試傳入 user 為 null")
        void muUserIsNullTest() {
            Mockito.when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
                   .thenReturn(Optional.empty());
            Mockito.when( myUserRepository.findByUserEmail(myUser.getUserEmail()))
                   .thenReturn(Optional.empty());

            myUser = null;
            sessionResponse result = userService.userRegister(myUser);
            sessionResponse except = sessionResponse.builder()
                                                    .success(false)
                                                    .message("User is null")
                                                    .build();
            assertEquals(except, result);
        }

        @Test
        @DisplayName("測試當發生 myUserRepository 回傳 null 拋出例外")
        void myUserRepositoryReturnNullTest() {
            Mockito.when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
                   .thenReturn(null);
            Mockito.when( myUserRepository.findByUserEmail(myUser.getUserEmail()))
                   .thenReturn(Optional.empty());

            sessionResponse result = userService.userRegister(myUser);
            sessionResponse except = sessionResponse.builder()
                                                    .success(false)
                                                    .message("Failed Cannot invoke \"java.util.Optional.isPresent()\" because \"myUserInDataBase\" is null")
                                                    .build();
            assertEquals(except, result);
        }
    }
}