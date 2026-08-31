import os
import stat
import shutil
import time

def remove_readonly(func, path, excinfo):
    try:
        os.chmod(path, stat.S_IWRITE)
        func(path)
    except Exception as e:
        print(f"Failed to remove {path}: {e}")

cache_dir = r"C:\Users\hp\.gradle\caches\8.13\transforms"

print("Stopping gradle daemons...")
os.system(r"C:\Users\hp\AndroidStudioProjects\Takaful\gradlew.bat --stop")
time.sleep(2)

print("Killing java processes just in case...")
os.system("taskkill /F /IM java.exe /T")
time.sleep(2)

if os.path.exists(cache_dir):
    print(f"Removing {cache_dir}...")
    shutil.rmtree(cache_dir, onerror=remove_readonly)
    print("Done!")
else:
    print("Cache directory already removed.")

print("Cleaning project...")
os.system(r"C:\Users\hp\AndroidStudioProjects\Takaful\gradlew.bat clean")
print("Finished!")
