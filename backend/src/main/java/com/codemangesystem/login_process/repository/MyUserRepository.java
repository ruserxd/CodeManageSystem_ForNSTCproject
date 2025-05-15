package com.codemangesystem.login_process.repository;

import com.codemangesystem.login_process.model.user.MyUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 使用者相關的 Repository
 */
@Repository
public interface MyUserRepository extends JpaRepository<MyUser, Long> {

  Optional<MyUser> findByUserEmail(String userEmail);

  Optional<MyUser> findByUserAccount(String userAccount);

  Optional<MyUser> findByUserId(Long userId);

  @Transactional
  int deleteMyUserByUserId(Long userId);
}
