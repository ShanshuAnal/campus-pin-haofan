param(
    [string]$Message = ""
)

if ([string]::IsNullOrWhiteSpace($Message)) {
    Write-Host "请输入 commit message，例如："
    Write-Host ".\scripts\git-save.ps1 `"docs: align api and sql contract`""
    exit 1
}

Write-Host "当前 Git 状态："
git status

Write-Host ""
Write-Host "即将提交以下变更："
git diff --stat

Write-Host ""
$confirm = Read-Host "确认提交？输入 y 继续"

if ($confirm -ne "y") {
    Write-Host "已取消提交。"
    exit 0
}

git add .
git commit -m $Message

Write-Host ""
Write-Host "提交完成。"
git log --oneline -5