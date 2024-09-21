package com.example.codemangesystem.LoginProcess.services;

import com.example.codemangesystem.LoginProcess.model_user.MyUser;
import com.example.codemangesystem.LoginProcess.model_user.RegisterResponse;
import com.example.codemangesystem.LoginProcess.repository.MyUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MyUserService {

    private final MyUserRepository myUserRepository;

    @Autowired
    public MyUserService(MyUserRepository myUserRepository) {
        this.myUserRepository = myUserRepository;
    }

    // Add User
    public RegisterResponse UserRegister(MyUser myUser) {
        try {
            Optional<MyUser> testCurrentHave = myUserRepository.findByUserEmail(myUser.getUserEmail());
            if (testCurrentHave.isPresent()) {
                return RegisterResponse.builder()
                        .success(false)
                        .message("email is taken")
                        .build();
            }
            myUserRepository.save(myUser);
            return RegisterResponse.builder()
                    .success(true)
                    .message("success register")
                    .build();

        } catch (Exception e) {
            return RegisterResponse.builder()
                    .success(false)
                    .message("Failed " + e.getMessage())
                    .build();
        }
    }
}
