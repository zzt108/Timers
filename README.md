# MultiTimer

A robust Android application written in Kotlin that allows users to create and manage multiple timers running in parallel.

## Features

- **Multiple Parallel Timers**: Create and run as many timers as needed simultaneously.
- **Background Operation**: Timers continue running reliably even when the app is in the background or the device is in doze mode.
- **Customizable Timers**: Name your timers and set custom durations (hours/minutes).
- **Timer Controls**: Start, pause, reset, edit, and delete individual timers.
- **Visual Progress**: Track remaining time with circular progress indicators.
- **Completion Notifications**: Receive alerts when timers complete, even on the lock screen.
- **Battery Optimization Handling**: Functions reliably despite system battery restrictions, with user prompts for exemption.
- **Data Persistence**: Timer states are preserved between app sessions using SharedPreferences and Gson.
- **Settings & About**: Dedicated screens for app configuration and version information.
- **Absolute Time Calculation**: Increased punctuality by calculating remaining time from a fixed end timestamp.

## Architecture

MultiTimer implements modern Android architecture patterns with Kotlin, adhering to **SOLID principles**:

### Core Components

- **MVVM Pattern**: Clean separation between UI (`MainActivity`, `TimerAdapter`), business logic (`TimerViewModel`), and data (`TimerRepository`).
- **Repository Pattern**: Centralized data persistence and retrieval logic.
- **Coroutines & Flow**: Efficient asynchronous timer processing using `viewModelScope`, `StateFlow`, and `Dispatchers.IO`.
- **Foreground Service**: `TimerService` ensures reliable background execution with ongoing notifications.
- **Broadcast Receivers**: `TimerAlarmReceiver` handles precise alarm triggers via `AlarmManager`.

### Key Libraries

- **Jetpack Compose** (Partial): Integrated for specific UI components.
- **ViewBinding**: Type-safe view interaction for XML layouts.
- **Gson**: JSON serialization for object persistence.
- **AndroidX Core/AppCompat**: Modern Android support libraries.

## Technical Implementation

- **Kotlin StateFlow**: Reactive UI updates through observable state streams.
- **AlarmManager**: Used for precise timer completion triggers (Android 12+ `SCHEDULE_EXACT_ALARM` support).
- **WakeLocks**: Ensures CPU stays awake for critical background processing.
- **Service Communication**: Bound service pattern for communication between `TimerViewModel` and `TimerService`.
- **Absolute Time Logic**: Timers calculate remaining time based on `System.currentTimeMillis()` vs. a stored `absoluteEndTimeMillis` to prevent drift.

## Getting Started

### Requirements

- **Android Studio**: Otter 2 or newer recommended.
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 35
- **Language**: Kotlin

### Installation

1. Clone this repository.
2. Open the project in Android Studio.
3. Sync Gradle files.
4. Build and run on your device or emulator.

## Usage

### Creating a Timer

1. Tap the **"Add Timer"** button.
2. Enter a name (optional).
3. Set hours using up/down controls.
4. Adjust minutes using the slider.
5. Confirm with **"Add"**.

### Managing Timers

- **Start/Pause**: Tap the play/pause button.
- **Reset**: Return timer to its initial duration.
- **Edit**: Modify timer name or duration (supports editing running timers).
- **Delete**: Remove unwanted timers.

### Battery Optimization

For best performance, the app may request to disable battery optimization. This ensures timers ring exactly when they should, even if the phone has been idle.

## Project Structure

```

com.pneumasoft.multitimer/
├── data/              \# Data models (TimerItem)
├── receivers/         \# Broadcast receivers (TimerAlarmReceiver)
├── repository/        \# Data access (TimerRepository)
├── services/          \# Background services (TimerService)
├── ui/                \# UI components (Activities, Adapters)
├── viewmodel/         \# Business logic (TimerViewModel)
└── MainActivity.kt    \# Entry point

```

## Known Issues

See `.doc/issues.md` for a current list of tracked bugs and feature requests.

- Active issues: ~14 open (e.g., repeating alarms, timer filtering).

## License

This project is licensed under the Apache License 2.0 - see the `LICENSE` file for details.
