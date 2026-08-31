$file = 'c:\Users\hp\AndroidStudioProjects\Takaful\web\dashboard.html'
$out  = 'c:\Users\hp\AndroidStudioProjects\Takaful\web\_fix_log.txt'

$lines = [System.IO.File]::ReadAllLines($file, [System.Text.Encoding]::UTF8)

"Total lines: $($lines.Length)" | Out-File $out
"Line 1052: $($lines[1051])" | Out-File $out -Append
"Line 1053: $($lines[1052])" | Out-File $out -Append
"Line 1054: $($lines[1053])" | Out-File $out -Append
"Line 1055: $($lines[1054])" | Out-File $out -Append
"Line 1056: $($lines[1055])" | Out-File $out -Append

# Check if receiptUrl appears anywhere
$found = $lines | Where-Object { $_ -match 'receiptUrl' }
"Receipt lines found: $($found.Count)" | Out-File $out -Append
if ($found.Count -gt 0) {
    $found | ForEach-Object { "  >> $_" | Out-File $out -Append }
}

"DONE" | Out-File $out -Append
