$file = 'c:\Users\hp\AndroidStudioProjects\Takaful\web\dashboard.html'
$out  = 'c:\Users\hp\AndroidStudioProjects\Takaful\web\_fix_log.txt'
$content = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)

# Check what's at line 1054 area by finding the receipt pattern
"File length: $($content.Length)" | Out-File $out
"Contains paymentMethod: $($content.Contains('paymentMethod'))" | Out-File $out -Append
"Contains receiptUrl: $($content.Contains('receiptUrl'))" | Out-File $out -Append
"Contains bi-receipt: $($content.Contains('bi-receipt'))" | Out-File $out -Append

# Find position
$idx = $content.IndexOf('bi-receipt')
"bi-receipt index: $idx" | Out-File $out -Append
if ($idx -ge 0) {
    $ctx = $content.Substring([Math]::Max(0, $idx - 200), [Math]::Min(500, $content.Length - [Math]::Max(0,$idx-200)))
    "Context around bi-receipt:" | Out-File $out -Append
    $ctx | Out-File $out -Append
}

# === PERFORM THE FIX ===
# Old receipt cell
$oldCell = "                         {d.receiptUrl && <a href={d.receiptUrl} target=""_blank"" className=""badge badge-completed mt-1 text-decoration-none"" style={{fontSize:'0.6rem', marginTop:'4px'}}><i className=""bi bi-receipt me-1""></i> " + [char]0x0625 + [char]0x064a + [char]0x0635 + [char]0x0627 + [char]0x0644 + " " + [char]0x062a + [char]0x062d + [char]0x0648 + [char]0x064a + [char]0x0644 + "</a>}"

$found_old = $content.Contains($oldCell)
"Old cell found: $found_old" | Out-File $out -Append

# Try a simpler targeted replacement using regex
$newReceiptBlock = @'
                         {d.receiptUrl && (
                           <div style={{marginTop:4}}>
                             {String(d.receiptUrl).match(/\.(jpeg|jpg|gif|png|webp|bmp)(\?.*)?$/i) ? (
                               <a href={d.receiptUrl} target="_blank" rel="noopener noreferrer" title="عرض الإيصال الكامل">
                                 <img src={d.receiptUrl} alt="إيصال" style={{width:52,height:40,objectFit:'cover',borderRadius:6,border:'1px solid var(--border)',cursor:'zoom-in',display:'block'}} />
                               </a>
                             ) : (
                               <a href={d.receiptUrl} target="_blank" rel="noopener noreferrer" className="badge badge-completed text-decoration-none" style={{fontSize:'0.6rem',display:'inline-flex',alignItems:'center',gap:3}}>
                                 <i className="bi bi-receipt"></i>إيصال تحويل
                               </a>
                             )}
                           </div>
                         )}
                         {d.donorMessage && <div style={{fontSize:'0.65rem',color:'var(--gold)',marginTop:4}}><i className="bi bi-chat-quote-fill me-1"></i>"{d.donorMessage}"</div>}
'@

# Use regex to replace the old receipt + donorMessage block
$pattern = '\{d\.receiptUrl \&\& <a href=\{d\.receiptUrl\}[^\}]+\}\}[^\n]*\n\s*\{d\.donorMessage[^\n]*\n'
$newContent = [System.Text.RegularExpressions.Regex]::Replace($content, $pattern, $newReceiptBlock + "`n")

if ($newContent -ne $content) {
    [System.IO.File]::WriteAllText($file, $newContent, [System.Text.Encoding]::UTF8)
    "SUCCESS: File updated with new receipt display" | Out-File $out -Append
} else {
    "WARNING: Regex did not match, file not changed" | Out-File $out -Append
    # Dump the area around paymentMethod
    $pmIdx = $content.IndexOf('paymentMethod')
    if ($pmIdx -ge 0) {
        "PaymentMethod context:" | Out-File $out -Append
        $content.Substring([Math]::Max(0,$pmIdx-5), [Math]::Min(600, $content.Length - [Math]::Max(0,$pmIdx-5))) | Out-File $out -Append
    }
}
"DONE" | Out-File $out -Append
