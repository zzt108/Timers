[CmdletBinding(SupportsShouldProcess = $true)]
param()

# Helper to set User environment variable if changed
function Set-EnvVar {
    [CmdletBinding(SupportsShouldProcess = $true)]
    param($Name, $Value)
    
    $current = [Environment]::GetEnvironmentVariable($Name, "User")
    
    if ($current -ne $Value) {
        if ($PSCmdlet.ShouldProcess("Env:User:$Name", "Set to '$Value'")) {
            [Environment]::SetEnvironmentVariable($Name, $Value, "User")
            Write-Host "Updated $Name to: $Value" -ForegroundColor Green
        }
    }
    else {
        Write-Verbose "$Name is already set correctly."
    }
    
    # Update current session too
    if ($PSCmdlet.ShouldProcess("Env:Session:$Name", "Set to '$Value'")) {
        Set-Item -Path "env:$Name" -Value $Value
    }
}

# Helper to add to User Path if missing
function Add-ToPath {
    [CmdletBinding(SupportsShouldProcess = $true)]
    param($PathToAdd)
    
    if (-not (Test-Path $PathToAdd)) { return }
    
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
    
    # Check if path already exists (careful splitting)
    if (($currentPath -split ';') -notcontains $PathToAdd) {
        if ($PSCmdlet.ShouldProcess("Env:User:Path", "Add '$PathToAdd'")) {
            $newPath = "$currentPath;$PathToAdd"
            [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
            Write-Host "Added to Path: $PathToAdd" -ForegroundColor Green
        }
        
        # Update current session too (only if modifying user path or forced)
        if ($PSCmdlet.ShouldProcess("Env:Session:Path", "Add '$PathToAdd'")) {
            $env:Path += ";$PathToAdd"
        }
    }
    else {
        Write-Verbose "Path already contains: $PathToAdd"
    }
}

# 1. Locate Android SDK
$sdkPath = $env:ANDROID_HOME
if (-not $sdkPath) {
    # Check common locations
    $candidates = @(
        "$env:LOCALAPPDATA\Android\Sdk",
        "$env:ProgramFiles\Android\Android Studio\sdk",
        "$env:ProgramFiles\Android\sdk"
    )
    foreach ($p in $candidates) {
        if (Test-Path "$p\platform-tools\adb.exe") {
            $sdkPath = $p
            break
        }
    }
}

if ($sdkPath) {
    Set-EnvVar "ANDROID_HOME" $sdkPath
    Set-EnvVar "ANDROID_SDK_ROOT" $sdkPath # For older tools
    
    Add-ToPath "$sdkPath\platform-tools"
    
    # cmdline-tools logic: use latest
    if (Test-Path "$sdkPath\cmdline-tools\latest\bin") {
        Add-ToPath "$sdkPath\cmdline-tools\latest\bin"
    }
    else {
        $versions = Get-ChildItem "$sdkPath\cmdline-tools" -Directory | Sort-Object Name -Descending
        if ($versions) {
            Add-ToPath "$($versions[0].FullName)\bin"
        }
    }
}
else {
    Write-Warning "Could not locate Android SDK. Please install Android Studio."
}

# 2. Locate Java JDK
$javaPath = $env:JAVA_HOME
if (-not $javaPath) {
    # Check bundled JDK (Android Studio) or standard Java
    $candidates = @(
        "$env:ProgramFiles\Android\Android Studio\jbr", # JetBrains Runtime (common in newer AS)
        "$env:ProgramFiles\Android\Android Studio\jre",
        "$env:ProgramFiles\Java\jdk*" # Wildcard search
    )
    
    foreach ($p in $candidates) {
        if ($p.Contains("*")) {
            $resolved = Get-Item $p -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
            $p = $resolved.FullName
        }
        if ($p -and (Test-Path "$p\bin\java.exe")) {
            $javaPath = $p
            break
        }
    }
}

if ($javaPath) {
    Set-EnvVar "JAVA_HOME" $javaPath
    Add-ToPath "$javaPath\bin"
}
else {
    Write-Warning "Could not locate Java JDK. Gradle tasks may fail."
}

Write-Host "Setup complete. Please RESTART your terminal/VS Code." -ForegroundColor Cyan
