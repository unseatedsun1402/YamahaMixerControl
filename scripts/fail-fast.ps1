# Runs the same gates as CI, stopping at the first failure:
#   1. python syntax  2. javac lint  3. unit tests  4. functional tests
Set-Location (Join-Path $PSScriptRoot '..')

$python = if (Get-Command python -ErrorAction SilentlyContinue) { 'python' } else { 'python3' }

$hints = @{
    'python-syntax'    = 'Fix the syntax error reported above.'
    'java-lint'        = 'Fix the javac [lint] warnings above (they are errors). Report: build\reports\problems\problems-report.html'
    'unit-tests'       = "Open build\reports\tests\unitTest\index.html. Rerun one class: .\gradlew unitTest --tests '<fqcn>'"
    'functional-tests' = "Open build\reports\tests\functionalTest\index.html. Rerun one class: .\gradlew functionalTest --tests '<fqcn>'"
}

function Invoke-Step([string]$Name, [scriptblock]$Command) {
    Write-Host "==> $Name"
    & $Command
    if ($LASTEXITCODE -ne 0) {
        Write-Host "FAIL-FAST: '$Name' failed. Hint: $($hints[$Name])" -ForegroundColor Red
        exit 1
    }
}

Invoke-Step 'python-syntax' { & $python -W error -m compileall -q src/main/python .github/hooks }
Invoke-Step 'java-lint' { & .\gradlew.bat lintJava --console=plain }
Invoke-Step 'unit-tests' { & .\gradlew.bat unitTest --console=plain }
Invoke-Step 'functional-tests' { & .\gradlew.bat functionalTest --console=plain }

Write-Host 'All checks passed.'
