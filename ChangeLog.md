# Changelog

All notable changes to MultiTimer will be documented in this file.

## [1.0.0] - 2025-04-25

### Added
- Initial release of MultiTimer
- Multiple parallel timers with independent controls
- Customizable timer names and durations
- Visual circular progress indicators for each timer
- Background operation via foreground service
- Timer notifications when complete
- Battery optimization handling for reliable background operation
- Complete timer controls (start, pause, reset, edit, delete)
- Data persistence between app sessions
- Default timer presets for first-time users
- Completion time display for running timers
- About screen with build information

### Technical
- Modern MVVM architecture with Kotlin coroutines and flows
- Repository pattern for data management
- AlarmManager integration for reliable timer completion
- WakeLock implementation for background operation
- Adaptive battery optimization detection for different manufacturers
- Responsive UI with RecyclerView and custom adapters

## [0.9.0] - 2025-04-24

### Added
- Implemented service communication for timer management
- Added battery optimization exemption request
- Created timer completion notifications

### Fixed
- Timer persistence issues
- UI updates for timer controls
- Background service stability

## [0.5.0] - 2025-04-24

### Added
- Basic timer functionality
- Timer creation and editing
- Initial UI implementation

### Known Issues
- Limited background operation
- No notification support
- Inconsistent timer behavior when app is closed

## [0.1.0] - 2025-04-23

### Added
- Project initialization
- Basic app structure
- Repository and data model setup