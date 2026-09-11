package org.windroid.core.input

/**
 * Mouse interaction modes for translating Android touch events to Windows input.
 */
enum class MouseMode(val displayName: String, val description: String) {
    RELATIVE_TRACKPAD(
        "Relative Trackpad",
        "Finger movement moves cursor with acceleration. Tap to left-click, two fingers to right-click."
    ),
    DIRECT_TOUCH(
        "Direct Touchscreen",
        "Cursor snaps directly to where finger touches screen. Ideal for strategy games and menus."
    ),
    VIRTUAL_JOYSTICK(
        "Virtual Gamepad Overlay",
        "On-screen D-pad, analog thumbsticks, and action buttons (ABXY, L1/R1)."
    ),
    EXTERNAL_CONTROLLER(
        "Physical Gamepad (HID/XInput)",
        "Pass-through Bluetooth or USB gamepad mapped to XInput."
    )
}

/**
 * On-screen touch control element definition.
 */
data class VirtualControlElement(
    val id: String,
    val type: String, // "BUTTON", "DPAD", "ANALOG_STICK"
    val label: String,
    val xPercent: Float, // 0.0f - 1.0f on screen
    val yPercent: Float,
    val sizeDp: Int,
    val windowsKeyCode: Int
)

/**
 * Controller configuration profile.
 */
data class InputProfile(
    val id: String,
    val name: String,
    val mouseMode: MouseMode = MouseMode.RELATIVE_TRACKPAD,
    val touchSensitivity: Float = 1.0f,
    val showVirtualOverlay: Boolean = false,
    val virtualControls: List<VirtualControlElement> = emptyList()
)
