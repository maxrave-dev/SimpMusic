---
name: android-emulator-skill
version: 1.0.0
description: Production-ready scripts for Android app testing, building, and automation. Provides semantic UI navigation, build automation, log monitoring, and emulator lifecycle management. Optimized for AI agents with minimal token output.
---

# Android Emulator Skill

Build, test, and automate Android applications using accessibility-driven navigation and structured data instead of pixel coordinates.

## Quick Start

```bash
# 1. Check environment
bash scripts/emu_health_check.sh

# 2. Launch app
python scripts/app_launcher.py --launch com.example.app

# 3. Map screen to see elements
python scripts/screen_mapper.py

# 4. Tap button
python scripts/navigator.py --find-text "Login" --tap

# 5. Enter text
python scripts/navigator.py --find-type EditText --enter-text "user@example.com"
```

All scripts support `--help` for detailed options and `--json` for machine-readable output.

## Production Scripts

### Build & Development

1. **build_and_test.py** - Build Android projects, run tests, parse results
   - Wrapper around Gradle
   - Support for assemble, install, and connectedCheck
   - Parse build errors and test results
   - Options: `--task`, `--clean`, `--json`

2. **log_monitor.py** - Real-time log monitoring with intelligent filtering
   - Wrapper around `adb logcat`
   - Filter by tag, priority, or PID
   - Deduplicate repeated messages
   - Options: `--package`, `--tag`, `--priority`, `--duration`, `--json`

### Navigation & Interaction

3. **screen_mapper.py** - Analyze current screen and list interactive elements
   - Dump UI hierarchy using `uiautomator`
   - Parse XML to identify buttons, text fields, etc.
   - Options: `--verbose`, `--json`

4. **navigator.py** - Find and interact with elements semantically
   - Find by text (fuzzy matching), resource-id, or class name
   - Interactive tapping and text entry
   - Options: `--find-text`, `--find-id`, `--tap`, `--enter-text`, `--json`

5. **gesture.py** - Perform swipes, scrolls, and other gestures
   - Swipe up/down/left/right
   - Scroll lists
   - Options: `--swipe`, `--scroll`, `--duration`, `--json`

6. **keyboard.py** - Key events and hardware buttons
   - Input key events (Home, Back, Enter, Tab)
   - Type text via ADB
   - Options: `--key`, `--text`, `--json`

7. **app_launcher.py** - App lifecycle management
   - Launch apps (`adb shell am start`)
   - Terminate apps (`adb shell am force-stop`)
   - Install/Uninstall APKs
   - List installed packages
   - Options: `--launch`, `--terminate`, `--install`, `--uninstall`, `--list`, `--json`

### Emulator Lifecycle Management

8. **emulator_manage.py** - Manage Android Virtual Devices (AVDs)
   - List available AVDs
   - Boot emulators
   - Shutdown emulators
   - Options: `--list`, `--boot`, `--shutdown`, `--json`

9. **emu_health_check.sh** - Verify environment is properly configured
    - Check ADB, Emulator, Java, Gradle, ANDROID_HOME
    - List connected devices

## Common Patterns

**Auto-Device Detection**: Scripts target the single connected device/emulator if only one is present, or require `-s <serial>` if multiple are connected.

**Output Formats**: Default is concise human-readable output. Use `--json` for machine-readable output.

## Requirements

- Android SDK Platform-Tools (adb, fastboot)
- Android Emulator
- Java / OpenJDK
- Python 3

## Key Design Principles

**Semantic Navigation**: Find elements by text, resource-id, or content-description.

**Token Efficiency**: Concise default output with optional verbose and JSON modes.

**Zero Configuration**: Works with standard Android SDK installation.
