package com.codemangesystem.loginProcess.service;

import com.codemangesystem.loginProcess.model_response.LoginINFO;
import com.codemangesystem.loginProcess.model_response.SessionResponse;
import com.codemangesystem.loginProcess.model_user.MyUser;
import com.codemangesystem.loginProcess.model_user.UserAuthority;
import com.codemangesystem.loginProcess.repository.MyUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
     * 用於登入，檢查使用者資訊
     * 加入 @Transactional(readOnly = true)
     * 避免 Lazy 的設定，導致讀取完後，JPA 自動關閉 session 的情況
     */
    @Transactional(readOnly = true)
    public SessionResponse checkUser(LoginINFO userINFO) {
        Optional<MyUser> myUser = myUserRepository.findByUserAccount(userINFO.getUserAccount());

        if (myUser.isPresent()) {
            log.info("Account have {}", myUser.get());

            // 密碼錯誤
            if (!passwordBcrypt.isPasswordSame(myUser.get().getUserPassword(), userINFO.getUserPassword())) {
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
            myUser.setUserAuthority(UserAuthority.OWNER);

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
    public boolean ifEmailExist(MyUser myUser) {
        Optional<MyUser> myUserInDataBase = myUserRepository.findByUserEmail(myUser.getUserEmail());
        return myUserInDataBase.isPresent();
    }

    /**
     * 判斷是否有重複的 account
     */
    public boolean ifAccountExist(MyUser myUser) {
        Optional<MyUser> myUserInDataBase = myUserRepository.findByUserAccount(myUser.getUserAccount());
        return myUserInDataBase.isPresent();
    }
}
