#!/usr/bin/env python3
"""
Android Navigator - Smart Element Finder and Interactor

Finds and interacts with UI elements using accessibility data.
"""

import argparse
import sys
import shlex
import subprocess
from common import resolve_serial, run_adb_command
from screen_mapper import ScreenMapper

class Navigator:
    def __init__(self, serial=None):
        self.serial = serial
        self.mapper = ScreenMapper(serial)

    def find_element(self, text=None, resource_id=None, element_class=None, index=0):
        """Find element in current screen hierarchy."""
        analysis = self.mapper.analyze()
        if "error" in analysis:
            return None

        candidates = []
        for elem in analysis["all_elements"]:
            match = True
            if text:
                # Fuzzy match text or content-desc
                elem_text = (elem.get("text") or "").lower()
                elem_desc = (elem.get("content-desc") or "").lower()
                search = text.lower()
                if search not in elem_text and search not in elem_desc:
                    match = False
            
            if resource_id and resource_id not in elem.get("resource-id", ""):
                match = False
                
            if element_class and element_class not in elem.get("class", ""):
                match = False
            
            if match:
                candidates.append(elem)

        if index < len(candidates):
            return candidates[index]
        return None

    def tap(self, x, y):
        """Tap at coordinates."""
        try:
            run_adb_command(["shell", "input", "tap", str(x), str(y)], self.serial)
            return True
        except subprocess.CalledProcessError:
            return False

    def enter_text(self, text):
        """Enter text (escaped)."""
        try:
            # Escape text for shell
            safe_text = shlex.quote(text).replace(" ", "%s")
            run_adb_command(["shell", "input", "text", safe_text], self.serial)
            return True
        except subprocess.CalledProcessError:
            return False

def main():
    parser = argparse.ArgumentParser(description="Navigate Android apps")
    
    # Finding options
    parser.add_argument("--find-text", help="Find element by text (fuzzy)")
    parser.add_argument("--find-id", help="Find element by resource-id")
    parser.add_argument("--find-class", help="Find element by class name")
    parser.add_argument("--index", type=int, default=0, help="Index of match")
    
    # Action options
    parser.add_argument("--tap", action="store_true", help="Tap the found element")
    parser.add_argument("--enter-text", help="Enter text into found element")
    parser.add_argument("--tap-at", help="Tap at coords x,y")
    
    parser.add_argument("--serial", "-s", help="Device serial")

    args = parser.parse_args()

    try:
        serial = resolve_serial(args.serial)
    except RuntimeError as e:
        print(f"Error: {e}")
        sys.exit(1)

    navigator = Navigator(serial)

    # Tap at coordinates
    if args.tap_at:
        x, y = map(int, args.tap_at.split(","))
        if navigator.tap(x, y):
            print(f"Tapped at {x},{y}")
        else:
            sys.exit(1)
        return

    # Find element
    if args.find_text or args.find_id or args.find_class:
        element = navigator.find_element(
            text=args.find_text, 
            resource_id=args.find_id, 
            element_class=args.find_class, 
            index=args.index
        )

        if not element:
            print("Element not found")
            sys.exit(1)

        print(f"Found: {element.get('class')} '{element.get('text')}' at {element.get('bounds')}")

        if args.tap:
            bounds = element.get("bounds")
            if bounds:
                cx, cy = bounds["center_x"], bounds["center_y"]
                if navigator.tap(cx, cy):
                    print(f"Tapped at {cx},{cy}")
                else:
                    print("Failed to tap")
                    sys.exit(1)
            else:
                print("Element has no bounds")
                sys.exit(1)

        if args.enter_text:
            # Tap first to focus if needed (optional, but good practice)
            if args.tap:
                time.sleep(0.5)
            
            if navigator.enter_text(args.enter_text):
                print(f"Entered text: {args.enter_text}")
            else:
                print("Failed to enter text")
                sys.exit(1)

if __name__ == "__main__":
    main()
