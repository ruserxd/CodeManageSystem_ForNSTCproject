package com.codemangesystem.login_process.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codemangesystem.login_process.model.response.LoginINFO;
import com.codemangesystem.login_process.model.response.SessionResponse;
import com.codemangesystem.login_process.model.user.MyUser;
import com.codemangesystem.login_process.model.user.UserAuthority;
import com.codemangesystem.login_process.repository.MyUserRepository;
import com.codemangesystem.quota.QuotaService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Slf4j
class UserServiceTest {

  private UserService userService;
  private PasswordBcrypt passwordBcrypt;
  private MyUserRepository myUserRepository;
  private QuotaService quotaService;

  @BeforeEach
  void setUp() {
    myUserRepository = Mockito.mock(MyUserRepository.class);
    passwordBcrypt = Mockito.mock(PasswordBcrypt.class);
    quotaService = Mockito.mock(QuotaService.class);
    when(quotaService.tryAcquire(QuotaService.Kind.REGISTRATION)).thenReturn(true);
    userService = new UserService(myUserRepository, passwordBcrypt, quotaService, "admin", "s3cret", "", "");
  }

  @Nested
  @DisplayName("測試 addSuperAccount()")
  class addSuperAccountTest {

    @Test
    @DisplayName("依設定建立管理員，密碼經過雜湊")
    void addAccountTest() {
      when(myUserRepository.findByUserAccount("admin")).thenReturn(Optional.empty());
      when(passwordBcrypt.encryptText("s3cret")).thenReturn("hashed");

      userService.addSuperAccount();

      ArgumentCaptor<MyUser> saved = ArgumentCaptor.forClass(MyUser.class);
      verify(myUserRepository).save(saved.capture());
      assertEquals("admin", saved.getValue().getUserAccount());
      assertEquals("hashed", saved.getValue().getUserPassword());
      assertEquals(UserAuthority.ADMIN, saved.getValue().getUserAuthority());
    }

    @Test
    @DisplayName("已存在超級帳號時不重複建立")
    void isSuperAccountExistTest() {
      when(myUserRepository.findByUserAccount("admin"))
          .thenReturn(Optional.of(MyUser.builder().userAccount("admin").build()));

      userService.addSuperAccount();

      verify(myUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("未設定帳密時不建立任何管理員")
    void notConfiguredTest() {
      UserService unconfigured = new UserService(myUserRepository, passwordBcrypt, quotaService, "", "", "", "");

      unconfigured.addSuperAccount();

      verify(myUserRepository, never()).save(any());
      verify(myUserRepository, never()).findByUserAccount(anyString());
    }
  }


  @Nested
  @DisplayName("測試 addDemoAccount()")
  class addDemoAccountTest {

    private UserService demoService;

    @BeforeEach
    void setUp() {
      demoService = new UserService(myUserRepository, passwordBcrypt, quotaService, "", "", "demo", "demo-pw");
    }

    @Test
    @DisplayName("依設定建立 DEMO 帳號，密碼經過雜湊")
    void addAccountTest() {
      when(myUserRepository.findByUserAccount("demo")).thenReturn(Optional.empty());
      when(passwordBcrypt.encryptText("demo-pw")).thenReturn("hashed");

      demoService.addDemoAccount();

      ArgumentCaptor<MyUser> saved = ArgumentCaptor.forClass(MyUser.class);
      verify(myUserRepository, times(1)).save(saved.capture());
      assertEquals("demo", saved.getValue().getUserAccount());
      assertEquals("hashed", saved.getValue().getUserPassword());
      assertEquals(UserAuthority.DEMO, saved.getValue().getUserAuthority());
    }

    @Test
    @DisplayName("帳號已存在時不重複建立")
    void accountExistTest() {
      when(myUserRepository.findByUserAccount("demo"))
          .thenReturn(Optional.of(MyUser.builder().userAccount("demo").build()));

      demoService.addDemoAccount();

      verify(myUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("帳號留空時不建立")
    void blankAccountTest() {
      UserService noAccount =
          new UserService(myUserRepository, passwordBcrypt, quotaService, "", "", "", "demo-pw");

      noAccount.addDemoAccount();

      verify(myUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("密碼留空時不建立")
    void blankPasswordTest() {
      UserService noPassword =
          new UserService(myUserRepository, passwordBcrypt, quotaService, "", "", "demo", "");

      noPassword.addDemoAccount();

      verify(myUserRepository, never()).save(any());
    }
  }


  @Nested
  @DisplayName("測試 demoLogin()")
  class demoLoginTest {

    private UserService demoService;

    @BeforeEach
    void setUp() {
      demoService = new UserService(myUserRepository, passwordBcrypt, quotaService, "", "", "demo", "demo-pw");
    }

    private MyUser userWithAuthority(UserAuthority authority) {
      return MyUser.builder()
          .userId(7L)
          .userAccount("demo")
          .userPassword("hashed")
          .userAuthority(authority)
          .build();
    }

    @Test
    @DisplayName("DEMO 角色帳號成功，不驗密碼、不寫入")
    void successTest() {
      MyUser demo = userWithAuthority(UserAuthority.DEMO);
      when(myUserRepository.findByUserAccount("demo")).thenReturn(Optional.of(demo));

      SessionResponse response = demoService.demoLogin();

      assertTrue(response.isSuccess());
      assertEquals(demo, response.getMyUser());
      verify(passwordBcrypt, never()).isPasswordSame(anyString(), anyString());
      verify(myUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("帳號是 ADMIN 時拒絕")
    void adminRejectedTest() {
      when(myUserRepository.findByUserAccount("demo"))
          .thenReturn(Optional.of(userWithAuthority(UserAuthority.ADMIN)));

      SessionResponse response = demoService.demoLogin();

      assertFalse(response.isSuccess());
      assertNull(response.getToken());
      assertNull(response.getMyUser());
      assertFalse(response.getMessage().isBlank());
    }

    @Test
    @DisplayName("帳號是 USER 時拒絕")
    void userRejectedTest() {
      when(myUserRepository.findByUserAccount("demo"))
          .thenReturn(Optional.of(userWithAuthority(UserAuthority.USER)));

      SessionResponse response = demoService.demoLogin();

      assertFalse(response.isSuccess());
      assertNull(response.getToken());
      assertNull(response.getMyUser());
      assertFalse(response.getMessage().isBlank());
    }

    @Test
    @DisplayName("未設定 demo 帳號時失敗，且不查資料庫")
    void notConfiguredTest() {
      UserService unconfigured = new UserService(myUserRepository, passwordBcrypt, quotaService, "", "", "", "");

      SessionResponse response = unconfigured.demoLogin();

      assertFalse(response.isSuccess());
      assertNull(response.getToken());
      assertNull(response.getMyUser());
      assertFalse(response.getMessage().isBlank());
      verify(myUserRepository, never()).findByUserAccount(anyString());
    }

    @Test
    @DisplayName("資料庫查無 demo 帳號時失敗")
    void accountNotFoundTest() {
      when(myUserRepository.findByUserAccount("demo")).thenReturn(Optional.empty());

      SessionResponse response = demoService.demoLogin();

      assertFalse(response.isSuccess());
      assertNull(response.getToken());
      assertNull(response.getMyUser());
      assertFalse(response.getMessage().isBlank());
    }

    @Test
    @DisplayName("用真實 JwtService 簽出的 token，role 是 DEMO")
    void tokenRoleIsDemoTest() {
      MyUser demo = userWithAuthority(UserAuthority.DEMO);
      when(myUserRepository.findByUserAccount("demo")).thenReturn(Optional.of(demo));
      SecretKey key = new SecretKeySpec(
          "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
      JwtService jwtService = new JwtService(encoder, 8);

      SessionResponse response = demoService.demoLogin();
      String token = jwtService.generateToken(response.getMyUser());

      Jwt jwt = NimbusJwtDecoder.withSecretKey(key).build().decode(token);
      assertEquals("DEMO", jwt.getClaimAsString("role"));
      assertEquals("7", jwt.getSubject());
      assertEquals("demo", jwt.getClaimAsString("account"));
    }
  }


  @Nested
  @DisplayName("測試 checkUser() 空密碼")
  class checkUserEmptyPasswordTest {

    @Test
    @DisplayName("空密碼不能登入 demo 帳號（真實 PasswordBcrypt）")
    void emptyPasswordRejectedTest() {
      PasswordBcrypt realBcrypt = new PasswordBcrypt();
      MyUser demo = MyUser.builder()
          .userId(7L)
          .userAccount("demo")
          .userPassword(realBcrypt.encryptText("demo-pw"))
          .userAuthority(UserAuthority.DEMO)
          .build();
      when(myUserRepository.findByUserAccount("demo")).thenReturn(Optional.of(demo));
      UserService realService =
          new UserService(myUserRepository, realBcrypt, quotaService, "", "", "demo", "demo-pw");

      LoginINFO login = LoginINFO.builder().userAccount("demo").userPassword("").build();
      SessionResponse response = realService.checkUser(login);

      assertFalse(response.isSuccess());
      assertNull(response.getToken());
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
    @DisplayName("請求中夾帶 userId / userAuthority / personalInfos 時一律忽略，只建立新的 USER")
    void ignoreUnsafeFieldsTest() {
      // 攻擊者想用註冊 API 覆蓋 userId = 1 的既有使用者，並把自己設成 ADMIN
      myUser.setUserId(1L);
      myUser.setUserAuthority(UserAuthority.ADMIN);
      myUser.setPersonalInfos(new ArrayList<>());
      when(myUserRepository.findByUserAccount(myUser.getUserAccount())).thenReturn(Optional.empty());
      when(myUserRepository.findByUserEmail(myUser.getUserEmail())).thenReturn(Optional.empty());
      when(passwordBcrypt.encryptText("123")).thenReturn("hashed");

      userService.userRegister(myUser);

      ArgumentCaptor<MyUser> saved = ArgumentCaptor.forClass(MyUser.class);
      verify(myUserRepository).save(saved.capture());
      assertNull(saved.getValue().getUserId(), "userId 必須由資料庫產生，不可沿用請求中的值");
      assertEquals(UserAuthority.USER, saved.getValue().getUserAuthority());
      assertNull(saved.getValue().getPersonalInfos());
      assertEquals("123", saved.getValue().getUserAccount());
      assertEquals("zz@gmail.com", saved.getValue().getUserEmail());
      assertEquals("Admin", saved.getValue().getUserName());
      assertEquals("hashed", saved.getValue().getUserPassword());
    }

    @Test
    @DisplayName("今日註冊名額已滿：回失敗訊息、不建立 user")
    void quotaExhaustedTest() {
      when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
          .thenReturn(Optional.empty());
      when(myUserRepository.findByUserEmail(myUser.getUserEmail()))
          .thenReturn(Optional.empty());
      when(quotaService.tryAcquire(QuotaService.Kind.REGISTRATION)).thenReturn(false);

      SessionResponse result = userService.userRegister(myUser);

      assertFalse(result.isSuccess());
      assertEquals("今日註冊名額已滿，可使用訪客模式", result.getMessage());
      verify(myUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("額度未滿：成功註冊並只扣一次註冊額度")
    void quotaAcquiredOnSuccessTest() {
      when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
          .thenReturn(Optional.empty());
      when(myUserRepository.findByUserEmail(myUser.getUserEmail()))
          .thenReturn(Optional.empty());

      SessionResponse result = userService.userRegister(myUser);

      assertTrue(result.isSuccess());
      verify(quotaService, times(1)).tryAcquire(QuotaService.Kind.REGISTRATION);
      verify(myUserRepository).save(any());
    }

    @Test
    @DisplayName("帳號或 email 重複、user 為 null 時不消耗額度")
    void validationFailureDoesNotConsumeQuotaTest() {
      when(myUserRepository.findByUserAccount(myUser.getUserAccount()))
          .thenReturn(Optional.of(myUser));
      when(myUserRepository.findByUserEmail(myUser.getUserEmail()))
          .thenReturn(Optional.empty());

      userService.userRegister(myUser);
      userService.userRegister(null);

      verify(quotaService, never()).tryAcquire(any());
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