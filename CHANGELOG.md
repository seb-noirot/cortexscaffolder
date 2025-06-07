<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# cortexscaffolder Changelog

## [Unreleased]
### Added
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- Git-based Cookiecutter template support with automatic cloning
- Template variable extraction from both `cookiecutter.json` and dynamic references
- Interactive UI wizard for template variable input with form validation
- Support for Cortex-style dynamic variables (e.g., `cookiecutter['_@cortex_inputs'].git.repository.projectName`)
- Background task execution with progress indicators for long-running operations
- Real-time console output dialog showing Cookiecutter execution progress
- Plugin settings panel for configuring default directories and template repositories
- Folder picker integration for target directory selection
- Proper nested JSON structure generation for complex Cortex variables
- Error handling with detailed error messages and stack traces
- Project opening options after successful template generation

### Fixed
- Fixed logging levels that were causing unnecessary error stack traces in console output
- Resolved EDT (Event Dispatch Thread) violations when showing dialogs from background threads
- Fixed nested variable structure generation for Cortex templates with proper JSON hierarchy
- Corrected ActionUpdateThread usage to avoid deprecated OLD_EDT warnings
- Fixed console output display to show both stdout and stderr even on Cookiecutter failures

### Changed
- Simplified Cookiecutter execution by directly modifying `cookiecutter.json` instead of using `--config-file` parameter
- Improved error handling with better user feedback and detailed logging
- Enhanced UI layout using IntelliJ's FormBuilder for consistent styling
- Refactored variable parsing to handle both legacy underscore format and new path-based format
- Upgraded to modern IntelliJ Platform APIs and removed deprecated functionality
- Implemented proper backup and restoration of original template files during execution

### Technical Improvements
- Added proper thread management for UI operations
- Implemented robust file handling with automatic cleanup
- Enhanced variable extraction with regex-based parsing for implicit references
- Added support for `_cortex_field_configurations` metadata parsing
- Improved JSON generation with proper escaping and nested structure support