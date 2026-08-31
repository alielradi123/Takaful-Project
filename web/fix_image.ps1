Add-Type -AssemblyName System.Drawing
try {
    $img = [System.Drawing.Image]::FromFile("c:\Users\hp\AndroidStudioProjects\Takaful\web\app-icon.webp")
    $bmp = New-Object System.Drawing.Bitmap($img)
    $width = $bmp.Width
    $height = $bmp.Height
    for ($x = 0; $x -lt $width; $x++) {
        for ($y = 0; $y -lt $height; $y++) {
            $pixel = $bmp.GetPixel($x, $y)
            if ($pixel.R -gt 230 -and $pixel.G -gt 230 -and $pixel.B -gt 230) {
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
            }
        }
    }
    $bmp.Save("c:\Users\hp\AndroidStudioProjects\Takaful\web\app-icon-transparent.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $img.Dispose()
    $bmp.Dispose()
    Write-Host "Success"
} catch {
    Write-Host "Error: $_"
}
