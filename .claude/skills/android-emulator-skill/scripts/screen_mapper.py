#!/usr/bin/env python3
"""
Android Screen Mapper - Current Screen Analyzer

Maps the current screen's UI elements for navigation decisions.
"""

import argparse
import json
import os
import re
import sys
import tempfile
import xml.etree.ElementTree as ET
from common import resolve_serial, run_adb_command

class ScreenMapper:
    def __init__(self, serial=None):
        self.serial = serial
        self.temp_file = os.path.join(tempfile.gettempdir(), "window_dump.xml")

    def dump_ui(self):
        """Dump UI hierarchy to local file."""
        # Dump to device
        run_adb_command(["shell", "uiautomator", "dump", "/sdcard/window_dump.xml"], self.serial)
        # Pull to local
        run_adb_command(["pull", "/sdcard/window_dump.xml", self.temp_file], self.serial)

    def parse_bounds(self, bounds_str):
        """Parse bounds string '[x1,y1][x2,y2]' to {'x':, 'y':, 'width':, 'height':}"""
        match = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds_str)
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            return {
                "x": x1,
                "y": y1,
                "width": x2 - x1,
                "height": y2 - y1,
                "center_x": (x1 + x2) // 2,
                "center_y": (y1 + y2) // 2
            }
        return None

    def analyze(self):
        """Analyze the UI hierarchy."""
        self.dump_ui()
        
        if not os.path.exists(self.temp_file):
            return {"error": "Failed to dump UI"}

        tree = ET.parse(self.temp_file)
        root = tree.getroot()

        analysis = {
            "buttons": [],
            "text_fields": [],
            "interactive": [],
            "all_elements": []
        }

        def process_node(node):
            bounds = self.parse_bounds(node.get("bounds", ""))
            
            element = {
                "class": node.get("class", ""),
                "text": node.get("text", ""),
                "resource-id": node.get("resource-id", ""),
                "content-desc": node.get("content-desc", ""),
                "package": node.get("package", ""),
                "clickable": node.get("clickable") == "true",
                "enabled": node.get("enabled") == "true",
                "focused": node.get("focused") == "true",
                "scrollable": node.get("scrollable") == "true",
                "bounds": bounds
            }
            
            # Identify specific types
            if element["class"].endswith("Button") or element["clickable"]:
                label = element["text"] or element["content-desc"] or element["resource-id"]
                if label:
                    analysis["buttons"].append(label)
            
            if element["class"].endswith("EditText"):
                analysis["text_fields"].append(element)

            if element["clickable"] or element["scrollable"] or element["class"].endswith("EditText"):
                analysis["interactive"].append(element)
                
            analysis["all_elements"].append(element)

            for child in node:
                process_node(child)

        process_node(root)
        
        # Deduplicate buttons
        analysis["buttons"] = list(set(analysis["buttons"]))
        return analysis

    def format_summary(self, analysis):
        """Format analysis as text summary."""
        lines = []
        lines.append(f"Screen: {len(analysis['all_elements'])} elements ({len(analysis['interactive'])} interactive)")
        
        if analysis["buttons"]:
            buttons = analysis["buttons"][:5]
            lines.append(f"Buttons: {', '.join(buttons)}")
            if len(analysis["buttons"]) > 5:
                lines.append(f"  ... +{len(analysis['buttons']) - 5} more")

        if analysis["text_fields"]:
            lines.append(f"TextFields: {len(analysis['text_fields'])}")
            for tf in analysis["text_fields"]:
                lines.append(f"  - {tf.get('text') or tf.get('resource-id') or 'Unnamed'}")

        return "\n".join(lines)

def main():
    parser = argparse.ArgumentParser(description="Map Android UI elements")
    parser.add_argument("--json", action="store_true", help="Output JSON")
    parser.add_argument("--verbose", action="store_true", help="Detailed output")
    parser.add_argument("--serial", "-s", help="Device serial")
    
    args = parser.parse_args()
    
    try:
        serial = resolve_serial(args.serial)
    except RuntimeError as e:
        print(f"Error: {e}")
        sys.exit(1)

    mapper = ScreenMapper(serial)
    analysis = mapper.analyze()

    if args.json:
        print(json.dumps(analysis, indent=2))
    else:
        print(mapper.format_summary(analysis))

if __name__ == "__main__":
    main()
