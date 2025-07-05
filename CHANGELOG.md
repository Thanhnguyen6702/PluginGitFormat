# Changelog

## [0.0.2] - 2025-07-05

### Added

- **Template Setup Dialog**: Automatic template creation for projects without PR templates
- **Flexible Project Code Input**: Allow free-form project codes (not just strict format)
- **Enhanced Time Parser**: Smart time input with fallback parsing (supports 10h, 150dsadm → 150m, etc.)
- **Settings Persistence**: All settings now saved securely using PasswordSafe (JIRA URL, Email, Tokens)
- **English Checklist**: Professional English checklist "I have checked the previous pull request"
- **Template Auto-Detection**: Smart detection and setup guidance for projects without templates

### Improved

- **Time Input Flexibility**: Support for various time formats (1h, 30m, 1.5h, 100m → 1h40m, fallback parsing)
- **Template Management**: Real-time preview and automatic refresh after template setup
- **User Experience**: Intuitive warnings and setup guidance for missing configurations
- **Settings Security**: Enhanced data persistence using OS credential store

### Fixed

- **Template Display**: Fixed time format issues (no more 10mm or 150dsadmm)
- **Settings Loss**: Resolved JIRA URL and email not persisting after IDE restart
- **Input Validation**: Improved handling of edge cases in time input

## [0.0.1] - 2025-06-30

### Added

- Smart commit message formatting following conventional commits standard
- JIRA integration for issue tracking systems
- Input validation for commit message quality
- Pull request creation with advanced integration
- Keyboard shortcut (Alt + Q) for commit format dialog
- Configuration settings for GitHub and JIRA integration
- Support for pull request templates
