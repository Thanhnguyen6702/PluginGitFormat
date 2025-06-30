# Git Format - IntelliJ IDEA Plugin

<!-- Plugin description -->
A powerful IntelliJ IDEA plugin that helps developers create professional, well-formatted Git commit messages and streamline their Git workflow.

## ✨ Key Features

🎯 **Smart Commit Message Formatting**: Automatically format commit messages following conventional commits standard  
🔗 **JIRA Integration**: Seamlessly integrate with JIRA and other issue tracking systems  
✅ **Input Validation**: Ensure your commit messages meet quality standards  
🚀 **Pull Request Integration**: Quick pull request creation with advanced integration  

## 🎯 Benefits

- Maintain consistent commit message format across your team
- Improve code review efficiency with clear, descriptive commits
- Better project history and easier debugging
- Professional Git workflow for development teams

Perfect for teams who want to maintain high-quality Git history and improve their development workflow.
<!-- Plugin description end -->

## 📦 Installation

### From JetBrains Marketplace
1. Open IntelliJ IDEA
2. Go to `File` → `Settings` → `Plugins`
3. Search for "Git Format"
4. Click Install and restart IDE

### From Source Code
1. Clone this repository
2. Open in IntelliJ IDEA
3. Run `./gradlew buildPlugin`
4. Install plugin from `.zip` file created in `build/distributions/`

## 🚀 Usage

### Format Commit Message

**Keyboard Shortcut**: `Alt + Q`

1. Press `Alt + Q` to open the commit format dialog
2. Enter issue number and description
3. Plugin will automatically format according to conventional commits standard
4. Preview the formatted message in real-time
5. Click OK to apply to commit editor

### Create Pull Request

1. Use "Create Pull Request" action from VCS menu
2. Fill in pull request information
3. JIRA integration if configured
4. Plugin will automatically use the latest commit message as template

## ⚙️ Configuration

Go to `File` → `Settings` → `Tools` → `Git Format Settings` to:

### GitHub Integration
- **GitHub Token**: Token for interacting with GitHub API
- **Default Base Branch**: Default branch for pull requests (usually `main` or `develop`)

### JIRA Integration  
- **JIRA URL**: URL of your JIRA instance (e.g., `https://yourcompany.atlassian.net`)
- **JIRA Email**: JIRA login email
- **JIRA API Token**: API token for JIRA authentication

### Commit Format
- Customize commit types and formats
- Set up project-specific prefixes
- Adjust other preferences

### Pull Request Templates

To use pull request templates, create template files in your project:

**Template Location**: `.github/PULL_REQUEST_TEMPLATE/`

**Example template structure**:
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
```


## 🤝 Contributing
All contributions are welcome!

## 📄 License

Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author Information

**Author**: Thanh Nguyen  
**Email**: thanhnguyen6702@gmail.com  
**GitHub**: [https://github.com/Thanhnguyen6702/PluginGitFormat](https://github.com/Thanhnguyen6702/PluginGitFormat)  
**Version**: 0.0.1

## 🆘 Support

If you encounter any issues or have questions:

1. Check existing [Issues](https://github.com/Thanhnguyen6702/PluginGitFormat/issues)
2. Create a new issue with detailed description
3. Or send an email to the author

---

⭐ If this plugin is helpful to you, please give it a star on GitHub! 