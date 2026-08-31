param()
$file = 'c:\Users\hp\AndroidStudioProjects\Takaful\web\dashboard.html'
$logFile = 'c:\Users\hp\AndroidStudioProjects\Takaful\web\_fix_log.txt'

try {
    $content = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)

    # Check what we have
    Add-Content $logFile "=== START ==="
    Add-Content $logFile "File size: $($content.Length)"
    Add-Content $logFile "Has bi-receipt: $($content.Contains('bi-receipt'))"
    Add-Content $logFile "Has paymentMethod: $($content.Contains('paymentMethod'))"

    # The old content - lines 1052-1056 in the actual file
    # We know line 1054 contains the receipt link, line 1055 has donorMessage
    # Try to find by unique nearby text
    $marker = "fontSize:'.72rem',color:'var(--text-secondary)'}"
    $idx = $content.IndexOf($marker)
    Add-Content $logFile "Marker index: $idx"

    if ($idx -ge 0) {
        # Get context
        $ctx = $content.Substring($idx, [Math]::Min(600, $content.Length - $idx))
        Add-Content $logFile "Context after marker:"
        Add-Content $logFile $ctx
    }
} catch {
    Add-Content $logFile "ERROR: $_"
}
Add-Content $logFile "=== END ==="
