#!/usr/bin/env python3
"""
Android App Launcher - App Lifecycle Control

Launches, terminates, and manages Android apps on the emulator/device.
"""

import argparse
import sys
import time
import subprocess
from common import resolve_serial, run_adb_command

class AppLauncher:
    """Controls app lifecycle on Android."""

    def __init__(self, serial: str = None):
        self.serial = serial

    def launch(self, package: str, activity: str = None) -> bool:
        """
        Launch an app.
        If activity is provided, uses explicitly.
        If not, tries to launch main activity via monkey (more robust than guessing).
        """
        if activity:
            cmd = ["shell", "am", "start", "-n", f"{package}/{activity}"]
        else:
             # Use monkey to launch the main activity of the package
            cmd = ["shell", "monkey", "-p", package, "-c", "android.intent.category.LAUNCHER", "1"]

        try:
            run_adb_command(cmd, self.serial)
            return True
        except subprocess.CalledProcessError as e:
            print(f"Error launching app: {e}")
            return False

    def terminate(self, package: str) -> bool:
        """Terminate an app."""
        try:
            run_adb_command(["shell", "am", "force-stop", package], self.serial)
            return True
        except subprocess.CalledProcessError:
            return False

    def install(self, apk_path: str) -> bool:
        """Install an APK."""
        try:
            run_adb_command(["install", "-r", apk_path], self.serial)
            return True
        except subprocess.CalledProcessError as e:
            print(f"Error installing APK: {e}")
            return False

    def uninstall(self, package: str) -> bool:
        """Uninstall an app."""
        try:
            run_adb_command(["uninstall", package], self.serial)
            return True
        except subprocess.CalledProcessError:
            return False

    def list_packages(self, filter_str: str = None) -> list[str]:
        """List installed packages."""
        try:
            cmd = ["shell", "pm", "list", "packages"]
            if filter_str:
                cmd.append(filter_str)
            
            result = run_adb_command(cmd, self.serial)
            packages = []
            for line in result.stdout.splitlines():
                if line.startswith("package:"):
                    packages.append(line.replace("package:", "").strip())
            return packages
        except subprocess.CalledProcessError:
            return []

    def get_app_state(self, package: str) -> str:
        """Get app state (running or not running)."""
        try:
            # Check if process exists
            result = run_adb_command(["shell", "pidof", package], self.serial, check=False)
            if result.returncode == 0 and result.stdout.strip():
                return "running"
            return "not running"
        except Exception:
            return "unknown"

def main():
    parser = argparse.ArgumentParser(description="Control Android app lifecycle")
    
    # Actions
    parser.add_argument("--launch", help="Launch app by package name")
    parser.add_argument("--activity", help="Specific activity to launch (optional)")
    parser.add_argument("--terminate", help="Terminate app by package name")
    parser.add_argument("--install", help="Install app from APK path")
    parser.add_argument("--uninstall", help="Uninstall app by package name")
    parser.add_argument("--list", action="store_true", help="List installed packages")
    parser.add_argument("--state", help="Get app state by package name")
    
    # Options
    parser.add_argument("--serial", "-s", help="Device serial (optional)")
    parser.add_argument("--json", action="store_true", help="Output JSON (TODO)")

    args = parser.parse_args()

    try:
        serial = resolve_serial(args.serial)
    except RuntimeError as e:
        print(f"Error: {e}")
        sys.exit(1)

    launcher = AppLauncher(serial)

    if args.launch:
        if launcher.launch(args.launch, args.activity):
            print(f"Launched {args.launch}")
        else:
            sys.exit(1)

    elif args.terminate:
        if launcher.terminate(args.terminate):
            print(f"Terminated {args.terminate}")
        else:
            sys.exit(1)

    elif args.install:
        if launcher.install(args.install):
            print(f"Installed {args.install}")
        else:
            sys.exit(1)

    elif args.uninstall:
        if launcher.uninstall(args.uninstall):
            print(f"Uninstalled {args.uninstall}")
        else:
            sys.exit(1)

    elif args.list:
        packages = launcher.list_packages()
        for pkg in packages:
            print(pkg)
            
    elif args.state:
        print(f"{args.state}: {launcher.get_app_state(args.state)}")

    else:
        parser.print_help()

if __name__ == "__main__":
    main()
