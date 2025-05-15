package com.codemangesystem.login_process.model.response;

import com.codemangesystem.login_process.model.user.MyUser;
import lombok.Builder;
import lombok.Data;

/**
 * 登入 api 的回傳資料
 */
@Builder
@Data
public class SessionResponse {

  /**
   * 是否登入登出成功
   */
  private boolean success;

  /**
   * 處理登入登出後的訊息
   */
  private String message;

  /**
   * 使用者資訊
   */
  private MyUser myUser;
}
