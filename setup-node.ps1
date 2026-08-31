# setup-node.ps1 — Download and extract a portable Node.js environment in the workspace
$nodeZipUrl = "https://nodejs.org/dist/v20.11.1/node-v20.11.1-win-x64.zip"
$workspace = "c:\Users\hp\AndroidStudioProjects\Takaful"
$destZip = Join-Path $workspace "node.zip"
$extractPath = Join-Path $workspace "node-env"

Write-Host "1. Creating target directory: $extractPath"
$null = New-Item -ItemType Directory -Force -Path $extractPath

Write-Host "2. Downloading Node.js from $nodeZipUrl..."
try {
    # Set SecurityProtocol to TLS 1.2
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $nodeZipUrl -OutFile $destZip -UseBasicParsing
    Write-Host "Download completed successfully."
} catch {
    Write-Error "Failed to download Node.js: $_"
    exit 1
}

Write-Host "3. Extracting archive..."
try {
    Expand-Archive -Path $destZip -DestinationPath $extractPath -Force
    Write-Host "Extraction completed successfully."
} catch {
    Write-Error "Failed to extract Node.js: $_"
    exit 1
}

Write-Host "4. Cleaning up archive..."
if (Test-Path $destZip) {
    Remove-Item -Path $destZip -Force
}

$nodeDir = Get-ChildItem -Path $extractPath -Directory | Select-Object -First 1
if ($nodeDir) {
    $nodeExe = Join-Path $nodeDir.FullName "node.exe"
    if (Test-Path $nodeExe) {
        Write-Host "Node.js successfully verified at: $nodeExe"
        $version = & $nodeExe -v
        Write-Host "Node version: $version"
    } else {
        Write-Warning "Could not find node.exe inside extracted directory."
    }
} else {
    Write-Warning "No directory found in extracted path."
}
