# start-all.ps1
# يشغّل خادمَي HTTP:  لوحة التحكم (8000)  و  بوابة المتبرعين (8090)
# طريقة التشغيل:
#   powershell -ExecutionPolicy Bypass -File .\start-all.ps1

param(
    [int]$AdminPort = 8000,
    [int]$DonorPort = 8090
)

$WEB_ROOT = Split-Path -Parent $MyInvocation.MyCommand.Path

# ── جدول أنواع المحتوى ──────────────────────────────────
$MIME = @{
    ".html" = "text/html; charset=utf-8"
    ".css"  = "text/css; charset=utf-8"
    ".js"   = "application/javascript; charset=utf-8"
    ".json" = "application/json; charset=utf-8"
    ".png"  = "image/png"
    ".jpg"  = "image/jpeg"
    ".jpeg" = "image/jpeg"
    ".webp" = "image/webp"
    ".svg"  = "image/svg+xml"
    ".gif"  = "image/gif"
    ".ico"  = "image/x-icon"
    ".woff2"= "font/woff2"
    ".woff" = "font/woff"
}

# ── دالة تشغيل خادم TCP مبسّط ────────────────────────────
function Start-TcpServer {
    param([int]$Port, [string]$Root, [string]$Label)

    $tcp = [System.Net.Sockets.TcpListener]::new(
        [System.Net.IPAddress]::Loopback, $Port)
    $tcp.Server.SetSocketOption(
        [System.Net.Sockets.SocketOptionLevel]::Socket,
        [System.Net.Sockets.SocketOptionName]::ReuseAddress, $true)
    $tcp.Start()
    Write-Host "[$Label] يعمل على http://localhost:$Port/" -ForegroundColor Green

    while ($true) {
        try {
            $client = $tcp.AcceptTcpClient()
            $client.ReceiveTimeout = 2000
            $stream = $client.GetStream()

            # ── قراءة رأس الطلب سطراً سطراً ──
            $sr      = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::ASCII, $false, 4096, $true)
            $reqLine = $sr.ReadLine()
            if (-not $reqLine) { $client.Close(); continue }

            # تجاهل بقية الرؤوس
            while ($true) {
                $h = $sr.ReadLine()
                if ([string]::IsNullOrEmpty($h)) { break }
            }

            # ── تحليل المسار ──
            $parts  = $reqLine -split '\s+'
            $rawUrl = if ($parts.Count -ge 2) { $parts[1] } else { "/" }
            $url    = [System.Uri]::UnescapeDataString($rawUrl.Split('?')[0])
            if ($url -eq "/" -or $url -eq "") { $url = "/index.html" }
            $file   = Join-Path $Root ($url.Replace('/', '\').TrimStart('\'))

            # ── بناء الرد ──
            if (Test-Path $file -PathType Leaf) {
                $bytes = [System.IO.File]::ReadAllBytes($file)
                $ext   = [System.IO.Path]::GetExtension($file).ToLower()
                $ct    = if ($MIME.ContainsKey($ext)) { $MIME[$ext] } else { "application/octet-stream" }
                $hdr   = "HTTP/1.1 200 OK`r`nContent-Type: $ct`r`nContent-Length: $($bytes.Length)`r`nAccess-Control-Allow-Origin: *`r`nCache-Control: no-cache`r`nConnection: close`r`n`r`n"
                Write-Host "[$Label] 200 $url" -ForegroundColor DarkGray
            } else {
                $bytes = [System.Text.Encoding]::UTF8.GetBytes("404 Not Found: $url")
                $hdr   = "HTTP/1.1 404 Not Found`r`nContent-Type: text/plain; charset=utf-8`r`nContent-Length: $($bytes.Length)`r`nConnection: close`r`n`r`n"
                Write-Host "[$Label] 404 $url" -ForegroundColor Yellow
            }

            $sw = New-Object System.IO.BinaryWriter($stream)
            $sw.Write([System.Text.Encoding]::ASCII.GetBytes($hdr))
            $sw.Write($bytes)
            $sw.Flush()
        } catch { }
        finally { try { $client.Close() } catch { } }
    }
}

# ── تشغيل خادم بوابة المتبرعين في مهمة خلفية ─────────────
$mimeTable = $MIME
$donorJob = Start-Job -Name "DonorServer" -ScriptBlock {
    param($Port, $Root, $MimeIn)
    $MIME = $MimeIn

    $tcp = [System.Net.Sockets.TcpListener]::new(
        [System.Net.IPAddress]::Loopback, $Port)
    $tcp.Server.SetSocketOption(
        [System.Net.Sockets.SocketOptionLevel]::Socket,
        [System.Net.Sockets.SocketOptionName]::ReuseAddress, $true)
    $tcp.Start()

    while ($true) {
        try {
            $client = $tcp.AcceptTcpClient()
            $client.ReceiveTimeout = 2000
            $stream = $client.GetStream()
            $sr = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::ASCII, $false, 4096, $true)
            $reqLine = $sr.ReadLine()
            if (-not $reqLine) { $client.Close(); continue }
            while ($true) { $h = $sr.ReadLine(); if ([string]::IsNullOrEmpty($h)) { break } }
            $parts  = $reqLine -split '\s+'
            $rawUrl = if ($parts.Count -ge 2) { $parts[1] } else { "/" }
            $url    = [System.Uri]::UnescapeDataString($rawUrl.Split('?')[0])
            if ($url -eq "/" -or $url -eq "") { $url = "/index.html" }
            $file   = Join-Path $Root ($url.Replace('/', '\').TrimStart('\'))
            if (Test-Path $file -PathType Leaf) {
                $bytes = [System.IO.File]::ReadAllBytes($file)
                $ext   = [System.IO.Path]::GetExtension($file).ToLower()
                $ct    = if ($MIME.ContainsKey($ext)) { $MIME[$ext] } else { "application/octet-stream" }
                $hdr   = "HTTP/1.1 200 OK`r`nContent-Type: $ct`r`nContent-Length: $($bytes.Length)`r`nAccess-Control-Allow-Origin: *`r`nCache-Control: no-cache`r`nConnection: close`r`n`r`n"
            } else {
                $bytes = [System.Text.Encoding]::UTF8.GetBytes("404: $url")
                $hdr   = "HTTP/1.1 404 Not Found`r`nContent-Type: text/plain`r`nContent-Length: $($bytes.Length)`r`nConnection: close`r`n`r`n"
            }
            $sw = New-Object System.IO.BinaryWriter($stream)
            $sw.Write([System.Text.Encoding]::ASCII.GetBytes($hdr))
            $sw.Write($bytes)
            $sw.Flush()
        } catch { }
        finally { try { $client.Close() } catch { } }
    }
} -ArgumentList $DonorPort, $WEB_ROOT, $mimeTable

# ── انتظار قصير ثم فتح المتصفح ──────────────────────────
Start-Sleep -Milliseconds 800
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  تكافل — خوادم الويب" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  لوحة التحكم    : http://localhost:$AdminPort/index.html" -ForegroundColor Yellow
Write-Host "  بوابة المتبرعين: http://localhost:$DonorPort/donor/index.html" -ForegroundColor Yellow
Write-Host "  اضغط Ctrl+C للإيقاف" -ForegroundColor DarkGray
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

try {
    Start-Process "http://localhost:$AdminPort/index.html"
    Start-Sleep -Milliseconds 400
    Start-Process "http://localhost:$DonorPort/donor/index.html"
} catch {}

# ── تشغيل خادم لوحة التحكم في العملية الرئيسية ─────────────
Start-TcpServer -Port $AdminPort -Root $WEB_ROOT -Label "Admin"
