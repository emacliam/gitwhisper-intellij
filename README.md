# GitWhisper IntelliJ Plugin

🤖 AI-powered Git commit message generator for IntelliJ IDEA and other JetBrains IDEs

<!-- Plugin description -->
GitWhisper is an IntelliJ IDEA plugin that generates meaningful, conventional commit messages using AI. It analyzes your staged changes and creates commit messages that follow best practices, supporting multiple AI providers and languages.

## ✨ Features

### 🤖 AI-Powered Intelligence
- **Multiple AI Providers**: OpenAI, Claude, Gemini, Ollama, and more
- **Smart Analysis**: Understands code context and generates relevant messages
- **Conventional Commits**: Follows conventional commit format with emojis
- **Multi-language Support**: Generate commit messages in 30+ languages

### 🔧 Smart Git Integration
- **Automatic Staging**: Option to auto-stage unstaged changes
- **File Filtering**: Ignore specific files (lock files, logs, etc.)
- **Multi-repository Support**: Works with multiple Git repositories
- **GitHub Integration**: Support for GitHub Personal Access Tokens

### 🎯 Developer Experience
- **Keyboard Shortcuts**: Quick access with customizable shortcuts
- **Status Bar Integration**: See GitWhisper status at a glance
- **Progress Indicators**: Visual feedback during AI processing
- **Secure Storage**: API keys stored securely using IntelliJ's credential system
<!-- Plugin description end -->

## 🚀 Quick Start

1. **Install the Plugin**
   - Open IntelliJ IDEA
   - Go to `File` → `Settings` → `Plugins`
   - Search for "GitWhisper" and install

2. **Configure AI Provider**
   - Go to `File` → `Settings` → `Tools` → `GitWhisper`
   - Select your preferred AI model (OpenAI, Claude, etc.)
   - Enter your API key
   - Choose your preferred language for commit messages

3. **Generate Your First Commit**
   - Stage your changes: `git add .` or use the Git tool window
   - Use `Ctrl+Alt+G` (or `Cmd+Alt+G` on Mac) to generate a commit message
   - Review and commit!

## 🎮 Usage

### Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+Alt+G` / `Cmd+Alt+G` | Generate AI Commit Message |
| `Ctrl+Alt+A` / `Cmd+Alt+A` | Analyze Staged Changes |
| `Ctrl+Alt+C` / `Cmd+Alt+C` | Configure GitWhisper |

### Menu Actions

All GitWhisper actions are available in:
- **VCS Menu**: `VCS` → `GitWhisper`
- **Git Tool Window**: Right-click context menu
- **Command Palette**: `Ctrl+Shift+A` / `Cmd+Shift+A` and search for "GitWhisper"

## 🤖 Supported AI Models

### Currently Implemented
- **OpenAI**: GPT-4o, GPT-4o Mini, GPT-4 Turbo, GPT-3.5 Turbo
- **Claude**: Claude 3.5 Sonnet, Claude 3.5 Haiku, Claude 3 Opus

### Coming Soon
- **Gemini**: Gemini 1.5 Pro, Gemini 1.5 Flash
- **Ollama**: Local AI models (Llama, Qwen, DeepSeek, etc.)
- **GitHub Models**: GitHub's AI model offerings
- **Grok**: X.AI's Grok models
- **DeepSeek**: DeepSeek Chat and Coder models

## 🌍 Multi-Language Support

GitWhisper supports commit messages in 30+ languages including:
- English, Spanish, French, German, Italian
- Portuguese, Russian, Chinese, Japanese, Korean
- Dutch, Polish, Czech, Hungarian, Romanian
- And many more...

## ⚙️ Configuration

### API Keys
- Stored securely using IntelliJ's credential system
- Support for environment variables as fallback
- Per-model configuration

### File Filtering
Configure which files to ignore when generating commit messages:
```
package-lock.json
yarn.lock
*.log
*.tmp
.DS_Store
```

### Advanced Settings
- Custom Ollama base URL
- Auto-staging preferences
- Auto-push configuration
- Custom model variants

## 🔧 Development

### Prerequisites
- IntelliJ IDEA 2024.3+
- JDK 21+
- Kotlin 1.9+

### Building
```bash
./gradlew build
```

### Running
```bash
./gradlew runIde
```

### Testing
```bash
./gradlew test
```

## 📝 Based on VSCode Extension

This IntelliJ plugin is based on the excellent [GitWhisper VSCode extension](https://github.com/emacliam/gitwhisper-vscode) and maintains feature parity while adapting to IntelliJ's ecosystem.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Original [GitWhisper CLI](https://github.com/iamngoni/gitwhisper) by [iamngoni](https://github.com/iamngoni)
- [GitWhisper VSCode Extension](https://github.com/emacliam/gitwhisper-vscode) for the reference implementation
- JetBrains for the excellent IntelliJ Platform

---

**Made with ❤️ for developers who want better commit messages**
