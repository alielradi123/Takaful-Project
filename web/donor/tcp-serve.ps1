# tcp-serve.ps1 — Minimal TCP-based HTTP Server (no admin needed)
param([int]$Port = 8090)

$webRoot = "c:\Users\hp\AndroidStudioProjects\Takaful\web"

$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, $Port)
$listener.Start()
Write-Host "=== SERVER READY ==="
Write-Host "  http://localhost:$Port/"
Write-Host "  http://localhost:$Port/donor/index.html"
Write-Host "=== Press Ctrl+C to stop ==="

function Get-ContentType($ext) {
    switch ($ext) {
        ".html" { "text/html; charset=utf-8" }
        ".css"  { "text/css; charset=utf-8" }
        ".js"   { "application/javascript; charset=utf-8" }
        ".png"  { "image/png" }
        ".jpg"  { "image/jpeg" }
        ".jpeg" { "image/jpeg" }
        ".webp" { "image/webp" }
        ".svg"  { "image/svg+xml" }
        ".json" { "application/json; charset=utf-8" }
        ".gif"  { "image/gif" }
        ".ico"  { "image/x-icon" }
        ".woff2" { "font/woff2" }
        default { "application/octet-stream" }
    }
}

while ($true) {
    try {
        $client = $listener.AcceptTcpClient()
        $stream = $client.GetStream()
        $reader = New-Object System.IO.StreamReader($stream)
        
        # Read request line
        $requestLine = $reader.ReadLine()
        if (-not $requestLine) { $client.Close(); continue }
        
        # Read headers (discard)
        while ($true) {
            $line = $reader.ReadLine()
            if ([string]::IsNullOrEmpty($line)) { break }
        }

        # Parse URL
        $parts = $requestLine -split '\s+'
        $method = $parts[0]
        $rawUrl = $parts[1]
        $url = [System.Uri]::UnescapeDataString($rawUrl.Split('?')[0])
        if ($url -eq "/" -or $url -eq "") { $url = "/index.html" }

        $filePath = Join-Path $webRoot ($url.Replace('/', '\').TrimStart('\'))

        if (Test-Path $filePath -PathType Leaf) {
            $bytes = [System.IO.File]::ReadAllBytes($filePath)
            $ext = [System.IO.Path]::GetExtension($filePath).ToLower()
            $ct = Get-ContentType $ext

            $responseHeader = "HTTP/1.1 200 OK`r`nContent-Type: $ct`r`nContent-Length: $($bytes.Length)`r`nAccess-Control-Allow-Origin: *`r`nConnection: close`r`n`r`n"
            $headerBytes = [System.Text.Encoding]::UTF8.GetBytes($responseHeader)
            $stream.Write($headerBytes, 0, $headerBytes.Length)
            $stream.Write($bytes, 0, $bytes.Length)
            Write-Host "200 $url ($ct)"
        } else {
            $body = "404 Not Found: $url"
            $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($body)
            $responseHeader = "HTTP/1.1 404 Not Found`r`nContent-Type: text/plain; charset=utf-8`r`nContent-Length: $($bodyBytes.Length)`r`nConnection: close`r`n`r`n"
            $headerBytes = [System.Text.Encoding]::UTF8.GetBytes($responseHeader)
            $stream.Write($headerBytes, 0, $headerBytes.Length)
            $stream.Write($bodyBytes, 0, $bodyBytes.Length)
            Write-Host "404 $url -> $filePath"
        }

        $stream.Flush()
        $client.Close()
    } catch {
        Write-Host "Error: $_"
        if ($client) { $client.Close() }
    }
}
