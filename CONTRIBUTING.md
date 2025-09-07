# Contributing to TempusTrace

Thank you for your interest in contributing to TempusTrace! This document provides guidelines and information for contributors.

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 21
- Git
- Android SDK with API 34+

### Setting Up Development Environment
1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/TempusTrace.git
   cd TempusTrace
   ```
3. Open the project in Android Studio
4. Wait for Gradle sync to complete
5. Run the app to ensure everything works

## 📝 Development Guidelines

### Code Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful names for variables, functions, and classes
- Add KDoc comments for public APIs
- Keep functions small and focused
- Use dependency injection with Hilt

### Architecture Patterns
- Follow MVVM architecture pattern
- Use Repository pattern for data access
- Keep ViewModels free of Android framework dependencies
- Use LiveData for observing data changes
- Implement proper lifecycle management

### Commit Messages
Use clear and descriptive commit messages:
- `feat: add break duration customization`
- `fix: resolve time calculation bug`
- `docs: update README with new features`
- `refactor: improve database query performance`

## 🧪 Testing

### Running Tests
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

### Writing Tests
- Write unit tests for ViewModels and repositories
- Add instrumented tests for database operations
- Test edge cases and error handling
- Maintain test coverage above 80%

## 🐛 Bug Reports

When reporting bugs, please include:
- Android version and device model
- App version
- Steps to reproduce
- Expected vs actual behavior
- Screenshots or screen recordings if applicable

## 💡 Feature Requests

Before submitting feature requests:
- Check existing issues to avoid duplicates
- Clearly describe the use case
- Explain why the feature would be valuable
- Consider implementation complexity

## 📋 Pull Request Process

1. **Create a Branch**: Use descriptive branch names
   ```bash
   git checkout -b feature/add-export-functionality
   git checkout -b fix/time-calculation-bug
   ```

2. **Make Changes**: Follow coding guidelines and best practices

3. **Test**: Ensure all tests pass and add new tests if needed

4. **Document**: Update README or add inline documentation as needed

5. **Commit**: Use clear, descriptive commit messages

6. **Push**: Push your changes to your fork

7. **Create PR**: Submit a pull request with:
   - Clear title and description
   - Reference any related issues
   - Include screenshots for UI changes
   - List any breaking changes

### PR Review Process
- All PRs require at least one review
- Address feedback promptly
- Keep PRs focused and reasonably sized
- Ensure CI checks pass

## 🎯 Priority Areas

We're especially interested in contributions for:

### High Priority
- [ ] Data export/import functionality
- [ ] Notification reminders
- [ ] Widget support
- [ ] Performance optimizations

### Medium Priority
- [ ] Advanced analytics and reporting
- [ ] Calendar integration
- [ ] Theme customization
- [ ] Accessibility improvements

### Low Priority
- [ ] Additional break types
- [ ] Time zone support
- [ ] Cloud synchronization
- [ ] Multiple work profiles

## 🏗️ Project Structure

Understanding the codebase:
```
app/src/main/java/com/example/tempustrace/
├── data/                   # Database entities, DAOs, repositories
│   ├── AppDatabase.kt     # Room database configuration
│   ├── WorkDay.kt         # Work session entity
│   ├── Break.kt           # Break entity
│   └── UserPreferencesRepository.kt
├── di/                    # Dependency injection
│   └── AppModule.kt       # Hilt modules
├── ui/                    # User interface
│   ├── account/           # Settings and preferences
│   ├── dashboard/         # Statistics and analytics
│   └── tracking/          # Time tracking functionality
└── MainActivity.kt        # Single activity navigation
```

## 🤝 Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Help others learn and grow
- Maintain a positive environment

## 📞 Getting Help

- Join discussions in GitHub Issues
- Ask questions in pull request comments
- Review existing documentation
- Reach out to maintainers for guidance

## 🎉 Recognition

Contributors will be recognized in:
- README contributors section
- Release notes for significant contributions
- GitHub contributors graph

Thank you for contributing to TempusTrace! 🚀