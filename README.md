# MultiTimer

A robust Android application written in Kotlin that allows users to create and manage multiple timers running in parallel.

## Features

- **Multiple Parallel Timers**: Create and run as many timers as needed simultaneously
- **Background Operation**: Timers continue running even when the app is in the background
- **Customizable Timers**: Name your timers and set custom durations
- **Timer Controls**: Start, pause, reset, edit, and delete individual timers
- **Visual Progress**: Track remaining time with circular progress indicators
- **Completion Notifications**: Receive alerts when timers complete
- **Battery Optimization Handling**: Functions reliably despite system battery restrictions
- **Data Persistence**: Timer states are preserved between app sessions

## Architecture

MultiTimer implements modern Android architecture patterns with Kotlin:

**SOLID Principles Application**

- **Single Responsibility**: Each component has a focused purpose (TimerViewModel for timer logic, TimerService for background operations)[1]
- **Open/Closed**: Code is extended rather than modified through proper abstractions[1]
- **Liskov Substitution**: Appropriate inheritance hierarchies for services and components[1]
- **Interface Segregation**: Focused interfaces with specific responsibilities[1]
- **Dependency Inversion**: Dependencies properly injected where needed[1]

**Core Components**

- **MVVM Pattern**: Clean separation between UI, business logic, and data management[1]
- **Repository Pattern**: Centralized data handling through TimerRepository[1]
- **Coroutines**: Efficient asynchronous timer processing without complex callbacks[1]
- **StateFlow**: Reactive UI updates through observable state streams[1]
- **Foreground Service**: Reliable background operation via TimerService[1]

## Technical Implementation

- **Kotlin Flow API**: For reactive programming throughout the application[1]
- **AlarmManager**: For precise timer completion even in doze mode[1]
- **Notifications**: Rich notifications for timer completion and service status[1]
- **SharedPreferences with Gson**: For timer state persistence[1]
- **WakeLocks**: To ensure timers function reliably in background[1]

## Getting Started

### Requirements
- Android Studio Meerkat or newer
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 35

### Installation
1. Clone this repository
2. Open the project in Android Studio
3. Build and run on your device or emulator

## Usage

### Creating a Timer
1. Tap the "Add Timer" button
2. Enter a name (optional)
3. Set hours using up/down controls
4. Adjust minutes using the slider
5. Confirm with "Add"

### Managing Timers
- **Start/Pause**: Tap the play/pause button
- **Reset**: Return timer to its initial duration
- **Edit**: Modify timer name or duration
- **Delete**: Remove unwanted timers

### Battery Optimization
For best performance:
- When prompted, allow the app to disable battery optimization
- Manufacturer-specific instructions are provided for different devices

## Project Structure

```
com.pneumasoft.multitimer/
├── data/              # Data persistence
├── model/             # Data models
├── receivers/         # Broadcast receivers
├── repository/        # Data access
├── services/          # Background services
├── ui/                # UI components
├── viewmodel/         # Business logic
└── MainActivity.kt    # Main activity
```

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
