# donor-serve.ps1 — Minimal HTTP Server for Takaful Donor Portal
param([int]$Port = 8090)

$webRoot = "c:\Users\hp\AndroidStudioProjects\Takaful\web"

try {
    $listener = New-Object System.Net.HttpListener
    $prefix = "http://localhost:$Port/"
    $listener.Prefixes.Add($prefix)
    $listener.Start()
    Write-Host "Server started: $prefix"
    Write-Host "Donor portal: ${prefix}donor/index.html"
} catch {
    Write-Error "Failed to start: $_"
    exit 1
}

while ($listener.IsListening) {
    try {
        $ctx = $listener.GetContext()
        $rawUrl = [System.Uri]::UnescapeDataString($ctx.Request.RawUrl.Split('?')[0])
        if ($rawUrl -eq "/" -or $rawUrl -eq "") { $rawUrl = "/index.html" }

        $filePath = Join-Path $webRoot ($rawUrl.Replace('/', '\').TrimStart('\'))

        if (Test-Path $filePath -PathType Leaf) {
            $bytes = [System.IO.File]::ReadAllBytes($filePath)
            $ext = [System.IO.Path]::GetExtension($filePath).ToLower()

            $contentType = "application/octet-stream"
            switch ($ext) {
                ".html" { $contentType = "text/html; charset=utf-8" }
                ".css"  { $contentType = "text/css; charset=utf-8" }
                ".js"   { $contentType = "application/javascript; charset=utf-8" }
                ".png"  { $contentType = "image/png" }
                ".jpg"  { $contentType = "image/jpeg" }
                ".jpeg" { $contentType = "image/jpeg" }
                ".webp" { $contentType = "image/webp" }
                ".svg"  { $contentType = "image/svg+xml" }
                ".json" { $contentType = "application/json; charset=utf-8" }
                ".gif"  { $contentType = "image/gif" }
                ".woff2" { $contentType = "font/woff2" }
            }

            $ctx.Response.Headers.Add("Access-Control-Allow-Origin", "*")
            $ctx.Response.ContentType = $contentType
            $ctx.Response.ContentLength64 = $bytes.Length
            $ctx.Response.OutputStream.Write($bytes, 0, $bytes.Length)
            Write-Host "200 $rawUrl"
        } else {
            $ctx.Response.StatusCode = 404
            $errBytes = [System.Text.Encoding]::UTF8.GetBytes("404 Not Found: $rawUrl")
            $ctx.Response.OutputStream.Write($errBytes, 0, $errBytes.Length)
            Write-Host "404 $rawUrl"
        }
        $ctx.Response.Close()
    } catch {
        Write-Host "Error: $_"
    }
}
