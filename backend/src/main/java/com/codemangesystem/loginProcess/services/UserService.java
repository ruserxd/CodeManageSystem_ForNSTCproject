package com.codemangesystem.loginProcess.services;

import com.codemangesystem.loginProcess.model_response.LoginINFO;
import com.codemangesystem.loginProcess.model_response.LoginResponse;
import com.codemangesystem.loginProcess.model_response.RegisterResponse;
import com.codemangesystem.loginProcess.model_user.MyUser;
import com.codemangesystem.loginProcess.model_user.UserAuthority;
import com.codemangesystem.loginProcess.repository.MyUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class MyUserService {

    private final MyUserRepository myUserRepository;
    private final PasswordBcrypt passwordBcrypt;

    @Autowired
    public MyUserService(MyUserRepository myUserRepository, PasswordBcrypt passwordBcrypt) {
        this.myUserRepository = myUserRepository;
        this.passwordBcrypt = passwordBcrypt;
    }

    /**
     * 系統加入超級帳號
     */
    public void addSuperAccount() {
        // 確定目前資料庫內沒有超級帳號
        Optional<MyUser> testCurrentHave = myUserRepository.findByUserAccount("123");
        if (testCurrentHave.isPresent())
            return;

        MyUser myUser = MyUser.builder()
                .userEmail("zz@gmail.com")
                .userName("Admin")
                .userAccount("123")
                .userPassword("123")
                .userAuthority(UserAuthority.ADMIN)
                .build();
        myUser.setUserPassword(passwordBcrypt.encryptPassword(myUser.getUserPassword()));

        myUserRepository.save(myUser);
    }

    /**
     * 用於登入，檢查使用者資訊
     */
    public LoginResponse checkUser(LoginINFO userINFO) {
        Optional<MyUser> testCurrentHave = myUserRepository.findByUserAccount(userINFO.getUserAccount());
        log.info(userINFO.getUserAccount());

        if (testCurrentHave.isPresent()) {
            log.info("Account have " + testCurrentHave);

            if (!passwordBcrypt.decryptPasswordIsSameOrNot(testCurrentHave.get().getUserPassword(), userINFO.getUserPassword())) {
                log.info("Has this email but the password wrong");

                return LoginResponse.builder()
                        .message("Email or Password Wrong")
                        .success(false)
                        .build();
            }
        } else {
            log.info("No this email");

            return LoginResponse.builder()
                    .message("Email or Password Wrong")
                    .success(false)
                    .build();
        }

        return LoginResponse.builder()
                .message("Success")
                .success(true)
                .myUser(testCurrentHave.get())
                .build();
    }

    /**
     * 新增 User
     */
    public RegisterResponse userRegister(MyUser myUser) {
        try {
            boolean emailExist = ifEmailExist(myUser);
            boolean accountExist = ifAccountExist(myUser);
            // 判斷 email, account 有沒有存在
            if (emailExist && accountExist) {
                log.info("email, account is taken" + myUser.getUserEmail(), myUser.getUserAccount());

                return RegisterResponse.builder()
                        .success(false)
                        .message("email, account is taken")
                        .build();
            } else if (emailExist) {
                log.info("email is taken" + myUser.getUserEmail());

                return RegisterResponse.builder()
                        .success(false)
                        .message("email is taken")
                        .build();
            } else if (accountExist) {
                log.info("email is taken" + myUser.getUserAccount());

                return RegisterResponse.builder()
                        .success(false)
                        .message("account is taken")
                        .build();
            }

            // 都沒問題開始加入帳號進入資料庫
            // 為密碼加密處理 bcrypt
            myUser.setUserPassword(passwordBcrypt.encryptPassword(myUser.getUserPassword()));
            // 設定權限
            myUser.setUserAuthority(UserAuthority.OWNER);

            myUserRepository.save(myUser);
            log.info("Register success");

            return RegisterResponse.builder()
                    .success(true)
                    .message("success register")
                    .build();
        } catch (Exception e) {
            log.info("Register failed " + e.getMessage());

            return RegisterResponse.builder()
                    .success(false)
                    .message("Failed " + e.getMessage())
                    .build();
        }
    }

    /**
     * 判斷是否有重複的 email
     */
    public boolean ifEmailExist(MyUser myUser) {
        Optional<MyUser> testCurrentHave = myUserRepository.findByUserEmail(myUser.getUserEmail());
        return testCurrentHave.isPresent();
    }

    /**
     * 判斷是否有重複的 account
     */
    public boolean ifAccountExist(MyUser myUser) {
        Optional<MyUser> testCurrentHave = myUserRepository.findByUserAccount(myUser.getUserAccount());
        return testCurrentHave.isPresent();
    }
}
