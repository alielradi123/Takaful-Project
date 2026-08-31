import os
import stat
import shutil
import subprocess
import time
import sys

print("=" * 50)
print("  Gradle Cache Fix - Final Solution")
print("=" * 50)

# Step 1: Stop Gradle daemon
print("\n[1/4] Stopping Gradle daemons...")
try:
    gradlew = r"C:\Users\hp\AndroidStudioProjects\Takaful\gradlew.bat"
    subprocess.run([gradlew, "--stop"], capture_output=True, timeout=30)
    print("  Gradle daemons stopped.")
except Exception as e:
    print(f"  Note: {e}")

# Step 2: Kill all Java processes
print("\n[2/4] Killing Java/Gradle processes...")
try:
    subprocess.run(["taskkill", "/F", "/IM", "java.exe", "/T"], capture_output=True)
    subprocess.run(["taskkill", "/F", "/IM", "gradle.exe", "/T"], capture_output=True)
    subprocess.run(["taskkill", "/F", "/IM", "javaw.exe", "/T"], capture_output=True)
    print("  Done. Waiting 3 seconds...")
    time.sleep(3)
except Exception as e:
    print(f"  Note: {e}")

# Helper: Force delete read-only files
def on_error(func, path, exc_info):
    try:
        os.chmod(path, stat.S_IWRITE | stat.S_IREAD | stat.S_IRWXU)
        func(path)
    except Exception as e:
        print(f"  Could not delete {path}: {e}")

# Step 3: Delete corrupted caches
print("\n[3/4] Cleaning corrupted Gradle caches...")

dirs_to_clean = [
    r"C:\Users\hp\.gradle\caches\8.13\transforms",
    r"C:\Users\hp\.gradle\caches\8.13\scripts",
    r"C:\Users\hp\.gradle\caches\modules-2\files-2.1",
    r"C:\Users\hp\.gradle\daemon",
]

for d in dirs_to_clean:
    if os.path.exists(d):
        print(f"  Removing: {d}")
        try:
            shutil.rmtree(d, onerror=on_error)
            print(f"  ✅ Deleted: {d}")
        except Exception as e:
            print(f"  ⚠️ Could not fully delete {d}: {e}")
            # Try alternative approach using robocopy trick
            try:
                empty_dir = r"C:\Users\hp\.gradle\__empty_temp__"
                os.makedirs(empty_dir, exist_ok=True)
                subprocess.run([
                    "robocopy", empty_dir, d,
                    "/MIR", "/NFL", "/NDL", "/NJH", "/NJS", "/NC", "/NS", "/NP"
                ], capture_output=True)
                shutil.rmtree(d, onerror=on_error)
                shutil.rmtree(empty_dir, ignore_errors=True)
                print(f"  ✅ Deleted via robocopy: {d}")
            except Exception as e2:
                print(f"  ❌ Failed: {e2}")
    else:
        print(f"  Already clean: {d}")

# Step 4: Clean project build cache
print("\n[4/4] Cleaning project build cache...")
build_dirs = [
    r"C:\Users\hp\AndroidStudioProjects\Takaful\.gradle",
    r"C:\Users\hp\AndroidStudioProjects\Takaful\app\build",
    r"C:\Users\hp\AndroidStudioProjects\Takaful\build",
]
for d in build_dirs:
    if os.path.exists(d):
        try:
            shutil.rmtree(d, onerror=on_error)
            print(f"  ✅ Cleaned: {d}")
        except Exception as e:
            print(f"  ⚠️ {d}: {e}")

print("\n" + "=" * 50)
print("  Cache cleanup complete!")
print("  Now open Android Studio and sync the project.")
print("=" * 50)
