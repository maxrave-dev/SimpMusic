---
name: android-accessibility
description: Expert checklist and prompts for auditing and fixing Android accessibility issues, especially in Jetpack Compose.
---

# Android Accessibility Checklist

## Instructions

Analyze the provided component or screen for the following accessibility aspects.

### 1. Content Descriptions
*   **Check**: Do `Image` and `Icon` composables have a meaningful `contentDescription`?
*   **Decorative**: If an image is purely decorative, use `contentDescription = null`.
*   **Actionable**: If an element is clickable, the description should describe the *action* (e.g., "Play music"), not the icon (e.g., "Triangle").

### 2. Touch Target Size
*   **Standard**: Minimum **48x48dp** for all interactive elements.
*   **Fix**: Use `MinTouchTargetSize` or wrap in `Box` with appropriate padding if the visual icon is smaller.

### 3. Color Contrast
*   **Standard**: WCAG AA requires **4.5:1** for normal text and **3.0:1** for large text/icons.
*   **Tool**: Verify colors against backgrounds using contrast logic.

### 4. Focus & Semantics
*   **Focus Order**: Ensure keyboard/screen-reader focus moves logically (e.g., Top-Start to Bottom-End).
*   **Grouping**: Use `Modifier.semantics(mergeDescendants = true)` for complex items (like a row with text and icon) so they are announced as a single item.
*   **State descriptions**: Use `stateDescription` to describe custom states (e.g., "Selected", "Checked") if standard semantics aren't enough.

### 5. Headings
*   **Traversal**: Mark title texts with `Modifier.semantics { heading() }` to allow screen reader users to jump between sections.

## Example Prompts for Agent Usage
*   "Analyze the content description of this Image. Is it appropriate?"
*   "Check if the touch target size of this button is at least 48dp."
*   "Does this custom toggle button report its 'Checked' state to TalkBack?"
