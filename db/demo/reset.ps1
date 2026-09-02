# Rebuild the demo database. PowerShell entry point for db/demo/reset.sh.
#
#   .\db\demo\reset.ps1
#
# A wrapper rather than a second implementation, so there is one script to keep
# correct.
#
# It finds Git Bash explicitly instead of trusting `bash` on PATH, because on
# this machine `bash` resolves to C:\Windows\system32\bash.exe - the WSL
# launcher. That runs the script inside a Linux distro with a different
# filesystem and no reason to see the same Docker CLI, which fails in a way that
# looks like the script is broken rather than like the wrong shell.

$ErrorActionPreference = 'Stop'

$candidates = @(
    "$env:ProgramFiles\Git\bin\bash.exe",
    "${env:ProgramFiles(x86)}\Git\bin\bash.exe",
    "$env:LOCALAPPDATA\Programs\Git\bin\bash.exe"
)

$bash = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $bash) {
    throw "Git Bash not found. Looked in: $($candidates -join ', ')"
}

$script = Join-Path $PSScriptRoot 'reset.sh'
& $bash $script
exit $LASTEXITCODE
