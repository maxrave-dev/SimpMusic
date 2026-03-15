#!/usr/bin/env python3
"""
Android Gesture - Swipe and Scroll

Perform gestures on the Android device.
"""

import argparse
import sys
import subprocess
from common import resolve_serial, run_adb_command, get_device_screen_size as get_size

def perform_swipe(serial, direction, duration=300):
    """
    Perform checks logic:
    - Up: swipe from bottom to top
    - Down: swipe from top to bottom
    - Left: swipe from right to left
    - Right: swipe from left to right
    """
    width, height = get_size(serial)
    
    # Safe margins (10%)
    w_min, w_max = int(width * 0.1), int(width * 0.9)
    h_min, h_max = int(height * 0.1), int(height * 0.9)
    
    # Centers
    cx = width // 2
    cy = height // 2
    
    start_x, start_y, end_x, end_y = 0, 0, 0, 0
    
    if direction == "up":
        start_x, start_y = cx, h_max
        end_x, end_y = cx, h_min
    elif direction == "down":
        start_x, start_y = cx, h_min
        end_x, end_y = cx, h_max
    elif direction == "left":
        start_x, start_y = w_max, cy
        end_x, end_y = w_min, cy
    elif direction == "right":
        start_x, start_y = w_min, cy
        end_x, end_y = w_max, cy
        
    cmd = ["shell", "input", "swipe", str(start_x), str(start_y), str(end_x), str(end_y), str(duration)]
    
    try:
        run_adb_command(cmd, serial)
        print(f"Swiped {direction}")
    except subprocess.CalledProcessError:
        print(f"Failed to swipe {direction}")

def main():
    parser = argparse.ArgumentParser(description="Perform gestures on Android")
    parser.add_argument("--swipe", choices=["up", "down", "left", "right"], help="Swipe direction")
    parser.add_argument("--scroll", choices=["up", "down", "left", "right"], help="Scroll direction (same as swipe but inverse logic usually, but here mapped 1:1 to swipe direction)")
    parser.add_argument("--duration", type=int, default=300, help="Duration in ms")
    parser.add_argument("--serial", "-s", help="Device serial")
    
    args = parser.parse_args()
    
    try:
        serial = resolve_serial(args.serial)
    except RuntimeError as e:
        print(f"Error: {e}")
        sys.exit(1)
        
    if args.swipe:
        perform_swipe(serial, args.swipe, args.duration)
    elif args.scroll:
        # Scroll down content usually means swiping up finger, but 'scroll down' command usually implies moving content down (swiping down)
        # We'll just map scroll to swipe for now to keep it simple
        perform_swipe(serial, args.scroll, args.duration)
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
