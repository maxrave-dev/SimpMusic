#!/usr/bin/env python3
"""Build the opt-in iOS framework using the installed JDK 21, restoring daemon criteria."""
import os
import fcntl
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
criteria = root / "gradle/gradle-daemon-jvm.properties"
# Xcode and CLI builds share the daemon criteria; serialize this temporary override.
lock_path = root / ".gradle/ios-build.lock"
lock_path.parent.mkdir(parents=True, exist_ok=True)
lock = lock_path.open("w")
fcntl.flock(lock, fcntl.LOCK_EX)
original = criteria.read_bytes()
env = os.environ.copy()
if not env.get("JAVA_HOME"):
    bundled = Path("/Applications/Android Studio.app/Contents/jbr/Contents/Home")
    if bundled.exists():
        env["JAVA_HOME"] = str(bundled)
tasks = sys.argv[1:] or [":composeApp:linkDebugFrameworkIosSimulatorArm64"]
try:
    # The repository pins a vendor that may not be installed; use any local JDK 21.
    criteria.write_text("toolchainVersion=21\n")
    result = subprocess.run(
        [str(root / "gradlew"), "-PenableIos=true", *tasks,
         "--no-configuration-cache", "--console=plain"], cwd=root, env=env,
    )
finally:
    criteria.write_bytes(original)
sys.exit(result.returncode)
