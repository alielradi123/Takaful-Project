import shutil
import os

src = r"C:\Users\hp\.gemini\antigravity-ide\brain\481191d9-426e-40e9-963f-be69394c523e\media__1782456352953.jpg"
dsts = [
    r"c:\Users\hp\AndroidStudioProjects\Takaful\web\donor\bok-qr.jpg",
    r"c:\Users\hp\AndroidStudioProjects\Takaful\web\bok-qr.jpg",
    r"c:\Users\hp\AndroidStudioProjects\Takaful\app\src\main\res\drawable\bok_qr.jpg"
]

print("Starting copy script...")
if not os.path.exists(src):
    print(f"Source file does not exist: {src}")
else:
    print(f"Source file exists, size: {os.path.getsize(src)} bytes")
    for dst in dsts:
        try:
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copy2(src, dst)
            print(f"Successfully copied to {dst}")
        except Exception as e:
            print(f"Failed to copy to {dst}: {e}")
