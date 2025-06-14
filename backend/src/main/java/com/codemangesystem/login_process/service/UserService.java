package com.codemangesystem.login_process.service;

import com.codemangesystem.login_process.model.response.LoginINFO;
import com.codemangesystem.login_process.model.response.SessionResponse;
import com.codemangesystem.login_process.model.user.MyUser;
import com.codemangesystem.login_process.model.user.UserAuthority;
import com.codemangesystem.login_process.repository.MyUserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 處理有關 User 的操作
 */
@Slf4j
@Service
public class UserService {

  private final MyUserRepository myUserRepository;
  private final PasswordBcrypt passwordBcrypt;

  public UserService(MyUserRepository myUserRepository, PasswordBcrypt passwordBcrypt) {
    this.myUserRepository = myUserRepository;
    this.passwordBcrypt = passwordBcrypt;
  }

  /**
   * 系統加入超級帳號
   */
  @Transactional
  public void addSuperAccount() {
    // 確定目前資料庫內沒有超級帳號
    Optional<MyUser> myUserInDataBase = myUserRepository.findByUserAccount("123");
    if (myUserInDataBase.isPresent()) {
      log.info("已存在超級帳號 {}", myUserInDataBase);
      return;
    }

    MyUser myUser = MyUser.builder()
        .userEmail("zz@gmail.com")
        .userName("Admin")
        .userAccount("123")
        .userPassword("123")
        .userAuthority(UserAuthority.ADMIN)
        .build();
    myUser.setUserPassword(passwordBcrypt.encryptText(myUser.getUserPassword()));

    myUserRepository.save(myUser);
    log.info("成功加入超級帳號");
  }

  /**
   * 用於登入，檢查使用者資訊 加入 @Transactional(readOnly = true) 避免 Lazy 的設定，導致讀取完後，JPA 自動關閉 session 的情況
   */
  @Transactional(readOnly = true)
  public SessionResponse checkUser(LoginINFO userINFO) {
    Optional<MyUser> myUser = myUserRepository.findByUserAccount(userINFO.getUserAccount());

    if (myUser.isPresent()) {
      log.info("Account have {}", myUser.get());

      // 密碼錯誤
      if (!passwordBcrypt.isPasswordSame(myUser.get().getUserPassword(),
          userINFO.getUserPassword())) {
        log.info("Has this email but the password wrong");

        return SessionResponse.builder()
            .message("Email or Password Wrong")
            .success(false)
            .build();
      }
    } else {
      log.info("No this email");

      return SessionResponse.builder()
          .message("Email or Password Wrong")
          .success(false)
          .build();
    }

    log.info("Check Success");
    return SessionResponse.builder()
        .message("Success")
        .success(true)
        .myUser(myUser.get())
        .build();
  }

  /**
   * 新增 User
   */
  @Transactional
  public SessionResponse userRegister(MyUser myUser) {
    if (myUser == null) {
      log.info("傳入資料為 null");
      return SessionResponse.builder()
          .success(false)
          .message("User is null")
          .build();
    }

    try {
      boolean emailExist = ifEmailExist(myUser);
      boolean accountExist = ifAccountExist(myUser);
      // 判斷 email, account 有沒有存在
      if (emailExist && accountExist) {
        log.info("Email, Account is taken {} {}", myUser.getUserEmail(), myUser.getUserAccount());

        return SessionResponse.builder()
            .success(false)
            .message("Email, Account is taken")
            .build();
      } else if (emailExist) {
        log.info("Email is taken {}", myUser.getUserEmail());

        return SessionResponse.builder()
            .success(false)
            .message("Email is taken")
            .build();
      } else if (accountExist) {
        log.info("Account is taken {}", myUser.getUserAccount());

        return SessionResponse.builder()
            .success(false)
            .message("Account is taken")
            .build();
      }

      // 都沒問題開始加入帳號進入資料庫
      // 為密碼加密處理 bcrypt
      myUser.setUserPassword(passwordBcrypt.encryptText(myUser.getUserPassword()));
      // 設定權限
      myUser.setUserAuthority(UserAuthority.USER);

      myUserRepository.save(myUser);
      log.info("Register success");

      return SessionResponse.builder()
          .success(true)
          .message("Success register")
          .build();
    } catch (Exception e) {
      log.info("Register failed {}", e.getMessage());

      return SessionResponse.builder()
          .success(false)
          .message("Failed " + e.getMessage())
          .build();
    }
  }

  /**
   * 判斷是否有重複的 email
   */
  @Transactional(readOnly = true)
  public boolean ifEmailExist(MyUser myUser) {
    Optional<MyUser> myUserInDataBase = myUserRepository.findByUserEmail(myUser.getUserEmail());
    return myUserInDataBase.isPresent();
  }

  /**
   * 判斷是否有重複的 account
   */
  @Transactional(readOnly = true)
  public boolean ifAccountExist(MyUser myUser) {
    Optional<MyUser> myUserInDataBase = myUserRepository.findByUserAccount(myUser.getUserAccount());
    return myUserInDataBase.isPresent();
  }

  /**
   * 管理使用者
   */
  @Transactional(readOnly = true)
  public List<MyUser> getIdsAccounts() {
    List<MyUser> users = myUserRepository.findAll();
    List<MyUser> result = new ArrayList<>();
    for (MyUser user : users) {
      result.add(new MyUser(
          user.getUserId(),
          user.getUserName(),
          user.getUserEmail(),
          user.getUserAccount(),
          user.getUserPassword(),
          user.getUserAuthority(),
          null
      ));
    }

    return result;
  }

  @Transactional
  public boolean deleteUserById(Long userId) {
    return myUserRepository.deleteMyUserByUserId(userId) == 1;
  }
}
