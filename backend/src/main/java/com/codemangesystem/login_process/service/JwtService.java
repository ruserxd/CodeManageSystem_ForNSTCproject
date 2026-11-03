package com.codemangesystem.login_process.service;

import com.codemangesystem.login_process.model.user.MyUser;
import com.codemangesystem.login_process.model.user.UserAuthority;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * 負責發放登入後的 JWT
 */
@Service
public class JwtService {

  private final JwtEncoder jwtEncoder;
  private final Duration expiration;

  public JwtService(JwtEncoder jwtEncoder,
      @Value("${app.jwt.expiration-hours:8}") long expirationHours) {
    this.jwtEncoder = jwtEncoder;
    this.expiration = Duration.ofHours(expirationHours);
  }

  /**
   * 產生 token，subject 為 userId，role 為使用者權限
   */
  public String generateToken(MyUser user) {
    Instant now = Instant.now();
    UserAuthority authority = Objects.requireNonNullElse(user.getUserAuthority(),
        UserAuthority.USER);

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("CodeMangeSystem")
        .issuedAt(now)
        .expiresAt(now.plus(expiration))
        .subject(String.valueOf(user.getUserId()))
        .claim("account", user.getUserAccount())
        .claim("role", authority.name())
        .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
