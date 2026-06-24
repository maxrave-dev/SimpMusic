#!/usr/bin/env python3
"""
Android Build & Test - Gradle Wrapper

Builds projects and runs tests with parsed output.
"""

import argparse
import sys
import subprocess
import os

def find_gradlew():
    """Find gradlew in current or parent directories."""
    cwd = os.getcwd()
    while cwd != "/":
        path = os.path.join(cwd, "gradlew")
        if os.path.exists(path):
            return path
        cwd = os.path.dirname(cwd)
    return None

def run_gradle_task(task, clean=False, verbose=False):
    gradlew = find_gradlew()
    if not gradlew:
        print("Error: gradlew not found in current directory tree.")
        return False

    cmd = [gradlew, task]
    if clean:
        cmd.insert(1, "clean")
    
    if not verbose:
        cmd.append("-q") # Quiet mode

    print(f"Running: {' '.join(cmd)}")
    
    try:
        process = subprocess.Popen(
            cmd, 
            stdout=subprocess.PIPE, 
            stderr=subprocess.STDOUT,
            text=True
        )
        
        # Stream output
        output_lines = []
        for line in process.stdout:
            output_lines.append(line)
            if verbose:
                print(line, end="")
        
        process.wait()
        
        if process.returncode == 0:
            print(f"✅ Build Successful: {task}")
            return True
        else:
            print(f"❌ Build Failed: {task}")
            # Print last 20 lines of error if not verbose
            if not verbose:
                print("Error details (last 20 lines):")
                print("".join(output_lines[-20:]))
            return False

    except Exception as e:
        print(f"Error running gradle: {e}")
        return False

def main():
    parser = argparse.ArgumentParser(description="Build and Test Android Project")
    parser.add_argument("--task", default="assembleDebug", help="Gradle task to run")
    parser.add_argument("--test", action="store_true", help="Run connectedAndroidTest")
    parser.add_argument("--clean", action="store_true", help="Run clean before task")
    parser.add_argument("--verbose", action="store_true", help="Show full gradle output")
    parser.add_argument("--json", action="store_true", help="Output JSON (TODO)")

    args = parser.parse_args()

    task = args.task
    if args.test:
        task = "connectedAndroidTest"

    if run_gradle_task(task, args.clean, args.verbose):
        sys.exit(0)
    else:
        sys.exit(1)

if __name__ == "__main__":
    main()
