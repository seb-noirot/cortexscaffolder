# 🧑‍💻 Coding Guidelines for Cortex Scaffolder Plugin

## 📝 General
- Use Kotlin (not Java) for all plugin logic
- Follow JetBrains Platform SDK idioms and best practices
- Avoid blocking the Event Dispatch Thread (EDT)

## 📦 Package Naming
- Use `com.sebnoirot.cortexscaffolder` as the base package

## 🎨 UI/UX
- Use standard IntelliJ Swing components
- Generate forms dynamically from variable definitions
- Use `DialogWrapper` or `Task.Backgroundable` for user interactions and background tasks

## 🧪 Testing
- Isolate pure logic for unit testing
- Mock file system access and subprocesses in tests

## 📂 Structure
Organize the codebase into:
- `actions`: IDE actions or entry points
- `ui`: Swing forms and dialogs
- `logic`: Copier/Template parsing, variable analysis, process execution
- `settings`: Persistent user configurations and defaults

## 🧱 Compatibility

- Target IntelliJ Platform version 2025.2
- Do not use deprecated APIs
- Follow IntelliJ deprecation warnings and migrate to recommended alternatives
