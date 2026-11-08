#!/bin/bash
# 用法：verify-worktree.sh <PR 編號> <base-dir>
# 等 PR 的 headRefOid 等於本機 HEAD，然後建立 <base-dir>/wt/pr-<PR> 的 detached worktree，
# 並依 PR 是否動到前端處理 frontend/node_modules。最後一行 stdout 是 worktree 的絕對路徑。
# 環境變數 SHIP_SYNC_TIMEOUT：等待同步的逾時秒數，預設 120。
set -u

usage() {
  echo "用法：$0 <PR 編號> <base-dir>" >&2
  exit 2
}

[ $# -eq 2 ] || usage
pr=$1
base=$2
case "$pr" in
  '' | *[!0-9]*) echo "PR 編號必須是數字：$pr" >&2; usage ;;
esac

timeout_s=${SHIP_SYNC_TIMEOUT:-120}
case "$timeout_s" in
  '' | *[!0-9]*) echo "SHIP_SYNC_TIMEOUT 必須是整數秒：$timeout_s" >&2; exit 2 ;;
esac

local_sha=$(git rev-parse HEAD) || { echo "無法取得本機 HEAD" >&2; exit 1; }

# 主工作目錄：worktree list 的第一筆
main_wt=$(git worktree list --porcelain | sed -n '1s/^worktree //p')
[ -n "$main_wt" ] || { echo "找不到主工作目錄" >&2; exit 1; }

mkdir -p "$base" || { echo "無法建立 $base" >&2; exit 1; }
base_abs=$(cd "$base" && pwd) || exit 1
target="$base_abs/wt/pr-$pr"
if [ -e "$target" ]; then
  echo "目標路徑已存在，拒絕覆蓋：${target}（請先 git worktree remove）" >&2
  exit 1
fi

# 等遠端 headRefOid 與本機 HEAD 同步
start=$(date +%s)
remote_sha=""
while :; do
  if out=$(gh pr view "$pr" --json headRefOid -q .headRefOid 2>&1); then
    remote_sha=$out
    [ "$remote_sha" = "$local_sha" ] && break
  else
    echo "gh pr view 失敗：$out" >&2
  fi
  now=$(date +%s)
  if [ $((now - start)) -ge "$timeout_s" ]; then
    echo "逾時（${timeout_s}s）：本機與 PR #$pr 的 headRefOid 未同步" >&2
    echo "  本機 HEAD：$local_sha" >&2
    echo "  遠端 headRefOid：${remote_sha:-（取不到）}" >&2
    exit 1
  fi
  sleep 2
done

# 判斷 PR 是否動到前端
files=$(gh pr diff "$pr" --name-only) || { echo "gh pr diff 失敗" >&2; exit 1; }
touch_frontend=0
touch_deps=0
if printf '%s\n' "$files" | grep -q '^frontend/'; then touch_frontend=1; fi
if printf '%s\n' "$files" | grep -qE '^frontend/(package\.json|yarn\.lock)$'; then touch_deps=1; fi

mkdir -p "$base_abs/wt" || exit 1
git worktree add --detach "$target" "$local_sha" >&2 || { echo "git worktree add 失敗" >&2; exit 1; }

# 準備環境失敗時，移除剛建好的 worktree，讓重跑不會被「目標路徑已存在」擋下
fail_cleanup() {
  echo "$1" >&2
  if git worktree remove --force "$target" >&2; then
    echo "已移除失敗的 worktree：${target}" >&2
  else
    echo "移除失敗，請手動執行 git worktree remove --force ${target}" >&2
  fi
  exit 1
}

if [ "$touch_deps" -eq 1 ]; then
  echo "PR 動到 package.json／yarn.lock，執行 yarn install --immutable" >&2
  (cd "$target/frontend" && yarn install --immutable) >&2 || fail_cleanup "yarn install --immutable 失敗"
elif [ "$touch_frontend" -eq 1 ]; then
  if [ ! -d "$main_wt/frontend/node_modules" ]; then
    fail_cleanup "PR 動到前端，但主工作目錄沒有 $main_wt/frontend/node_modules，無法建立 symlink"
  fi
  ln -s "$main_wt/frontend/node_modules" "$target/frontend/node_modules" || fail_cleanup "建立 node_modules symlink 失敗"
  echo "已建立 frontend/node_modules symlink -> $main_wt/frontend/node_modules" >&2
fi

echo "$target"
