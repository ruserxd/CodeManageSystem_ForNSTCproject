package com.example.codemangesystem.LoginProcess.repository;

import com.example.codemangesystem.LoginProcess.model_user.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MyUserRepository extends JpaRepository<MyUser, Long> {
    Optional<MyUser> findByUserEmail(String UserEmail);
    Optional<MyUser> findByUserAccount(String UserAccount);
}
