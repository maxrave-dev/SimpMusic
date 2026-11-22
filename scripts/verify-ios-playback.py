#!/usr/bin/env python3
"""Exercise the Debug iOS application's real AVPlayer and queue on a booted simulator."""
import argparse
import json
import math
from pathlib import Path
import struct
import subprocess
import sys
import time
import wave

parser = argparse.ArgumentParser()
parser.add_argument("--device", help="Simulator UDID; defaults to the first booted device")
parser.add_argument("--url", help="Optional remote audio URL; otherwise generate a quiet WAV fixture")
args = parser.parse_args()

def simctl(*arguments):
    return subprocess.check_output(["xcrun", "simctl", *arguments], text=True).strip()

if not args.device:
    inventory = json.loads(simctl("list", "devices", "booted", "--json"))
    args.device = next((device["udid"] for devices in inventory["devices"].values()
                        for device in devices if device["state"] == "Booted"), None)
if not args.device:
    parser.error("Boot an iOS simulator first")
container = Path(simctl("get_app_container", args.device, "com.maxrave.simpmusic", "data"))
report = container / "Documents/SimpMusic/ios-playback-smoke.txt"
report.unlink(missing_ok=True)
url = args.url
if not url:
    fixture = container / "Documents/ios-playback-fixture.wav"
    with wave.open(str(fixture), "wb") as audio:
        audio.setparams((1, 2, 22050, 0, "NONE", "not compressed"))
        audio.writeframes(b"".join(struct.pack("<h", int(900 * math.sin(2 * math.pi * 440 * i / 22050)))
                                  for i in range(22050 * 30)))
    url = fixture.as_uri()
simctl("launch", "--terminate-running-process", "--stdout=/tmp/simpmusic-smoke.log",
       "--stderr=/tmp/simpmusic-smoke-error.log", args.device, "com.maxrave.simpmusic",
       "--playback-smoke-test", url)
for _ in range(90):
    if report.exists():
        result = report.read_text()
        if "RESULT " in result:
            print(result, end="")
            sys.exit(0 if "RESULT PASS" in result else 1)
    time.sleep(1)
sys.exit("No playback report after 90 seconds; inspect the simulator's /tmp/simpmusic-smoke*.log")
