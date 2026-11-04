---
name: ship
description: 多 agent 交付流程：任務指示 → 設定標準 → 執行工作 → 驗證成果 → 交付成果。一次處理一個 issue、產出一個 PR。使用者輸入 /ship <指令或 issue 編號> 時使用。
argument-hint: "<要完成的任務描述，或既有的 issue 編號，例如 #21>"
disable-model-invocation: true
---

# /ship：多 agent 交付流程

使用者的指令：$ARGUMENTS

你是總指揮（orchestrator）。subagent 不能再呼叫 subagent，所以每個階段的 agent 都由你呼叫。

## 狀態放在哪裡

所有狀態都放在 GitHub，不在本機另存檔案：

| 階段 | 位置 |
|---|---|
| ① 任務指示 | issue 本文 |
| ② 設定標準 | issue 留言，標題「📋 驗收標準」；改版時新增一則「📋 驗收標準（第 n 版）」，以最新一則為準 |
| ③ 執行工作 | 工作分支上的 commit 與 PR 描述 |
| ④ 驗證成果 | PR 留言（verifier 報告，每一輪一則） |
| ⑤ 交付成果 | 合併後的 PR |

agent 之間**只透過 issue、PR 與 repo 內容交接**，不要把你自己的推論塞進下一個 agent 的 prompt。

## ① 任務指示

1. 讀 CLAUDE.md 與相關程式碼（排除 `backend/src/cloneCode/`），理解指令。
2. 不清楚的地方用 AskUserQuestion 問（最多一輪），不要猜。
3. **一個 issue 對應一個 PR。** 任務太大、一個 PR 放不下時，提出拆分方式給使用者，只對第一個 issue 繼續，其餘的留給之後的 /ship。
4. 準備 issue：
   - 指令是 issue 編號 → `gh issue view <N> --json title,body,comments` 讀現況（不要用 `--comments`，非終端機輸出時它不印本文）
   - 否則 → `gh issue create`，本文包含：目標、範圍內／範圍外、使用者原話、已知限制
5. 準備分支：`git status --short` 必須是空的；`git switch main && git pull --ff-only`，再建立 `<type>/<issue 編號>-<英文-slug>`（例如 `chore/21-ship-github-flow`）。

## ② 設定標準

1. 呼叫 `spec-writer` agent。它沒有 Bash，所以 prompt 直接附上 issue 的標題、本文與所有留言的原文（後來補的需求常常寫在留言裡）。
2. 在它回傳的內容最前面加上一行 `## 📋 驗收標準`（改版時寫 `## 📋 驗收標準（第 n 版）`），其餘原樣貼成 issue 留言（`gh issue comment <N> --body-file <檔案>`）。implementer 和 verifier 靠這一行找到標準。
3. ⏸ **人工關卡**（整個流程唯一的一關）：把子任務切分、驗收條目、標記 `owner: human` 的項目整理給使用者，用 AskUserQuestion 問「核准／要修改」。
   - 沒有核准就不能進入 ③。
   - 要修改 → 帶著使用者的意見重新呼叫 `spec-writer`，貼成新一版留言，再回到這個關卡。
   - 使用者同意超過 3 個檔案時，在 issue 留言記下使用者的原話，verifier 會檢查這一點。

## ③ 執行工作

1. 依子任務順序，對每個 `owner: agent` 的子任務呼叫 `implementer` agent，prompt 只給 issue 編號、子任務編號與分支名稱。
   - 整個 issue 用**同一個** implementer：第一個子任務用 Agent 呼叫，之後的子任務用 SendMessage 延續，讓它保留上下文。
   - session 中沒有 `implementer` agent 類型時（agent 清單在 session 開始時載入），改用 general-purpose agent，請它先讀 `.claude/agents/implementer.md` 並完全照規則做。
   - 回報是 BLOCKED → 依原因處理：需要使用者決定的就問使用者，標準有問題的就回到 ②。
2. `owner: human` 的子任務（Dockerfile、compose、K8S YAML、nginx）**不交給 implementer**，交給 `mentor` agent，由使用者自己寫：
   - **講解**：呼叫 mentor，prompt 給「講解模式」、issue 編號、子任務編號，並附上 issue 的標題、本文與所有留言（它沒有 Bash）。把它的回覆完整轉給使用者，然後停下來等使用者寫完。
   - **review**：使用者說寫好後，用 SendMessage 請**同一個** mentor review，prompt 給檔案路徑和第幾輪。把結果完整轉給使用者；有 🔴 就等使用者修改後再 review 下一輪。
   - 判定「可以 commit」後，把最後一輪 review 貼成 issue 留言（verifier 會據此確認 `owner: human` 的檔案經過 review），再由你 commit 這些檔案：訊息註明由使用者撰寫，並加上一行 `Reviewed-in: <那則留言的 URL>`（`gh issue comment` 會印出這個 URL），然後繼續下一個子任務。不先 commit 的話，下一個 implementer 會因為工作目錄不乾淨而 BLOCKED。
   - 不要自己動手改使用者的檔案，也不要替 mentor 補上它沒給的答案。
   - session 中沒有 `mentor` agent 類型時，先告訴使用者：改用 general-purpose agent 的話它有寫檔工具，「只讀不寫」只剩規則文字約束。使用者同意後，再請它先讀 `.claude/agents/mentor.md` 並完全照規則做；不同意就請使用者開新的 session。
3. 全部子任務完成後，自己先檢查一次：`git diff --stat main...HEAD` 的檔案數與範圍符合 ② 的標準，再 push 並開 PR：
   - `git push -u origin <branch>`
   - `gh pr create`，描述包含：`Closes #<N>`、做了什麼、每條驗收條目對應哪個 commit、測試統計
   - **不要把 implementer 的回報貼進 PR**，PR 描述只寫你自己確認過的事實。

## ④ 驗證成果

1. 取得 PR 的 `headRefOid`（`gh pr view <PR> --json headRefOid`）。剛 push 完時 GitHub 可能延遲幾秒才更新，要等到它等於本機的 `git rev-parse HEAD`，再在 scratchpad 建立 detached worktree：
   ```bash
   git worktree add --detach <scratchpad>/wt/pr-<PR> <headRefOid>
   ```
   **不要用 Agent 的 `isolation: "worktree"`**：它會從 main 建立工作區，verifier 會拿到沒有改動的程式碼。
   PR 改到前端時，新的 worktree 裡沒有 `frontend/node_modules`，verifier 也不能自己安裝，所以由你先準備：
   - `package.json`、`yarn.lock` 沒變 → 連結主工作目錄的套件：`ln -s <主工作目錄>/frontend/node_modules <worktree>/frontend/node_modules`（`.gitignore` 已忽略這個路徑）
   - 有變 → 在 worktree 的 `frontend/` 執行 `yarn install --immutable`
2. 呼叫 `verifier` agent（PR 模式），prompt 只給 PR 編號和 worktree 路徑。**不要給它 implementer 的回報**，讓驗證保持獨立。
   - session 中沒有 `verifier` agent 類型時（agent 清單在 session 開始時載入），改用 general-purpose agent，請它先讀 `.claude/agents/verifier.md` 並完全照規則做。
3. 把報告原樣貼成 PR 留言（`gh pr comment <PR> --body-file <檔案>`），用完執行 `git worktree remove <路徑>`。
4. 依判定處理：
   - **PASS** → 進入 ⑤。
   - **PASS（有建議）** → 🟡 和 ⚪ 分開處理：
     - 🟡：逐項決定，合理的就修正，範圍外的另開 issue，不處理的在 PR 留言寫明理由。有新的 commit 才回到 ④ 重驗；沒有就直接進入 ⑤（報告裡還有 ⚪ 時，要等 ⚪ 也照下面處理完）。
     - ⚪「無法執行」：是環境問題造成的（例如缺 `frontend/node_modules`、Java version 不是 21、`pmd:check` 指令本身跑不起來）。verifier 不能自己安裝套件，所以由你補好環境再重驗，**不能只在 PR 留言寫理由帶過**：
       1. 第 3 步已經移除 worktree，用**同一個** `headRefOid` 重建 worktree，照第 1 步準備環境（node_modules 用 symlink 或 `yarn install --immutable`）；後端測試要用 JDK 21（見「規則」）。
       2. 用 SendMessage 請**同一個** verifier 重驗，照第 3 步把報告貼成 PR 留言、移除 worktree，再依新的判定回到這一步處理。
       3. 這種重驗只是補環境、沒有新的 commit，**不計入**退回次數。
       4. 試過後還是補不好（例如本機沒有 JDK 21）→ 停下來用 AskUserQuestion 交給使用者決定，不能自己判定可以合併。
     - ⚪「待確認」：verifier 有懷疑但沒有證據。
       1. 由你自己查證：讀報告指出的 `檔案:行號`、跑指令。
       2. 確定是問題 → 依嚴重程度當成 🔴 或 🟡：🔴 走下面 FAIL 的退回流程，🟡 照上面 🟡 的規則處理。
       3. 確定不是問題 → 在 PR 留言寫出查證方式和結果（指令與輸出，或 `檔案:行號`），不能只寫結論。
       4. 查不清楚 → 用 AskUserQuestion 交給使用者決定。
   - **FAIL**，且退回次數還不到 2 次 → 用 SendMessage 把報告交給**同一個** implementer。implementer commit 修正後，由你 push，用新的 `headRefOid` 重建 worktree，再用 SendMessage 請**同一個** verifier 重驗，讓它追蹤上次的問題。
     - FAIL 的項目在 `owner: human` 的檔案上時，**不要交給 implementer**（它不能碰這些檔案），回到 ③ 的 mentor 流程：把 verifier 指出的問題交給同一個 mentor，由使用者修改、mentor 重新 review 判定「可以 commit」後，照 ③ 把新的 review 貼成 issue 留言、commit 時附上新留言的 URL，再重驗。這也算一次退回。
     - 同一份報告同時有 agent 檔案與 `owner: human` 檔案的問題時，兩條路都要走，全部修完後一起重驗，只算一次退回。
     - 報告中同時有 ⚪「無法執行」時，在用新的 `headRefOid` 重建 worktree 時就一併補好環境，做法照上面「PASS（有建議）」底下 ⚪「無法執行」的說明（node_modules 用 symlink 或 `yarn install --immutable`；後端測試要用 JDK 21），讓同一輪重驗一起涵蓋這些項目。
       - 這裡用的是 implementer 修正後的**新** `headRefOid`，不是那裡第 1 小步寫的「同一個」`headRefOid`。
       - 這一輪仍然只算一次退回，補環境不另外計算。
       - 試過後還是補不好 → 不要因此跳過重驗，其他項目仍照常請 verifier 重驗 implementer 的修正；重驗後再照那裡第 4 小步，把無法執行的部分用 AskUserQuestion 交給使用者決定，不能自己判定可以合併。
     - 報告中同時有 ⚪「待確認」時，要在用 SendMessage 把報告交給 implementer（或把 `owner: human` 檔案的問題交給 mentor）**之前**先查證，做法照上面「PASS（有建議）」底下 ⚪「待確認」的第 1 小步（讀報告指出的 `檔案:行號`、跑指令），再依查證結果處理：
       - 確定是 🔴 → 和報告裡其他 🔴 在**同一輪**一起交出去：agent 檔案交給 implementer，`owner: human` 檔案照上面的 mentor 流程處理。這一輪仍然只算一次退回，查證後新增的 🔴 不另外計算。
       - 確定是 🟡 → 照上面「PASS（有建議）」底下 🟡 的規則逐項決定：合理的就修正，範圍外的另開 issue，不處理的在 PR 留言寫明理由。決定修正的 🟡 和這一輪的 🔴 一起交給 implementer（`owner: human` 檔案同樣照 mentor 流程），不另外計算退回。那裡「有新的 commit 才回到 ④ 重驗；沒有就直接進入 ⑤」這一句在 FAIL 分支不適用：FAIL 一定要等修正後重驗。
       - 確定不是問題 → 在 PR 留言寫出查證方式和結果（指令與輸出，或 `檔案:行號`），不能只寫結論。
       - 查不清楚 → 用 AskUserQuestion 交給使用者決定，而且要在交給 implementer（或 mentor）**之前**問，這樣使用者判定是問題時，修正可以放進同一輪。
   - verifier 驗過的 SHA 之後只要有新的 commit，不管改的是程式碼還是文件，都要重驗（這個 repo 的 agent 與 skill 定義本身就是 Markdown，很難分清「只是文件」）。
   - 已經退回 2 次（實作跑了 3 輪）仍是 FAIL → 停下來，向使用者說明卡在哪幾條標準，詢問要改標準、改設計，還是手動處理。
5. verifier 在範圍外的發現另開 issue，不要塞進這個 PR。它的建議若牽涉權限設定（`settings.json`）或 `CLAUDE.md`，交給使用者決定。

## ⑤ 交付成果

1. 確認 CI 通過：`gh pr checks <PR>`。還在跑就等它跑完；失敗就當成 FAIL 回到 ④ 的退回流程。
2. 合併條件都成立時直接合併，不用再問使用者（使用者 2026-11-03 已授權）：
   - 你自己檢查過
   - verifier 對 PR 目前的 `headRefOid` 判定 PASS，或 PASS（有建議）且：
     - 剩下的 🟡 都已另開 issue 或說明不處理的理由
     - ⚪ 都已依 ④ 第 4 步處理完：補環境重驗後消失、查證確定不是問題且 PR 留言附了證據，或使用者已決定可以合併。⚪ 不能只靠說明理由合併
   - CI 全部通過
   ```bash
   # --match-head-commit：PR 在驗證之後又被推了新 commit 時，拒絕合併
   gh pr merge <PR> --squash --delete-branch --match-head-commit <交給 verifier 檢驗的 headRefOid（完整 SHA）>
   git switch main && git pull --ff-only
   ```
3. 向使用者交付：做了什麼、對應哪幾條驗收條目、驗證證據摘要、`owner: human` 的待辦、另開的 issue、已知限制。
4. `docs/PROGRESS.md` 不在每個 PR 更新（會佔掉 3 個檔案的額度）。累積數個 PR 或完成 ROADMAP 的一個階段時，另開 docs PR 更新。

## 規則

- 每個 PR 改動不超過 3 個檔案（CLAUDE.md 的規定）。需要更多時，請 spec-writer 切分，或在 ② 的關卡取得使用者同意。
- push、開 issue／PR、留言、合併都由你處理，但**不要 force-push**，也不要繞過被拒絕的指令。
- 跑後端測試一律用 JDK 21，指令寫法見 `.claude/agents/implementer.md` 的「自我驗證」。
- 每個階段開始時更新任務清單，讓使用者看得到目前進度。
