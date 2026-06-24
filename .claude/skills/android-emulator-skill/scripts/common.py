#!/usr/bin/env python3
"""
Common utilities for Android Emulator Skill scripts.
Handles ADB command execution and device resolution.
"""

import os
import subprocess
import sys
from typing import List, Optional, Tuple

def get_adb_path() -> str:
    """Get the path to the adb executable."""
    # Check environment variable first
    android_home = os.environ.get("ANDROID_HOME")
    if android_home:
        adb_path = os.path.join(android_home, "platform-tools", "adb")
        if os.path.exists(adb_path):
            return adb_path
    
    # Check if adb is in PATH
    try:
        subprocess.run(["adb", "--version"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
        return "adb"
    except (subprocess.CalledProcessError, FileNotFoundError):
        pass

    # Standard locations
    home = os.path.expanduser("~")
    possible_paths = [
        os.path.join(home, "Library/Android/sdk/platform-tools/adb"),
        os.path.join(home, "Android/Sdk/platform-tools/adb"),
    ]
    
    for path in possible_paths:
        if os.path.exists(path):
            return path
            
    return "adb"  # Hope for the best

ADB_PATH = get_adb_path()

def run_adb_command(cmd: List[str], serial: Optional[str] = None, check: bool = True) -> subprocess.CompletedProcess:
    """
    Run an ADB command.
    
    Args:
        cmd: List of command arguments (e.g. ["shell", "ls"])
        serial: Optional device serial number
        check: Whether to raise an exception on failure
        
    Returns:
        CompletedProcess object
    """
    full_cmd = [ADB_PATH]
    if serial:
        full_cmd.extend(["-s", serial])
    full_cmd.extend(cmd)
    
    return subprocess.run(full_cmd, capture_output=True, text=True, check=check)

def get_connected_devices() -> List[str]:
    """Get a list of connected device serials."""
    result = run_adb_command(["devices"])
    devices = []
    # Skip first line (List of devices attached)
    lines = result.stdout.strip().splitlines()[1:]
    for line in lines:
        if not line.strip():
            continue
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            devices.append(parts[0])
    return devices

def resolve_serial(serial: Optional[str] = None) -> str:
    """
    Resolve the device serial to use.
    
    If serial is provided, verifies it exists.
    If not provided:
        - If 1 device connected, returns it.
        - If multiple, raises RuntimeError.
        - If none, raises RuntimeError.
    """
    devices = get_connected_devices()
    
    if serial:
        if serial not in devices:
            raise RuntimeError(f"Device '{serial}' not found or not connected.")
        return serial
        
    if not devices:
        raise RuntimeError("No Android devices connected or emulators running.")
        
    if len(devices) == 1:
        return devices[0]
        
    raise RuntimeError(f"Multiple devices connected: {', '.join(devices)}. Please specify one with --serial.")

def get_screen_size(serial: str) -> Tuple[int, int]:
    """Get screen width and height in pixels."""
    result = run_adb_command(["shell", "wm", "size"], serial=serial)
    # Output: Physical size: 1080x2400
    try:
        if result.stdout:
            line = result.stdout.strip().splitlines()[0]
            if "Physical size:" in line:
                size_str = line.split(":")[-1].strip()
                width, height = map(int, size_str.split("x"))
                return width, height
    except Exception:
        pass
    return (1080, 1920) # Default fallback
