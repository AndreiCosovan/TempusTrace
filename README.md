# TempusTrace

[![Android](https://img.shields.io/badge/Android-34+-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-34%2B-brightgreen.svg)](https://android-arsenal.com/api?level=34)

TempusTrace is a modern Android time tracking application designed to help you monitor your work hours and breaks efficiently. Built with the latest Android technologies and following Material Design principles, it provides an intuitive interface for tracking daily work patterns and analyzing productivity metrics.

## 📱 Features

### Time Tracking
- **Work Session Tracking**: Start and stop work sessions with precise timing
- **Break Management**: Track multiple breaks during work sessions
  - First break (default: 18 minutes)
  - Second break (default: 36 minutes)
- **Automatic Duration Calculation**: Net work time calculation excluding breaks

### Dashboard & Analytics
- **Weekly Statistics**: Days worked and total hours for the current week
- **Monthly Overview**: Comprehensive monthly work tracking
- **Overall Statistics**: Total tracked days and average daily hours
- **Time Balance**: Track overtime or undertime with visual indicators
- **Recent Workdays**: Quick overview of recent work sessions

### User Preferences
- **Customizable Work Hours**: Set default start and end times
- **Flexible Break Durations**: Configure break lengths to match your schedule
- **Persistent Settings**: All preferences saved locally using DataStore

### Modern UI
- **Material Design 3**: Clean, modern interface following latest design guidelines
- **Dark/Light Theme Support**: Adapts to system theme preferences
- **Responsive Layout**: Optimized for different screen sizes
- **Intuitive Navigation**: Bottom navigation with three main sections

## 🏗️ Architecture

TempusTrace follows modern Android development best practices:

### Architecture Pattern
- **MVVM (Model-View-ViewModel)**: Clean separation of concerns
- **Repository Pattern**: Centralized data access layer
- **Dependency Injection**: Hilt for managing dependencies

### Components
- **UI Layer**: Fragments with ViewBinding for type-safe view access
- **ViewModel Layer**: Business logic and UI state management
- **Repository Layer**: Data access abstraction
- **Database Layer**: Room for local data persistence

### Navigation
- **Single Activity Architecture**: Using Navigation Component
- **Fragment-based Navigation**: Three main destinations:
  - 🕐 **Tracking**: Start/stop work sessions and manage breaks
  - 📊 **Dashboard**: View statistics and recent activity
  - ⚙️ **Account**: Configure preferences and settings

## 🛠️ Technology Stack

### Core Technologies
- **Kotlin**: 100% Kotlin codebase
- **Android SDK**: Target API 36, Minimum API 34
- **Gradle**: 8.13 with Kotlin DSL

### Architecture Components
- **Navigation Component**: Fragment navigation and deep linking
- **ViewModel & LiveData**: Lifecycle-aware UI components
- **ViewBinding**: Type-safe view access
- **DataStore**: Modern preferences storage

### Database & Storage
- **Room Database**: Local SQLite database with type converters
- **Entities**: WorkDay and Break with proper relationships
- **DataStore Preferences**: User settings storage

### Dependency Injection
- **Hilt**: Dagger-based dependency injection
- **KSP**: Kotlin Symbol Processing for annotation processing

### UI & Design
- **Material Design Components**: Modern UI components
- **ConstraintLayout**: Flexible layout management
- **RecyclerView**: Efficient list displays

## 📦 Installation & Setup

### Prerequisites
- **Android Studio**: Arctic Fox or later
- **JDK**: Java 21
- **Android SDK**: API 34 or higher

### Clone and Build
```bash
# Clone the repository
git clone https://github.com/AndreiCosovan/TempusTrace.git
cd TempusTrace

# Make gradlew executable (Linux/macOS)
chmod +x gradlew

# Build the project
./gradlew build

# Install on connected device
./gradlew installDebug
```

### Project Structure
```
app/
├── src/main/java/com/example/tempustrace/
│   ├── data/              # Database entities, DAOs, and repositories
│   ├── di/                # Dependency injection modules
│   ├── ui/
│   │   ├── account/       # Settings and preferences
│   │   ├── dashboard/     # Statistics and analytics
│   │   └── tracking/      # Time tracking functionality
│   ├── MainActivity.kt    # Single activity with navigation
│   └── TempusTraceApplication.kt
├── src/main/res/          # Resources (layouts, strings, etc.)
└── build.gradle.kts       # Module-level build configuration
```

## 📊 Database Schema

### WorkDay Entity
```kotlin
@Entity(tableName = "work_days")
data class WorkDay(
    val id: Long,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime?
)
```

### Break Entity
```kotlin
@Entity(tableName = "breaks")
data class Break(
    val id: Long,
    val workDayId: Long,        // Foreign key to WorkDay
    val startTime: LocalTime,
    val endTime: LocalTime?,
    val durationMinutes: Int?
)
```

## 🎯 Usage

1. **Start Tracking**: Navigate to the Tracking tab and tap "Start Work" to begin a session
2. **Take Breaks**: Use the break buttons to track your rest periods
3. **End Session**: Stop tracking when your workday is complete
4. **View Analytics**: Check the Dashboard for detailed statistics
5. **Customize Settings**: Adjust work hours and break durations in Account settings

## 🤝 Contributing

We welcome contributions! Here's how you can help:

### Development Setup
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes following the coding standards
4. Add tests for new functionality
5. Ensure all tests pass: `./gradlew test`
6. Commit your changes: `git commit -m 'Add amazing feature'`
7. Push to the branch: `git push origin feature/amazing-feature`
8. Open a Pull Request

### Coding Standards
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Maintain consistent formatting
- Write tests for new features

### Areas for Contribution
- [ ] Export data functionality
- [ ] Week/month view calendars
- [ ] Notification reminders
- [ ] Data backup/restore
- [ ] Widget support
- [ ] Advanced analytics

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Links

- [Android Developer Documentation](https://developer.android.com)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Material Design Guidelines](https://material.io/design)

## 📞 Support

If you encounter any issues or have questions:
1. Check existing [Issues](https://github.com/AndreiCosovan/TempusTrace/issues)
2. Create a new issue with detailed information
3. Include device information and steps to reproduce

---

Built with ❤️ using modern Android development practices