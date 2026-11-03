---
name: ship
description: 多 agent 交付流程：任務指示 → 設定標準 → 執行工作 → 驗證成果 → 交付成果。使用者輸入 /ship <指令> 時使用。
argument-hint: <要完成的任務描述>
disable-model-invocation: true
---

# /ship：多 agent 交付流程

使用者的指令：$ARGUMENTS

你是總指揮（orchestrator）。subagent 不能再呼叫 subagent，所以每個階段的 agent 都由你呼叫。
階段之間**只透過 `docs/tasks/<task-id>/` 裡的檔案交接**，不要把你自己的推論塞進下一個 agent 的 prompt。

## 狀態檔

```
docs/tasks/<task-id>/          task-id 格式：YYYYMMDD-<英文-slug>
├── brief.md                   ① 任務指示
├── criteria.md                ② 設定標準
├── impl-log-<n>.md            ③ 執行工作（n = 第幾輪實作，從 1 開始）
├── verify-report-<n>.md       ④ 驗證成果
└── delivery.md                ⑤ 交付成果
```

## ① 任務指示

1. 讀 CLAUDE.md 與相關程式碼（排除 `backend/src/cloneCode/`），理解指令。
2. 不清楚的地方用 AskUserQuestion 問（最多一輪），不要猜。
3. 用 EnterWorktree 開 `ship-<task-id>` 工作區，之後所有階段都在這個工作區進行。
4. 寫 `brief.md`：目標、範圍內／範圍外、使用者原話、已知限制。

## ② 設定標準

1. 呼叫 `spec-writer` agent，prompt 只給 `brief.md` 的路徑。
2. 把它回傳的內容原樣寫進 `criteria.md`。
3. ⏸ **人工關卡**：把子任務切分、驗收條目、標記 `owner: human` 的項目整理給使用者，用 AskUserQuestion 問「核准／要修改」。
   - 沒有核准就不能進入 ③。
   - 要修改 → 帶著使用者的意見重新呼叫 `spec-writer`，再回到這個關卡。

## ③ 執行工作

依 `criteria.md` 的子任務順序，對每個 `owner: agent` 的子任務呼叫 `implementer` agent，prompt 給 `brief.md`、`criteria.md` 路徑與子任務編號。
implementer 的回報寫進 `impl-log-<n>.md`。

`owner: human` 的子任務（Dockerfile、compose、K8S YAML、nginx）**不交給 implementer**，留到 ⑤ 整理成觀念說明與待辦，讓使用者自己寫。

## ④ 驗證成果

呼叫 `verifier` agent，prompt 只給 `criteria.md` 路徑，並說明要檢查的改動範圍是 `git diff main...HEAD`。
**不要給它 impl-log**，讓驗證保持獨立。結果寫進 `verify-report-<n>.md`。

- 全部 PASS → 進入 ⑤。
- 有 FAIL，且退回次數還不到 2 次 → 用 SendMessage 把 `verify-report-<n>.md` 交給**同一個** implementer（保留它的上下文），n+1，修完後回到 ④。
- 已經退回 2 次（實作跑了 3 輪）仍有 FAIL → 停下來，向使用者說明卡在哪幾條標準，詢問要改標準、改設計，還是手動處理。

## ⑤ 交付成果

1. 在工作分支上 commit，訊息遵循專案現有的 commit 風格。
2. 寫 `delivery.md`：
   - 做了什麼、對應哪幾條驗收條目
   - 驗證證據摘要（測試統計、build 結果）
   - `owner: human` 的待辦：每項附上觀念說明、「為什麼這樣做」和「其他可行做法」
   - 已知限制與後續建議
3. push 與開 PR 是對外動作，要先問過使用者才做。

## 規則

- 每個子任務改動不超過 3 個檔案（CLAUDE.md 的規定）。需要更多時，請 spec-writer 切分，或在 ② 的關卡取得使用者同意。
- 每個階段開始時更新任務清單，讓使用者看得到目前進度。
