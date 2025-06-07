# 🧠 Plugin Plan: Cortex Scaffolder

## 🎯 Objective

Develop a JetBrains plugin that integrates the Cookiecutter templating tool with extended support for Cortex-style dynamic templates. The plugin should allow users to select a Git-based template, extract all required and implicit variables, provide input via a UI wizard, and generate the scaffolded project inside the IDE.

---

## 🧭 Scope

- Support Git-based Cookiecutter templates
- Parse both `cookiecutter.json` and dynamic variable references (e.g. Cortex-style variables)
- Show extracted variables in a UI wizard with type hints and defaults
- Run Cookiecutter CLI with user-provided data
- Provide plugin settings for default paths and template repositories
- Execute all long-running actions in background tasks

---

## 🔩 Architecture Overview

- Kotlin-based plugin using the IntelliJ Platform SDK
- Subprocess wrapper to call `cookiecutter` CLI
- Regex-based variable extractor for implicit references
- Swing-based UI with `DialogWrapper` for wizards
- YAML and JSON parsing for template metadata
- Plugin-wide settings persisted via IntelliJ services

---

## 🧪 Compatibility Notes

- Target platform: IntelliJ IDEA 2025.2
- Avoid all deprecated APIs
- Comply with migration guidelines and API evolution from JetBrains

---

## 🗺️ Future Enhancements (Out of Scope for Initial Version)

- Auto-detection of `.cookiecutter.json` in existing projects and propose updates
- Regeneration or update wizard (`cookiecutter update`) if supported
- Template discovery from GitHub organizations or template index
