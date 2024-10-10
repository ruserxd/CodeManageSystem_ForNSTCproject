package com.example.codemangesystem.LoginProcess.services;

import com.example.codemangesystem.LoginProcess.model_response.LoginINFO;
import com.example.codemangesystem.LoginProcess.model_response.LoginResponse;
import com.example.codemangesystem.LoginProcess.model_user.MyUser;
import com.example.codemangesystem.LoginProcess.model_response.RegisterResponse;
import com.example.codemangesystem.LoginProcess.model_user.UserAuthority;
import com.example.codemangesystem.LoginProcess.repository.MyUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MyUserService {

    private static final Logger logger = LoggerFactory.getLogger(MyUserService.class);
    private final MyUserRepository myUserRepository;
    private final PasswordBcrypt passwordBcrypt;

    @Autowired
    public MyUserService(MyUserRepository myUserRepository, PasswordBcrypt passwordBcrypt) {
        this.myUserRepository = myUserRepository;
        this.passwordBcrypt = passwordBcrypt;
    }

    // 系統簡單加入超級帳號
    public void AddSuperAccount() {
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

    // 用於登入，檢查使用者資訊
    public LoginResponse checkUser(LoginINFO UserINFO) {
        Optional<MyUser> testCurrentHave = myUserRepository.findByUserAccount(UserINFO.getUserAccount());
        logger.info(UserINFO.getUserAccount());

        if (testCurrentHave.isPresent()) {
            logger.info("Account have " + testCurrentHave);

            if (!passwordBcrypt.decryptPasswordIsSameOrNot(testCurrentHave.get().getUserPassword(), UserINFO.getUserPassword())) {
                logger.info("Has this email but the password wrong");

                return LoginResponse.builder()
                        .message("Email or Password Wrong")
                        .success(false)
                        .build();
            }
        } else {
            logger.info("No this email");

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

    // Add User
    public RegisterResponse userRegister(MyUser myUser) {
        try {
            boolean emailExist = ifEmailExist(myUser);
            boolean accountExist = ifAccountExist(myUser);
            // 判斷 email, account 有沒有存在
            if (emailExist && accountExist) {
                logger.info("email, account is taken" + myUser.getUserEmail(), myUser.getUserAccount());

                return RegisterResponse.builder()
                        .success(false)
                        .message("email, account is taken")
                        .build();
            } else if (emailExist) {
                logger.info("email is taken" + myUser.getUserEmail());

                return RegisterResponse.builder()
                        .success(false)
                        .message("email is taken")
                        .build();
            } else if (accountExist) {
                logger.info("email is taken" + myUser.getUserAccount());

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
            logger.info("Register success");

            return RegisterResponse.builder()
                    .success(true)
                    .message("success register")
                    .build();
        } catch (Exception e) {
            logger.info("Register failed " + e.getMessage());

            return RegisterResponse.builder()
                    .success(false)
                    .message("Failed " + e.getMessage())
                    .build();
        }
    }

    // judge email
    public boolean ifEmailExist(MyUser myUser) {
        Optional<MyUser> testCurrentHave = myUserRepository.findByUserEmail(myUser.getUserEmail());
        return testCurrentHave.isPresent();
    }

    // judge account
    public boolean ifAccountExist(MyUser myUser) {
        Optional<MyUser> testCurrentHave = myUserRepository.findByUserAccount(myUser.getUserAccount());
        return testCurrentHave.isPresent();
    }
}
