#!/bin/bash
# 用法：check-frontmatter.sh [檔案...]
# 不帶參數時檢查 .claude/agents/*.md 與 .claude/skills/*/SKILL.md（相對於 repo 根目錄）。
# 檢查：有 frontmatter、YAML 可解析、有 name 與 description、
# 未加引號的值不含 " #"（YAML 會把它當註解吞掉）。唯讀，任何檔案有問題就整體以非 0 結束。
set -u

command -v ruby >/dev/null 2>&1 || { echo "FAIL: 找不到 ruby，無法解析 frontmatter" >&2; exit 1; }

if [ $# -eq 0 ]; then
  root=$(git rev-parse --show-toplevel) || { echo "FAIL: 無法取得 repo 根目錄" >&2; exit 1; }
  set -- "$root"/.claude/agents/*.md "$root"/.claude/skills/*/SKILL.md
fi

checker=$(cat <<'RUBY'
require "yaml"
path = ARGV[0]
lines = File.readlines(path)
abort "沒有 frontmatter（第一行不是 ---）" unless lines[0] && lines[0].strip == "---"
idx = lines[1..-1].index { |l| l.strip == "---" }
abort "frontmatter 沒有結尾的 ---" unless idx
fm = lines[1, idx]
begin
  data = YAML.safe_load(fm.join)
rescue Exception => e
  # Psych 的行號從 frontmatter 第一行算起，換算成檔案行號（+1，因為第 1 行是 ---）
  abort "YAML 解析錯誤：" + e.message.gsub(/line (\d+)/) { "檔案第 #{Regexp.last_match(1).to_i + 1} 行" }
end
abort "frontmatter 不是 key: value 的對應表" unless data.is_a?(Hash)
%w[name description].each do |k|
  v = data[k]
  abort "缺少必要欄位 #{k}" if v.nil? || v.to_s.strip.empty?
end
fm.each_with_index do |l, i|
  l = l.chomp
  next unless l =~ /\A[A-Za-z0-9_-]+:[ \t]+(.*)\z/
  v = Regexp.last_match(1)
  next if v =~ /\A["'|>]/
  abort "檔案第 #{i + 2} 行未加引號的值含「空白或 TAB 後接 #」，YAML 會把後面當註解截斷：#{l.strip}" if v =~ /[ \t]#/
end
RUBY
)

fail=0
for f in "$@"; do
  if [ ! -f "$f" ]; then
    echo "FAIL: ${f}：檔案不存在"
    fail=1
    continue
  fi
  if msg=$(ruby -e "$checker" "$f" 2>&1); then
    echo "PASS: ${f}"
  else
    echo "FAIL: ${f}：${msg}"
    fail=1
  fi
done
exit "$fail"
