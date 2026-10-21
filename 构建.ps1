param([switch]$Offline)
$ErrorActionPreference='Stop'
$tasks=@('assembleDebug','lintDebug','--console=plain')
if($Offline){$tasks+='--offline'}
& (Join-Path $PSScriptRoot 'gradlew.bat') -p $PSScriptRoot @tasks
if($LASTEXITCODE -ne 0){throw 'Build or lint failed.'}
