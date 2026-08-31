Set oShell = CreateObject("WScript.Shell")
Set oFS = CreateObject("Scripting.FileSystemObject")

WScript.Echo "=== Gradle Cache Fix ==="

' Kill Java processes
oShell.Run "taskkill /F /IM java.exe /T", 0, True
oShell.Run "taskkill /F /IM javaw.exe /T", 0, True

WScript.Sleep 2000

' Get paths
Dim userProfile
userProfile = oShell.ExpandEnvironmentStrings("%USERPROFILE%")

Dim transformsPath
transformsPath = userProfile & "\.gradle\caches\8.13\transforms"

Dim daemonPath
daemonPath = userProfile & "\.gradle\daemon"

' Delete transforms
If oFS.FolderExists(transformsPath) Then
    WScript.Echo "Deleting: " & transformsPath
    On Error Resume Next
    oFS.DeleteFolder transformsPath, True
    If Err.Number <> 0 Then
        WScript.Echo "Warning: " & Err.Description
        Err.Clear
    Else
        WScript.Echo "Done!"
    End If
    On Error GoTo 0
Else
    WScript.Echo "Already clean: " & transformsPath
End If

' Delete daemon
If oFS.FolderExists(daemonPath) Then
    WScript.Echo "Deleting daemon: " & daemonPath
    On Error Resume Next
    oFS.DeleteFolder daemonPath, True
    On Error GoTo 0
End If

' Delete project caches
Dim projectPath
projectPath = "C:\Users\hp\AndroidStudioProjects\Takaful"

Dim projectGradle
projectGradle = projectPath & "\.gradle"
If oFS.FolderExists(projectGradle) Then
    On Error Resume Next
    oFS.DeleteFolder projectGradle, True
    On Error GoTo 0
End If

Dim appBuild
appBuild = projectPath & "\app\build"
If oFS.FolderExists(appBuild) Then
    On Error Resume Next
    oFS.DeleteFolder appBuild, True
    On Error GoTo 0
End If

WScript.Echo ""
WScript.Echo "=============================="
WScript.Echo "DONE! Now:"
WScript.Echo "1. Open Android Studio"
WScript.Echo "2. File > Sync Project with Gradle Files"
WScript.Echo "=============================="
WScript.Echo ""
MsgBox "Gradle cache cleaned successfully!" & vbCrLf & vbCrLf & "Now open Android Studio and sync the project.", vbInformation, "Fix Complete"
