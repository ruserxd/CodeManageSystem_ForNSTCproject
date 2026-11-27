# Git 慣例

> 這份文件是 commit 與 PR 格式的**唯一定義**。/ship、/autoship 與一般 session 都依照這份文件；其他地方只引用，不另外定義。

## 目標

`main` 的 git log 是寫給日後讀歷史的人看的：**一個 PR 在 `main` 上只留一個 commit**，看 subject 就知道做了什麼，看 body 就知道為什麼。審查過程（verifier 報告、修改了幾輪）放在 PR 留言，不寫進 log。

## Repo 設定（已套用，#163）

| 設定 | 值 | 作用 |
|---|---|---|
| 合併方式 | 只允許 squash | `main` 保持一條直線，每個 PR 只有一個 commit |
| squash 預設標題 | `PR_TITLE` | GitHub 自動在後面加上 ` (#PR)` |
| squash 預設內容 | `PR_BODY` | 不會再把所有子 commit 訊息串起來 |
| 合併後刪除分支 | 開啟 | |

預設值只是安全網。合併時仍然要依下方「合併」一節，自己指定 subject 和 body。

## Commit 訊息格式

```
<type>(<scope>): <做了什麼> (#<PR>)

<為什麼，一兩句>

- <重點 1>
- <重點 2>

Closes #<issue>
Reviewed-in: <URL>
Co-Authored-By: Claude <noreply@anthropic.com>
```

- **語言**：用繁體中文寫；`type`、`scope` 用英文。
- **Subject**：一行，不加句號，結尾只放 PR 編號，**不要放 issue 編號**（issue 寫在 `Closes`）。
  - ✅ `feat(frontend): 前端改名為 MethodTracker (#154)`
  - ❌ `feat(frontend): 前端改名為 MethodTracker（#153） (#154)`
- **Body**：先用一兩句說明為什麼要改，再用 3–5 個重點條列，一個功能一條，不要一個子 commit 一條。改動很小時可以只寫一句，或不寫 body。
- **不寫審查過程**：不寫 verifier／mentor 審了幾輪，也不寫「依建議修正」。
- **Trailer**：放在最後一段，順序固定：`Closes #<issue>`（或 `Refs #<issue>`）→ 所有 `Reviewed-in:` → **只有一行** `Co-Authored-By: Claude <noreply@anthropic.com>`（不寫模型型號，有 AI 參與才加）。

### type

`feat`（新功能，包含新的 skill／agent 流程；安全強化也用 `feat`）、`fix`（只用在已經進 `main` 的 bug）、`refactor`、`test`、`docs`、`ci`、`build`、`chore`（只用在真正的雜項）。

### scope

`backend｜frontend｜nginx｜compose｜ci｜agents｜skills｜docs`。可以省略，但不可以用清單以外的值（例如不能用 `skill`、`ship`）。

## PR

- **標題**：就是 commit 的 subject，但不寫 ` (#PR)`，GitHub 會自動補上。
- **描述**：寫成將來 commit body 的樣子（為什麼 + 重點 + `Closes #issue`）。測試步驟、驗證結果放在 PR 留言。
- **一個 PR 只做一件事**，對應一個 issue；發現的其他問題另開 issue。

## 分支上的子 commit

子 commit 合併時會被 squash 掉，所以不必寫得很仔細，但仍要遵守 `<type>(<scope>): <摘要>` 格式。

- 修正審查意見用 `git commit --fixup <sha>`，不要另外寫一個 `fix(...)`。
- 子 commit 不加 `Co-Authored-By`，合併時只在 squash 訊息寫一行。
- 不要用 `--amend`、`rebase`、`reset` 改寫已有的 commit，不論有沒有推送；要修正就加新的 commit。唯一的例外是「本機設定」一節的 `pull.rebase`。

## 合併

先把訊息寫進檔案，再指定 subject 和 body 合併：

```bash
gh pr merge <PR> --squash --delete-branch --match-head-commit <verifier 驗過的 SHA> \
  --subject "<type>(<scope>): <做了什麼> (#<PR>)" --body-file <訊息檔>
git switch main && git fetch && git merge --ff-only origin/main
```

- 同步 `main` 用 `fetch` + `merge --ff-only`，不要用 `git pull --ff-only`：開了 `pull.rebase` 之後，只要工作目錄有未提交的修改，`pull` 就會拒絕執行（#166）。`merge --ff-only` 會保留不衝突的未提交修改。
- 自己指定 `--subject` 時，GitHub 不會自動補 PR 編號，要自己寫上 `(#<PR>)`。
- `Reviewed-in:`：用 `gh pr view <PR> --json commits` 列出所有子 commit，把每一行 `Reviewed-in:` 原樣抄過來，不要去重。子 commit 沒有就不寫。如果改用 `git log`，本機 `main` 可能落後，要先 `git fetch`，再用 `origin/main..HEAD`，不要用 `main..HEAD`。
- `--match-head-commit` 要填 verifier 驗過的**完整 SHA**：驗證之後如果又有新的 commit 推上來，合併會被拒絕。
- body 檔的 `Co-Authored-By` 照本文件寫成 `Claude <noreply@anthropic.com>`，不要抄子 commit 或系統提示的寫法。
- 不要用 GitHub 網頁上的按鈕直接合併：網頁會帶入 PR 描述的全文，包含 `🤖 Generated with Claude Code` 這類 PR 專用的文字。

## 本機設定

```bash
git config pull.rebase true   # pull 時改用 rebase，不產生 "Merge remote-tracking branch" commit
```

這是「不要改寫已有的 commit」唯一的例外：`pull` 時 rebase 只會把本機還沒推送的 commit 接到最新的遠端後面，已推送的 commit 不會被改動。agent（implementer 等）不執行 `pull`，所以對它們沒有例外；/ship、/autoship 的協調者只用 `--ff-only` 同步 `main`，分岔時會直接中止，不會改寫 commit。

## 不改寫既有歷史

#163 之前的 commit（`Update README.md`、串起來的長訊息）保持原樣。這是公開 repo，SHA 已經被 PR、issue 和文件引用；改寫歷史會讓這些連結失效，也必須 force push `main`。規則從 #163 起生效。
