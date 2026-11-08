---
name: ship
description: "Multi-agent delivery flow: task brief → set criteria → do the work → verify → deliver. Handles one issue and produces one PR per run. Use when the user types /ship <instruction or issue number>."
argument-hint: "<task description, or an existing issue number such as #21>"
disable-model-invocation: true
---

# /ship: multi-agent delivery flow

User's instruction: $ARGUMENTS

You are the orchestrator. Subagents cannot call other subagents, so you call the agent for every stage yourself.

## Language

These instructions are in English, but **everything the user reads is written in Traditional Chinese (繁體中文)**: issues, issue/PR comments, PR descriptions, commit summaries, and your messages to the user. The fixed strings other agents look for are kept verbatim, so older issues and PRs stay compatible:

- Criteria marker: `📋 驗收標準`, `📋 驗收標準（第 n 版）`
- Verifier verdicts: `PASS`, `FAIL`, `PASS（有建議）`
- Finding severities: `🔴 必須修正`, `🟡 建議`, `⚪ 無法執行`, `⚪ 待確認`
- Mentor verdicts: `可以 commit`, `需要修改`

## Where state lives

All state lives on GitHub; do not keep separate files locally:

| Stage | Location |
|---|---|
| ① Task brief | Issue body |
| ② Criteria | Issue comment headed `📋 驗收標準`; a revision is a new comment headed `📋 驗收標準（第 n 版）`, and the latest one wins |
| ③ Work | Commits on the working branch, and the PR description |
| ④ Verification | PR comments (one verifier report per round) |
| ⑤ Delivery | The merged PR |

Agents hand off **only through the issue, the PR and the repo contents**. Do not put your own reasoning into the next agent's prompt.

## ① Task brief

1. Read CLAUDE.md and the relevant code (excluding `backend/src/cloneCode/`) and understand the instruction.
2. Ask about anything unclear with AskUserQuestion (at most one round). Do not guess.
3. **One issue maps to one PR.** If the task is too big for one PR, propose a split to the user, continue only with the first issue, and leave the rest for later /ship runs.
4. Prepare the issue:
   - Instruction is an issue number → read it with `gh issue view <N> --json title,body,comments` (do not use `--comments`: when output is not a terminal, it does not print the body)
   - Otherwise → `gh issue create`, with a body containing: goal, in scope / out of scope, the user's own words, known limitations
5. Prepare the branch: `git status --short` must be empty; run `git switch main && git pull --ff-only`, then create `<type>/<issue number>-<english-slug>` (for example `chore/21-ship-github-flow`).

## ② Set criteria

1. Call the `spec-writer` agent. It has no Bash, so paste the issue's title, body and the original text of every comment into the prompt (requirements added later often live in comments).
2. Prepend a line `## 📋 驗收標準` to what it returns (for a revision, `## 📋 驗收標準（第 n 版）`) and post the rest unchanged as an issue comment (`gh issue comment <N> --body-file <file>`). The implementer and the verifier find the criteria by that line.
3. ⏸ **Human gate** (the only one in the whole flow): summarize the subtask split, the acceptance items and the items marked `owner: human` for the user, and ask "approve / revise" with AskUserQuestion.
   - Without approval you may not enter ③.
   - Revise → call `spec-writer` again with the user's feedback, post it as a new version comment, and come back to this gate.
   - When the user agrees to more than 3 files, record the user's own words in an issue comment; the verifier checks for this.

## ③ Do the work

1. In subtask order, call the `implementer` agent for each `owner: agent` subtask. The prompt contains only the issue number, the subtask number and the branch name.
   - Use **one** implementer for the whole issue: call it with Agent for the first subtask, and continue it with SendMessage for later subtasks so it keeps its context.
   - If the session has no `implementer` agent type (the agent list is loaded at session start), use a general-purpose agent instead and tell it to read `.claude/agents/implementer.md` first and follow it exactly.
   - If it reports BLOCKED → handle by cause: ask the user when it needs the user's decision; go back to ② when the criteria are the problem.
2. `owner: human` subtasks (Dockerfile, compose, K8S YAML, nginx, and any `.github/workflows/*.yml` that builds or pushes images or deploys) are **not given to the implementer**. They go to the `mentor` agent, and the user writes them:
   - **Explain**: call mentor with "explain mode", the issue number and the subtask number in the prompt, plus the issue's title, body and all comments (it has no Bash). Relay its whole reply to the user, then stop and wait for the user to finish writing.
   - **Review**: when the user says they are done, ask the **same** mentor for a review via SendMessage, with the file path and the round number in the prompt. Relay the whole result to the user; if it has any 🔴, wait for the user's fix and review again in the next round.
   - Once the verdict is `可以 commit`, post the last review round as an issue comment (the verifier uses it to confirm the `owner: human` file was reviewed). Then commit those files yourself: the message states that the user wrote them and includes a line `Reviewed-in: <URL of that comment>` (`gh issue comment` prints the URL). Then move on to the next subtask. If you do not commit first, the next implementer will report BLOCKED because the working tree is not clean.
   - Do not edit the user's files yourself, and do not fill in answers the mentor did not give.
   - If the session has no `mentor` agent type, first tell the user: a general-purpose agent has file-writing tools, so "read-only" would rest only on the written rules. Once the user agrees, tell it to read `.claude/agents/mentor.md` first and follow it exactly; if they do not agree, ask the user to start a new session.
3. After all subtasks are done, check yourself first: the file count and scope of `git diff --stat main...HEAD` match the criteria from ②. Then push and open the PR:
   - `git push -u origin <branch>`
   - `gh pr create`, with a description containing: `Closes #<N>`, what was done, which commit covers each acceptance item, and the test statistics
   - **Do not paste the implementer's report into the PR**; the PR description contains only facts you confirmed yourself.

## ④ Verify

1. Get the PR's `headRefOid` (`gh pr view <PR> --json headRefOid`). Right after a push GitHub may lag a few seconds; wait until it equals the local `git rev-parse HEAD`, then create a detached worktree in the scratchpad:
   ```bash
   git worktree add --detach <scratchpad>/wt/pr-<PR> <headRefOid>
   ```
   **Do not use the Agent tool's `isolation: "worktree"`**: it creates the worktree from main, so the verifier would get code without the changes.
   When the PR touches the frontend, the new worktree has no `frontend/node_modules`, and the verifier may not install packages itself, so prepare it:
   - `package.json` and `yarn.lock` unchanged → link the main working directory's packages: `ln -s <main working directory>/frontend/node_modules <worktree>/frontend/node_modules` (`.gitignore` already ignores this path)
   - Changed → run `yarn install --immutable` in the worktree's `frontend/`
2. Call the `verifier` agent (PR mode). The prompt contains only the PR number and the worktree path. **Do not give it the implementer's report**, so verification stays independent.
   - If the session has no `verifier` agent type (the agent list is loaded at session start), use a general-purpose agent instead and tell it to read `.claude/agents/verifier.md` first and follow it exactly.
3. Post the report unchanged as a PR comment (`gh pr comment <PR> --body-file <file>`), then run `git worktree remove <path>`.
4. Act on the verdict:
   - **PASS** → go to ⑤.
   - **PASS（有建議）** → handle 🟡 and ⚪ separately:
     - 🟡: decide item by item — fix what is reasonable, open a new issue for what is out of scope, and for anything you will not handle, write the reason in a PR comment. Return to ④ for re-verification only if there are new commits; otherwise go straight to ⑤ (if the report also has ⚪, wait until those are handled as described below).
     - ⚪ 無法執行: caused by the environment (for example missing `frontend/node_modules`, a Java version other than 21, or the `pmd:check` command itself failing to run). The verifier cannot install packages, so you fix the environment and re-verify. **Writing a reason in a PR comment is not enough**:
       1. Step 3 already removed the worktree. Recreate it from the **same** `headRefOid` and prepare the environment as in step 1 (node_modules via symlink or `yarn install --immutable`); backend tests must use JDK 21 (see "Rules").
       2. Ask the **same** verifier to re-verify via SendMessage, post the report as a PR comment and remove the worktree as in step 3, then come back to this step with the new verdict.
       3. This kind of re-verification only fixes the environment with no new commits, so it **does not count** as a send-back.
       4. If you still cannot fix it after trying (for example, no JDK 21 on this machine) → stop and let the user decide with AskUserQuestion. You may not decide on your own that it can be merged.
     - ⚪ 待確認: the verifier suspects something but has no evidence.
       1. Investigate it yourself: read the `file:line` the report points to and run commands.
       2. Confirmed as a problem → treat it as 🔴 or 🟡 by severity: 🔴 follows the FAIL send-back flow below, 🟡 follows the 🟡 rule above.
       3. Confirmed not a problem → write how you checked and the result in a PR comment (commands and output, or `file:line`). A bare conclusion is not enough.
       4. Cannot tell → let the user decide with AskUserQuestion.
   - **FAIL**, with fewer than 2 send-backs so far → give the report to the **same** implementer via SendMessage. After the implementer commits the fix, you push, recreate the worktree from the new `headRefOid`, and ask the **same** verifier to re-verify via SendMessage so it can track the previous findings.
     - When a FAIL item is in an `owner: human` file, **do not give it to the implementer** (it may not touch those files). Go back to the mentor flow in ③: hand the verifier's findings to the same mentor; the user fixes the file; once the mentor's new review says `可以 commit`, post the new review as an issue comment as in ③, commit with the new comment's URL, then re-verify. This also counts as one send-back.
     - When the same report has findings in both agent files and `owner: human` files, take both paths, and re-verify once after everything is fixed. This counts as a single send-back.
     - When the report also has ⚪ 無法執行, fix the environment while recreating the worktree from the new `headRefOid`, in the way described under ⚪ 無法執行 in "PASS（有建議）" above (node_modules via symlink or `yarn install --immutable`; backend tests on JDK 21), so the same re-verification round covers those items too.
       - Here you use the implementer's **new** `headRefOid`, not the "same" `headRefOid` from sub-step 1 there.
       - This round still counts as only one send-back; fixing the environment is not counted separately.
       - If you still cannot fix the environment → do not skip re-verification because of it; still ask the verifier to re-verify the implementer's fix for the other items. After re-verification, follow sub-step 4 there: let the user decide on the parts that could not run with AskUserQuestion. You may not decide on your own that it can be merged.
     - When the report also has ⚪ 待確認, investigate it **before** sending the report to the implementer via SendMessage (or before handing `owner: human` findings to the mentor), as in sub-step 1 under ⚪ 待確認 in "PASS（有建議）" above (read the `file:line` the report points to, run commands). Then act on the result:
       - Confirmed 🔴 → send it out in the **same** round as the report's other 🔴: agent files to the implementer, `owner: human` files through the mentor flow above. This round still counts as one send-back; 🔴 found by your investigation are not counted separately.
       - Confirmed 🟡 → decide item by item as in the 🟡 rule under "PASS（有建議）": fix what is reasonable, open a new issue for what is out of scope, and for anything you will not handle, write the reason in a PR comment. 🟡 you decide to fix go to the implementer together with this round's 🔴 (`owner: human` files likewise through the mentor flow); not counted as a separate send-back. The sentence there "return to ④ only if there are new commits; otherwise go straight to ⑤" does not apply in the FAIL branch: FAIL always waits for the fix and re-verification.
       - Confirmed not a problem → write how you checked and the result in a PR comment (commands and output, or `file:line`). A bare conclusion is not enough.
       - Cannot tell → let the user decide with AskUserQuestion, and ask **before** handing off to the implementer (or mentor), so that if the user judges it a problem, the fix can go into the same round.
   - Any new commit after the SHA the verifier checked requires re-verification, whether it changes code or docs (this repo's agent and skill definitions are themselves Markdown, so "docs only" is hard to tell apart).
   - Already sent back 2 times (3 rounds of implementation) and still FAIL → stop, explain to the user which criteria it is stuck on, and ask whether to change the criteria, change the design, or handle it manually.
5. The verifier's out-of-scope findings become new issues; do not stuff them into this PR. If its suggestions involve permission settings (`settings.json`) or `CLAUDE.md`, leave the decision to the user.

## ⑤ Deliver

1. Confirm CI passed: `gh pr checks <PR>`. If it is still running, wait for it to finish; if it failed, treat it as FAIL and go back to the send-back flow in ④.
2. When all merge conditions hold, merge directly without asking the user again (the user authorized this on 2026-11-03):
   - You checked it yourself
   - The verifier judged the PR's current `headRefOid` as PASS, or as PASS（有建議） and:
     - every remaining 🟡 has a new issue or a stated reason for not handling it
     - every ⚪ has been handled as in ④ step 4: it disappeared after the environment was fixed and re-verified, it was confirmed not a problem with evidence in a PR comment, or the user decided it can be merged. A ⚪ can never be merged on a stated reason alone
   - All CI checks passed
   ```bash
   # --match-head-commit: refuses to merge if new commits were pushed to the PR after verification
   gh pr merge <PR> --squash --delete-branch --match-head-commit <full SHA of the headRefOid the verifier checked>
   git switch main && git pull --ff-only
   ```
3. **Post-merge verification (deployment PRs only).** Applies when the PR changes a workflow that deploys (decided by content, as in ③ step 2: it builds or pushes an image, logs in to a registry, or deploys over SSH or to a remote host; a workflow that only runs tests or lint does not count), or any other file whose merge triggers a deployment. For a pure code or docs PR skip this step; the rest of ⑤ is unchanged. A deploy workflow normally triggers on `push: main`, so success is only visible after the merge:
   - **Observe the run.** Get the merge commit SHA (`gh pr view <PR> --json mergeCommit --jq .mergeCommit.oid`), then find the run whose `headSha` equals it:
     ```bash
     gh run list --workflow <workflow file name> --branch main --json databaseId,headSha,status,conclusion
     gh run watch <run-id> --exit-status   # exit code non-zero when the run fails
     ```
   - If no run with that `headSha` appears after about 2 minutes, or `gh run watch` does not finish within a reasonable time (for example 15 minutes), do not assume success: tell the user and ask with AskUserQuestion whether to keep waiting or investigate (for example the workflow's trigger or path filter).
   - **Smoke test.** The backend has no health endpoint that works without login (`/actuator/**` needs ADMIN), so only check that the stack answers and nginx has not lost the backend. The host address is never written in this file or in the repo: take it from the user, or from a GitHub repo variable or secret that the user names. Check the HTTP status codes only:
     ```bash
     curl -s -o /dev/null -w '%{http_code}' http://<host>/                                                  # expect 200
     curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{}' http://<host>/api/login   # expect 4xx, not 502 or any 5xx
     ```
   - If this machine cannot reach the lab network, ask the user to run the commands and report the codes. You may not count the smoke test as passed on your own.
   - **On failure** (the run failed, or a smoke test result is not as expected): do not declare the delivery complete; tell the user first. Do not force-push. Rollback is a new revert PR, or redeploying the last known-good image tag. Let the user choose between rollback and a fix forward with AskUserQuestion. The deploy workflow is `owner: human`, so a fix goes through the mentor flow in ③, not to the implementer. The PR is already merged, so this does not count as a send-back in ④; handle it as a new issue and PR (a revert or fix) that goes through ① to ⑤ again.
4. Deliver to the user: what was done, which acceptance items it covers, a summary of the verification evidence, `owner: human` to-dos, newly opened issues, and known limitations. For a deployment PR, also report the workflow run result and the smoke test result.
5. Do not update `docs/PROGRESS.md` in every PR (it would use up the 3-file budget). After several PRs, or when a ROADMAP stage is done, open a separate docs PR to update it.

## Rules

- Each PR changes at most 3 files (CLAUDE.md). If more are needed, have spec-writer split the work, or get the user's consent at the ② gate.
- You handle pushing, opening issues/PRs, commenting and merging, but **never force-push**, and never work around a command that was denied.
- Backend tests always run on JDK 21; see "Self-verification" in `.claude/agents/implementer.md` for the command.
- Update the task list at the start of every stage so the user can see the current progress.
