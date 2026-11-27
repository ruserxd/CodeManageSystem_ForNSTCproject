// 首頁範例：本專案 repo 中 UserService.checkUser()（早期在 MyUserService）的真實修改紀錄。
// 從 21 次修改中節錄 4 次，diff 內容取自 git log -L / git show，只保留與該方法相關的區塊；
// 第一筆較長，省略了幾行 log 與結尾（以 ... 表示）。
export const SAMPLE_METHOD = {
	file: 'UserService.java',
	signature: 'checkUser(LoginINFO userINFO)',
	totalCommits: 21
};

export const SAMPLE_HISTORY = [
	{
		sha: '8d3de4a',
		date: '2024-09-25',
		summary: '建立登入功能',
		note: '用 Email 找使用者，兩種失敗回傳不同訊息。',
		diff: `+    public LoginResponse checkUser(LoginINFO UserINFO) {
+        Optional<MyUser> testCurrentHave = myUserRepository.findByUserEmail(UserINFO.getUserEmail());
+        logger.info(UserINFO.getUserEmail());
+
+        if (testCurrentHave.isPresent()) {
+            logger.info("email have " + testCurrentHave);
+
+            if (!passwordBcrypt.decryptPasswordIsSameOrNot(testCurrentHave.get().getUserPassword(), UserINFO.getUserPassword())) {
+                return LoginResponse.builder()
+                        .message("Password Wrong")
+                        .success(false)
+                        .build();
+            }
+        } else {
+            return LoginResponse.builder()
+                    .message("No this email")
+                    .success(false)
+                    .build();
+        }
+        ...
+    }`
	},
	{
		sha: '17ac658',
		date: '2024-10-02',
		summary: '改用帳號登入',
		note: '查詢條件從 Email 換成帳號。',
		diff: `     public LoginResponse checkUser(LoginINFO UserINFO) {
-        Optional<MyUser> testCurrentHave = myUserRepository.findByUserEmail(UserINFO.getUserEmail());
-        logger.info(UserINFO.getUserEmail());
+        Optional<MyUser> testCurrentHave = myUserRepository.findByUserAccount(UserINFO.getUserAccount());
+        logger.info(UserINFO.getUserAccount());

         if (testCurrentHave.isPresent()) {
-            logger.info("email have " + testCurrentHave);
+            logger.info("Account have " + testCurrentHave);`
	},
	{
		sha: '076732f',
		date: '2024-10-10',
		summary: '統一登入失敗訊息',
		note: '不再透露帳號是否存在。',
		diff: `                 return LoginResponse.builder()
-                        .message("Password Wrong")
+                        .message("Email or Password Wrong")
                         .success(false)
                         .build();
             }
         } else {
             logger.info("No this email");

             return LoginResponse.builder()
-                    .message("No this email")
+                    .message("Email or Password Wrong")
                     .success(false)
                     .build();`
	},
	{
		sha: '8fde934',
		date: '2026-11-14',
		summary: 'log 不再印出整個使用者物件',
		note: 'log 只記錄 userId，不再印出密碼 hash。',
		diff: `   public SessionResponse checkUser(LoginINFO userINFO) {
     Optional<MyUser> myUser = myUserRepository.findByUserAccount(userINFO.getUserAccount());

     if (myUser.isPresent()) {
-      log.info("Account have {}", myUser.get());
+      log.info("Account found, userId={}", myUser.get().getUserId());`
	}
];
