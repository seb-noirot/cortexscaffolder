# Cortex Scaffolder

![Build](https://github.com/seb-noirot/cortexscaffolder/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
**Cortex Scaffolder** is a JetBrains plugin that integrates [Cookiecutter](https://cookiecutter.readthedocs.io) templating into your IDE, with advanced support for dynamic templates used by Cortex.

The plugin allows you to:
- Select a Cookiecutter template from a Git repository
- Automatically extract all variables, including dynamically referenced ones (e.g. `cookiecutter['_@cortex_inputs'].git.repository.projectName`)
- Fill them in via a dynamic UI wizard
- Run the scaffolder in a background task
- Open the generated project in your IDE
<!-- Plugin description end -->

---

## Features

- Git-based template selection
- Dynamic variable extraction from all template files
- UI wizard for variable input
- Background execution of Cookiecutter CLI
- Real-time output display in a dedicated tool window
- Cortex-style support for nested dynamic variables
- Configurable default templates and output paths

## Usage

1. Select **Tools** → **Cortex Scaffolder** → **Run Template** from the menu
2. Enter the Git URL of your Cookiecutter template and the target folder
3. Fill in the template variables in the wizard
4. The template generation process will start and the output will be displayed in the **Cortex Scaffolder** tool window (accessible from the bottom toolbar)
5. Once complete, you'll be prompted to open the generated project

## Installation

### From the IDE

<kbd>Settings / Preferences</kbd> → <kbd>Plugins</kbd> → <kbd>Marketplace</kbd> → Search for `Cortex Scaffolder` → <kbd>Install</kbd>

### From JetBrains Marketplace

Visit [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and click **Install to IDE**.

### Manual installation

Download the [latest release](https://github.com/seb-noirot/cortexscaffolder/releases/latest) and install it using:

<kbd>Settings / Preferences</kbd> → <kbd>Plugins</kbd> → <kbd>⚙️</kbd> → <kbd>Install plugin from disk...</kbd>

---

## Development Checklist

- [x] Project initialized from [IntelliJ Platform Plugin Template][template]
- [x] Set `pluginGroup`, `pluginName`, and `id` in `gradle.properties` and `plugin.xml`
- [ ] Replace `MARKETPLACE_ID` in badges and links once published
- [ ] Complete legal setup and token signing before publishing

---

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
