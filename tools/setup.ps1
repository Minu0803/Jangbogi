$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$root  = Split-Path -Parent $PSScriptRoot
$tools = Join-Path $root 'tools'
$dl    = Join-Path $tools 'dl'
New-Item -ItemType Directory -Force $dl | Out-Null

function Step($name, [scriptblock]$body) {
    $t = Get-Date
    try {
        & $body
        Write-Output ("STEP {0} OK ({1:n0}s)" -f $name, ((Get-Date) - $t).TotalSeconds)
    } catch {
        Write-Output "STEP $name FAIL: $($_.Exception.Message)"
        throw
    }
}

Step 'JDK17' {
    if (-not (Test-Path "$tools\jdk17\bin\java.exe")) {
        Invoke-WebRequest 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile "$dl\jdk17.zip" -UseBasicParsing
        Expand-Archive "$dl\jdk17.zip" -DestinationPath "$dl\jdk17x" -Force
        $inner = Get-ChildItem "$dl\jdk17x" -Directory | Select-Object -First 1
        Move-Item $inner.FullName "$tools\jdk17"
        Remove-Item "$dl\jdk17x" -Recurse -Force -ErrorAction SilentlyContinue
    }
    if (-not (Test-Path "$tools\jdk17\bin\java.exe")) { throw 'jdk missing after extract' }
}

Step 'GRADLE89' {
    if (-not (Test-Path "$tools\gradle-8.9\bin\gradle.bat")) {
        Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile "$dl\gradle.zip" -UseBasicParsing
        Expand-Archive "$dl\gradle.zip" -DestinationPath $tools -Force
    }
    if (-not (Test-Path "$tools\gradle-8.9\bin\gradle.bat")) { throw 'gradle missing after extract' }
}

Step 'SDKTOOLS' {
    $sdk = "$tools\android-sdk"
    if (-not (Test-Path "$sdk\cmdline-tools\latest\bin\sdkmanager.bat")) {
        Invoke-WebRequest 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip' -OutFile "$dl\cmdtools.zip" -UseBasicParsing
        Expand-Archive "$dl\cmdtools.zip" -DestinationPath "$dl\cmdx" -Force
        New-Item -ItemType Directory -Force "$sdk\cmdline-tools" | Out-Null
        Move-Item "$dl\cmdx\cmdline-tools" "$sdk\cmdline-tools\latest"
        Remove-Item "$dl\cmdx" -Recurse -Force -ErrorAction SilentlyContinue
    }
    New-Item -ItemType Directory -Force "$sdk\licenses" | Out-Null
    Set-Content "$sdk\licenses\android-sdk-license" "8933bad161af4178b1185d1a37fbf41ea5269c55`nd56f5187479451eabf01fb78af6dfcb131a6481e`n24333f8a63b6825ea9c5514f83c2829b004d1fee" -Encoding ascii
    Set-Content "$sdk\licenses\android-sdk-preview-license" "84831b9409646a918e30573bab4c9c91346d8abd" -Encoding ascii
}

Step 'SDKPKGS' {
    $sdk = "$tools\android-sdk"
    $env:JAVA_HOME = "$tools\jdk17"
    & "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root=$sdk 'platform-tools' 'platforms;android-34' 'build-tools;34.0.0' | Out-Null
    if (-not (Test-Path "$sdk\platforms\android-34")) { throw 'platform-34 missing after install' }
    if (-not (Test-Path "$sdk\build-tools\34.0.0")) { throw 'build-tools missing after install' }
}

Step 'PROPS' {
    Set-Content "$root\local.properties" ("sdk.dir=" + (($tools -replace '\\', '/') + '/android-sdk')) -Encoding ascii
}

# 의존성 프리다운로드용 웜업 빌드. 에이전트가 소스를 쓰는 중이라 컴파일 실패 가능 — 실패해도 정상(캐시 목적)
Step 'WARMUP' {
    $env:JAVA_HOME = "$tools\jdk17"
    & "$tools\gradle-8.9\bin\gradle.bat" -p $root ':app:assembleRelease' --console=plain -q
    Write-Output "warmup gradle exit=$LASTEXITCODE (실패해도 의존성 캐시는 확보됨)"
}

Write-Output 'SETUP COMPLETE'
