import os
import shutil

files_to_delete = [
    "BUILD_STATUS.md", "CACHE_FIXED.md", "FINAL_SOLUTION.md", "FIX_FIREBASE_MESSAGING.md",
    "FIX_GRADLE.vbs", "FIX_GRADLE_NOW.bat", "FIX_JDK.md", "README_QUICK_FIX.txt",
    "SOLVE_JDK_STEP_BY_STEP.md", "TODO.md", "fix_gradle_cache.bat", "fix_gradle_final.py",
    "clear_cache.py", "copy_new_qr.py", "copy_qr.bat", "copy_qr.py", "install.cmd",
    "setup-node.ps1", "generate-test-verifications.html", "hs_err_pid2892.log",
    "hs_err_pid7548.log", "replay_pid2892.log", "test-image.png"
]

web_files_to_delete = [
    "_diag.ps1", "_diag2.ps1", "_fix2.ps1", "fast_clean.ps1", "fix_image.ps1",
    "remove_bg.py", "check_error.js", "fix_supabase_images.html"
]

dirs_to_delete = [
    "web/node-env",
    "build"
]

root_dir = r"c:\Users\hp\AndroidStudioProjects\Takaful"

for f in files_to_delete:
    path = os.path.join(root_dir, f)
    if os.path.exists(path):
        try:
            os.chmod(path, 0o777)
            os.remove(path)
            print(f"Deleted: {f}")
        except Exception as e:
            print(f"Failed to delete {f}: {e}")

for f in web_files_to_delete:
    path = os.path.join(root_dir, "web", f)
    if os.path.exists(path):
        try:
            os.chmod(path, 0o777)
            os.remove(path)
            print(f"Deleted: web/{f}")
        except Exception as e:
            print(f"Failed to delete web/{f}: {e}")

for d in dirs_to_delete:
    path = os.path.join(root_dir, d)
    if os.path.exists(path):
        try:
            shutil.rmtree(path)
            print(f"Deleted dir: {d}")
        except Exception as e:
            print(f"Failed to delete dir {d}: {e}")
