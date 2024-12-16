package com.codemangesystem.loginProcess.repository;

import com.codemangesystem.loginProcess.model_user.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 使用者相關的 Repository
 */
@Repository
public interface MyUserRepository extends JpaRepository<MyUser, Long> {
    Optional<MyUser> findByUserEmail(String userEmail);

    Optional<MyUser> findByUserAccount(String userAccount);

    Optional<MyUser> findByUserId(Long userId);
}
