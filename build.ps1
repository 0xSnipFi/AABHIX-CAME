param([switch]$Install)
$ErrorActionPreference='Stop'
Push-Location $PSScriptRoot
try {
    & "$PSScriptRoot\gradlew.bat" --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
    if($LASTEXITCODE -ne 0){throw 'Build checks failed'}
    $apk=Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk'
    Get-FileHash -LiteralPath $apk -Algorithm SHA256
    if($Install){
        $sdkPath=if($env:ANDROID_HOME){$env:ANDROID_HOME}else{$env:ANDROID_SDK_ROOT}
        $adbExecutable=if($sdkPath -and (Test-Path (Join-Path $sdkPath 'platform-tools\adb.exe'))){Join-Path $sdkPath 'platform-tools\adb.exe'}else{'adb'}
        & $adbExecutable install -r $apk
        if($LASTEXITCODE -ne 0){throw 'Install failed. Check device access and signing compatibility.'}
    }
} finally {Pop-Location}
