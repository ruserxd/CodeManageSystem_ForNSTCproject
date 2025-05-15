package com.codemangesystem.login_process.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.codemangesystem.login_process.model.response.LoginINFO;
import com.codemangesystem.login_process.model.response.SessionResponse;
import com.codemangesystem.login_process.model.user.MyUser;
import com.codemangesystem.login_process.model.user.UserAuthority;
import com.codemangesystem.login_process.repository.MyUserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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

  @Nested
  @DisplayName("測試 addSuperAccount()")
  class addSuperAccountTest {

    @Test
    @DisplayName("測試加入超級帳號")
    void addAccountTest() {
      userService.addSuperAccount();
    }

    @Test
    @DisplayName("已存在超級帳號")
    void isSuperAccountExistTest() {
      MyUser superAccount = MyUser.builder()
          .userEmail("zz@gmail.com")
          .userName("Admin")
          .userAccount("123")
          .userPassword("123")
          .userAuthority(UserAuthority.ADMIN)
          .build();
      when(myUserRepository.findByUserAccount("123"))
          .thenReturn(Optional.ofNullable(superAccount));

      userService.addSuperAccount();
    }
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

      when(myUserRepository.findByUserAccount(userINFO.getUserAccount()))
          .thenReturn(Optional.ofNullable(myUser));
      when(passwordBcrypt.isPasswordSame(userINFO.getUserPassword(), myUser.getUserPassword()))
          .thenReturn(false);

      SessionResponse result = userService.checkUser(userINFO);
      SessionResponse excepted = SessionResponse.builder()
          .message("Email or Password Wrong")
          .success(false)
          .build();
      assertEquals(excepted, result);
    }

    @Test
    @DisplayName("測試沒有該使用者")
    void emailWrongTest() {
      // 設置錯誤的密碼

      when(myUserRepository.findByUserAccount(userINFO.getUserAccount()))
          .thenReturn(Optional.empty());

      SessionResponse result = userService.checkUser(userINFO);
      SessionResponse excepted = SessionResponse.builder()
          .message("Email or Password Wrong")
          .success(false)
          .build();
      assertEquals(excepted, result);
    }

    @Test
    @DisplayName("測試有該使用者且密碼正確")
    void checkSuccessTest() {

      when(myUserRepository.findByUserAccount(userINFO.getUserAccount()))
          .thenReturn(Optional.ofNullable(myUser));
      when(passwordBcrypt.isPasswordSame(userINFO.getUserPassword(), myUser.getUserPassword()))
          .thenReturn(true);

      SessionResponse result = userService.checkUser(userINFO);
      SessionResponse excepted = SessionResponse.builder()
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
      when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
          .thenReturn(Optional.ofNullable(myUser));
      when(myUserRepository.findByUserEmail(myUser.getUserEmail()))
          .thenReturn(Optional.ofNullable(myUser));

      SessionResponse result = userService.userRegister(myUser);
      SessionResponse except = SessionResponse.builder()
          .success(false)
          .message("Email, Account is taken")
          .build();
      assertEquals(except, result);
    }

    @Test
    @DisplayName("測試只有 email 存在")
    void emailExistTest() {
      when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
          .thenReturn(Optional.empty());
      when(myUserRepository.findByUserEmail(myUser.getUserEmail()))
          .thenReturn(Optional.ofNullable(myUser));

      SessionResponse result = userService.userRegister(myUser);
      SessionResponse except = SessionResponse.builder()
          .success(false)
          .message("Email is taken")
          .build();
      assertEquals(except, result);
    }

    @Test
    @DisplayName("測試只有 account 存在")
    void AccountExistTest() {
      when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
          .thenReturn(Optional.ofNullable(myUser));
      when(myUserRepository.findByUserEmail(myUser.getUserEmail()))
          .thenReturn(Optional.empty());

      SessionResponse result = userService.userRegister(myUser);
      SessionResponse except = SessionResponse.builder()
          .success(false)
          .message("Account is taken")
          .build();
      assertEquals(except, result);
    }

    @Test
    @DisplayName("測試成功建立帳號")
    void successRegisterTest() {
      when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
          .thenReturn(Optional.empty());
      when(myUserRepository.findByUserEmail(myUser.getUserEmail()))
          .thenReturn(Optional.empty());

      SessionResponse result = userService.userRegister(myUser);
      SessionResponse except = SessionResponse.builder()
          .success(true)
          .message("Success register")
          .build();
      assertEquals(except, result);
    }

    @Test
    @DisplayName("測試傳入 user 為 null")
    void muUserIsNullTest() {
      when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
          .thenReturn(Optional.empty());
      when(myUserRepository.findByUserEmail(myUser.getUserEmail()))
          .thenReturn(Optional.empty());

      myUser = null;
      SessionResponse result = userService.userRegister(myUser);
      SessionResponse except = SessionResponse.builder()
          .success(false)
          .message("User is null")
          .build();
      assertEquals(except, result);
    }

    @Test
    @DisplayName("測試當發生 myUserRepository 回傳 null 拋出例外")
    void myUserRepositoryReturnNullTest() {
      when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
          .thenReturn(null);
      when(myUserRepository.findByUserEmail(myUser.getUserEmail()))
          .thenReturn(Optional.empty());

      SessionResponse result = userService.userRegister(myUser);
      SessionResponse except = SessionResponse.builder()
          .success(false)
          .message(
              "Failed Cannot invoke \"java.util.Optional.isPresent()\" because \"myUserInDataBase\" is null")
          .build();
      assertEquals(except, result);
    }
  }

  @Nested
  @DisplayName("測試 getIdsAccounts()")
  class getIdsAccountsTest {

    List<MyUser> excepted = new ArrayList<>();

    @Test
    public void getAllUserTest() {
      // 模擬設定
      when(myUserRepository.findAll())
          .thenReturn(excepted);

      // 資料庫獲得內容
      excepted.add(MyUser.builder()
          .userId(0L)
          .userAccount("test1@gmail.com")
          .userAuthority(UserAuthority.USER)
          .userName("tester1")
          .build());
      excepted.add(MyUser.builder()
          .userId(1L)
          .userAccount("test2@gmail.com")
          .userAuthority(UserAuthority.ADMIN)
          .userName("tester2")
          .build());
      // 測試
      assertEquals(userService.getIdsAccounts(), excepted);
    }
  }

  @Nested
  @DisplayName("測試 deleteUserById()")
  class deleteUserByIdTest {

    List<MyUser> excepted = new ArrayList<>();

    @Test
    @DisplayName("測試是否從資料庫刪除(成功) user by userID")
    void deleteSuccess() {
      // 模擬設定
      when(myUserRepository.deleteMyUserByUserId(0L))
          .thenReturn(1);

      // 測試
      assertTrue(userService.deleteUserById(0L));
    }

    @Test
    @DisplayName("測試從資料庫刪除(失敗) user by userID")
    void deleteFailed() {
      // 模擬設定
      when(myUserRepository.deleteMyUserByUserId(0L))
          .thenReturn(0);

      // 測試
      assertFalse(userService.deleteUserById(0L));
    }
  }
}