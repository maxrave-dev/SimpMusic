#!/usr/bin/env python3
"""
Android Log Monitor - ADB Logcat Wrapper

Monitor device logs with filtering.
"""

import argparse
import sys
import subprocess
import signal
from common import resolve_serial, run_adb_command

def main():
    parser = argparse.ArgumentParser(description="Monitor Android Logs")
    parser.add_argument("--package", help="Filter by package name (requires app to be running)")
    parser.add_argument("--tag", help="Filter by tag")
    parser.add_argument("--priority", choices=["V", "D", "I", "W", "E", "F"], default="V", help="Minimum priority")
    parser.add_argument("--grep", help="Grep filter")
    parser.add_argument("--clear", "-c", action="store_true", help="Clear logs first")
    parser.add_argument("--serial", "-s", help="Device serial")

    args = parser.parse_args()

    try:
        serial = resolve_serial(args.serial)
    except RuntimeError as e:
        print(f"Error: {e}")
        sys.exit(1)

    if args.clear:
        run_adb_command(["logcat", "-c"], serial)
        print("Logs cleared.")

    cmd = ["logcat", "-v", "color", f"*:{args.priority}"]

    if args.tag:
        cmd = ["logcat", "-v", "color", "-s", args.tag]

    full_cmd = ["adb"]
    if serial:
        full_cmd.extend(["-s", serial])
    full_cmd.extend(cmd)

    if args.package:
        # Get PID of package
        try:
            res = run_adb_command(["shell", "pidof", args.package], serial, check=False)
            pid = res.stdout.strip()
            if pid:
                print(f"Filtering for package {args.package} (PID: {pid})")
                full_cmd.append(f"--pid={pid}")
            else:
                print(f"Package {args.package} not running. Showing all logs.")
        except Exception:
            pass
            
    if args.grep:
        full_cmd.extend(["|", "grep", args.grep])

    print(f"Running: {' '.join(full_cmd)}")
    try:
        # Use subprocess directly to stream
        process = subprocess.Popen(full_cmd, stdout=sys.stdout, stderr=sys.stderr)
        process.wait()
    except KeyboardInterrupt:
        sys.exit(0)

if __name__ == "__main__":
    main()
