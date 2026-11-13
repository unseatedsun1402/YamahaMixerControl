# Auto-formats Java files changed relative to $env:BASE_REF (default origin/main)
# with the repo's google-java-format jar, then runs fail-fast.ps1.
Set-Location (Join-Path $PSScriptRoot '..')

$baseRef = if ($env:BASE_REF) { $env:BASE_REF } else { 'origin/main' }
$jar = 'tools/google-java-format.jar'

if (Test-Path $jar) {
    $files = @(
        (git diff --name-only --diff-filter=d $baseRef -- '*.java' 2>$null)
        (git diff --name-only --diff-filter=d -- '*.java')
        (git ls-files --others --exclude-standard -- '*.java')
    ) | Where-Object { $_ } | Sort-Object -Unique
    if ($files) {
        Write-Host "==> formatting $(@($files).Count) changed Java file(s)"
        java -jar $jar -i @files
        if ($LASTEXITCODE -ne 0) { Write-Warning 'Formatter failed; continuing.' }
    }
} else {
    Write-Warning "$jar not found; skipping formatting."
}

& (Join-Path $PSScriptRoot 'fail-fast.ps1')
exit $LASTEXITCODE
