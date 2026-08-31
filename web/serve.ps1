# serve.ps1 — A simple PowerShell HTTP Server to host the static Takaful Web files
param(
    [int]$Port = 8000
)

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://localhost:$Port/")
try {
    $listener.Start()
    Write-Host "Server started on http://localhost:$Port/"
} catch {
    Write-Error "Failed to start listener on port $Port. Error: $_"
    exit 1
}

$webRoot = "c:\Users\hp\AndroidStudioProjects\Takaful\web"

# Clean up listener on script termination
$run = $true
[System.AppDomain]::CurrentDomain.add_ProcessExit({
    if ($listener.IsListening) {
        $listener.Stop()
        $listener.Close()
        Write-Host "Server stopped."
    }
})

while ($listener.IsListening) {
    try {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response

        $rawUrl = $request.RawUrl.Split('?')[0]
        # URL decode using System.Uri
        $rawUrl = [System.Uri]::UnescapeDataString($rawUrl)
        if ($rawUrl -eq "/" -or $rawUrl -eq "") { $rawUrl = "/index.html" }
        
        $filePath = Join-Path $webRoot $rawUrl.Replace('/', '\').TrimStart('\')
        
        if (Test-Path $filePath -PathType Leaf) {
            $bytes = [System.IO.File]::ReadAllBytes($filePath)
            
            # Determine content type
            $ext = [System.IO.Path]::GetExtension($filePath).ToLower()
            $contentType = "text/plain"
            switch ($ext) {
                ".html" { $contentType = "text/html; charset=utf-8" }
                ".css"  { $contentType = "text/css; charset=utf-8" }
                ".js"   { $contentType = "application/javascript; charset=utf-8" }
                ".png"  { $contentType = "image/png" }
                ".jpg"  { $contentType = "image/jpeg" }
                ".jpeg" { $contentType = "image/jpeg" }
                ".svg"  { $contentType = "image/svg+xml" }
                ".json" { $contentType = "application/json; charset=utf-8" }
                ".webp" { $contentType = "image/webp" }
                ".woff2" { $contentType = "font/woff2" }
                ".gif"  { $contentType = "image/gif" }
            }
            
            # Add CORS headers if needed
            $response.Headers.Add("Access-Control-Allow-Origin", "*")
            $response.ContentType = $contentType
            $response.ContentLength64 = $bytes.Length
            $response.OutputStream.Write($bytes, 0, $bytes.Length)
        } else {
            $response.StatusCode = 404
            $errBytes = [System.Text.Encoding]::UTF8.GetBytes("404 Not Found: $rawUrl")
            $response.OutputStream.Write($errBytes, 0, $errBytes.Length)
        }
        $response.Close()
    } catch {
        Write-Host "Error processing request: $_"
    }
}
