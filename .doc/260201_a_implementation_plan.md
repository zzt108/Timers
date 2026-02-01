# Goal Description

The user wants the "disable dialog" (AlarmActivity) to pop up automatically when a "stoppable timer" (alarming timer) expires. Currently, they feel they have to dig into notifications to find it.

This plan addresses this by:

1. Ensuring the alarm notification is explicitly clickable, taking the user directly to the `AlarmActivity`.
2. Reinforcing the "pop up" behavior by ensuring the `ContentIntent` matches the `FullScreenIntent`, which provides a fallback if the full-screen interruption is suppressed by the OS (common on modern Android).

# User Review Required
>
> [!NOTE]
> On Android 10+ (Q+), apps cannot start activities from the background (pop up) while the user is using another app, unless they grant specific permissions or usage rights. The "Heads-up" notification is the standard behavior. Adding `ContentIntent` ensures that *clicking* that notification immediately opens the dialog.

# Proposed Changes

### [MODIFY] [TimerNotificationHelper.kt](file:///c:/Git/Timers/app/src/main/java/com/pneumasoft/multitimer/services/TimerNotificationHelper.kt)

- In `showTimerCompletionNotification`:
  - Add `.setContentIntent(fullScreenPendingIntent)` to the notification builder.
  - This ensures that tapping the notification body (not just buttons) opens the `AlarmActivity`.

### [MODIFY] [TimerService.kt](file:///c:/Git/Timers/app/src/main/java/com/pneumasoft/multitimer/services/TimerService.kt)

- In `handleTimerCompletedIntent`:
  - **Check `Settings.canDrawOverlays(this)` first.**
  - If permission granted: Explicitly launch `AlarmActivity` using `startActivity`.
    - Use flags `Intent.FLAG_ACTIVITY_NEW_TASK` and `Intent.FLAG_ACTIVITY_CLEAR_TOP`.
  - This works alongside the notification to ensure the dialog appears.

## [app/src/main]

### [MODIFY] [AndroidManifest.xml](file:///c:/Git/Timers/app/src/main/AndroidManifest.xml)

- Add `<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />`
- Update `AlarmActivity` declaration:
  - Verify `android:launchMode="singleInstance"` is present (previously added).

## [app/src/main/java/com/pneumasoft/multitimer]

### [MODIFY] [MainActivity.kt](file:///c:/Git/Timers/app/src/main/java/com/pneumasoft/multitimer/MainActivity.kt)

- Add `checkAndRequestOverlayPermission()`
  - Check `Settings.canDrawOverlays(this)`.
  - If false, show dialog and redirect to `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`.
- Add `checkAndRequestFullScreenIntentPermission()` similar to existing permission checks.
  - Targeting Android 14+ (SDK 34).
  - Use `NotificationManager.canUseFullScreenIntent()`.
  - Show an explanation dialog and redirect to `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` if needed.
- Call these checks in `onCreate` or `onStart`.

# Verification Plan

### Manual Verification

1. Build and install the app on an Android 14+ device (if possible) or emulator.
2. **Permission Check:** Verify that on first launch (or when appropriate), the app asks for "Full screen notifications" permission if not granted.
3. **Set a timer for 10 seconds.**
4. **Screen Locked Test:** Lock the screen. Wait for timer. Verify that `AlarmActivity` appears over the lock screen (original "pop up" behavior).
5. **Screen Unlocked Test:** Unlock screen and use another app. Wait for timer.
    - Verify a high-priority "Heads-up" notification appears.
    - **Tap the notification body.** Verify it immediately opens `AlarmActivity`.
6. **Background Test:** Minimize the app. Wait for timer. Verify `AlarmActivity` comes to foreground (if permission granted).
