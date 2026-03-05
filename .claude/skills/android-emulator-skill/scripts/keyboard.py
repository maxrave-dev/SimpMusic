#!/usr/bin/env python3
"""
Android Keyboard - Input text and key events

Type text and press hardware buttons.
"""

import argparse
import sys
import shlex
import subprocess
from common import resolve_serial, run_adb_command

KEYCODES = {
    "home": 3,
    "back": 4,
    "call": 5,
    "endcall": 6,
    "enter": 66,
    "tab": 61,
    "delete": 67,
    "power": 26,
    "camera": 27,
    "volume_up": 24,
    "volume_down": 25,
    "menu": 82,
    "search": 84,
}

def press_key(serial, key):
    keycode = KEYCODES.get(key.lower())
    if not keycode:
        # Try as integer
        try:
            keycode = int(key)
        except ValueError:
            print(f"Unknown key: {key}")
            return False
            
    try:
        run_adb_command(["shell", "input", "keyevent", str(keycode)], serial)
        return True
    except subprocess.CalledProcessError:
        return False

def type_text(serial, text):
    try:
        safe_text = shlex.quote(text).replace(" ", "%s")
        run_adb_command(["shell", "input", "text", safe_text], serial)
        return True
    except subprocess.CalledProcessError:
        return False

def main():
    parser = argparse.ArgumentParser(description="Android Keyboard Input")
    parser.add_argument("--key", help="Key to press (home, back, enter, tab, delete, or keycode)")
    parser.add_argument("--text", help="Text to type")
    parser.add_argument("--serial", "-s", help="Device serial")
    
    args = parser.parse_args()
    
    try:
        serial = resolve_serial(args.serial)
    except RuntimeError as e:
        print(f"Error: {e}")
        sys.exit(1)
        
    if args.key:
        if press_key(serial, args.key):
            print(f"Pressed {args.key}")
        else:
            sys.exit(1)
            
    elif args.text:
        if type_text(serial, args.text):
            print(f"Typed: {args.text}")
        else:
            sys.exit(1)
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
