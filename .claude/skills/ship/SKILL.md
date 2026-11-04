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
   - 指令是 issue 編號 → `gh issue view <N> --comments` 讀現況
   - 否則 → `gh issue create`，本文包含：目標、範圍內／範圍外、使用者原話、已知限制
5. 準備分支：`git status --short` 必須是空的；`git switch main && git pull --ff-only`，再建立 `<type>/<issue 編號>-<英文-slug>`（例如 `chore/21-ship-github-flow`）。

## ② 設定標準

1. 呼叫 `spec-writer` agent。它沒有 Bash，所以 prompt 直接附上 issue 本文的原文。
2. 把它回傳的內容原樣貼成 issue 留言（`gh issue comment <N> --body-file <檔案>`），標題「📋 驗收標準」。
3. ⏸ **人工關卡**（整個流程唯一的一關）：把子任務切分、驗收條目、標記 `owner: human` 的項目整理給使用者，用 AskUserQuestion 問「核准／要修改」。
   - 沒有核准就不能進入 ③。
   - 要修改 → 帶著使用者的意見重新呼叫 `spec-writer`，貼成新一版留言，再回到這個關卡。
   - 使用者同意超過 3 個檔案時，在 issue 留言記下使用者的原話，verifier 會檢查這一點。

## ③ 執行工作

1. 依子任務順序，對每個 `owner: agent` 的子任務呼叫 `implementer` agent，prompt 只給 issue 編號、子任務編號與分支名稱。
   - 整個 issue 用**同一個** implementer：第一個子任務用 Agent 呼叫，之後的子任務用 SendMessage 延續，讓它保留上下文。
   - 回報是 BLOCKED → 依原因處理：需要使用者決定的就問使用者，標準有問題的就回到 ②。
2. `owner: human` 的子任務（Dockerfile、compose、K8S YAML、nginx）**不交給 implementer**：
   - 向使用者說明要寫什麼、相關觀念、「為什麼這樣做」和「其他可行做法」，然後停下來等使用者寫完。
   - 使用者寫完後 review 他們的檔案，提出修改建議，由使用者自己改。
3. 全部子任務完成後，自己先檢查一次：`git diff --stat main...HEAD` 的檔案數與範圍符合 ② 的標準，再 push 並開 PR：
   - `git push -u origin <branch>`
   - `gh pr create`，描述包含：`Closes #<N>`、做了什麼、每條驗收條目對應哪個 commit、測試統計
   - **不要把 implementer 的回報貼進 PR**，PR 描述只寫你自己確認過的事實。

## ④ 驗證成果

1. 取得 PR 的 `headRefOid`（`gh pr view <PR> --json headRefOid`），在 scratchpad 建立 detached worktree：
   ```bash
   git worktree add --detach <scratchpad>/wt/pr-<PR> <headRefOid>
   ```
   **不要用 Agent 的 `isolation: "worktree"`**：它會從 main 建立工作區，verifier 會拿到沒有改動的程式碼。
2. 呼叫 `verifier` agent（PR 模式），prompt 只給 PR 編號和 worktree 路徑。**不要給它 implementer 的回報**，讓驗證保持獨立。
   - session 中沒有 `verifier` agent 類型時（agent 清單在 session 開始時載入），改用 general-purpose agent，請它先讀 `.claude/agents/verifier.md` 並完全照規則做。
3. 把報告原樣貼成 PR 留言（`gh pr comment <PR> --body-file <檔案>`），用完執行 `git worktree remove <路徑>`。
4. 依判定處理：
   - **PASS** → 進入 ⑤。
   - **PASS（有建議）** → 處理合理的 🟡。之後若有程式碼或設定的改動（不是只改註解或文件），就回到 ④ 重驗。
   - **FAIL**，且退回次數還不到 2 次 → 用 SendMessage 把報告交給**同一個** implementer。它 commit 修正後 push，用新的 `headRefOid` 重建 worktree，再用 SendMessage 請**同一個** verifier 重驗，讓它追蹤上次的問題。
   - 已經退回 2 次（實作跑了 3 輪）仍是 FAIL → 停下來，向使用者說明卡在哪幾條標準，詢問要改標準、改設計，還是手動處理。
5. verifier 在範圍外的發現另開 issue，不要塞進這個 PR。它的建議若牽涉權限設定（`settings.json`）或 `CLAUDE.md`，交給使用者決定。

## ⑤ 交付成果

1. 確認 CI 通過：`gh pr checks <PR>`。還在跑就等它跑完；失敗就當成 FAIL 回到 ④ 的退回流程。
2. 合併條件都成立時直接合併，不用再問使用者（使用者 2026-11-03 已授權）：
   - 你自己檢查過
   - verifier 判定 PASS，或 PASS（有建議）且建議已處理
   - CI 全部通過
   ```bash
   gh pr merge <PR> --squash --delete-branch
   git switch main && git pull --ff-only
   ```
3. 向使用者交付：做了什麼、對應哪幾條驗收條目、驗證證據摘要、`owner: human` 的待辦、另開的 issue、已知限制。
4. `docs/PROGRESS.md` 不在每個 PR 更新（會佔掉 3 個檔案的額度）。累積數個 PR 或完成 ROADMAP 的一個階段時，另開 docs PR 更新。

## 規則

- 每個 PR 改動不超過 3 個檔案（CLAUDE.md 的規定）。需要更多時，請 spec-writer 切分，或在 ② 的關卡取得使用者同意。
- push、開 issue／PR、留言、合併都由你處理，但**不要 force-push**，也不要繞過被拒絕的指令。
- 跑後端測試一律用 JDK 21，指令寫法見 `.claude/agents/implementer.md` 的「自我驗證」。
- 每個階段開始時更新任務清單，讓使用者看得到目前進度。
