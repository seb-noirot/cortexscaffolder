# ⚙️ Cortex Scaffolder Plugin (JetBrains) via Juni

This plugin integrates the Cookiecutter scaffolding tool into JetBrains IDEs with extended support for Cortex-style dynamic variables.

---

## ✅ Objectives

- Support running Cookiecutter via IDE wizard
- Extract and display both static and dynamic variables
- Handle user input and run the generator as a background task
- Provide plugin configuration UI

---

## 🧱 Tasks

### 1. Project Setup - [x]
```juni
Create a JetBrains plugin named `Cortex Scaffolder` using the `intellij-platform-plugin-template`. It should be written in Kotlin, use Gradle, and follow JetBrains plugin SDK standards.
```

### 2. Define Plugin Structure and Packages - [x]
```juni
Organize the plugin into packages:
- `actions`: Entry points (menu/actions)
- `ui`: All Swing components
- `logic`: Business logic for parsing and executing templates
- `settings`: Persistent user preferences and defaults
Use `com.sebnoirot.cortexscaffolder` as base package.
```

### 3. Add Action to Trigger Template Wizard - [x]
```juni
Add an IDE action named “Run Cortex Template” under the Tools menu. When triggered, open a dialog asking for a Git URL of the template and the target folder.
```

### 4. Clone and Parse Template - [x]
```juni
Clone the provided Git template to a temporary folder. Recursively scan all text-based files and extract all `cookiecutter[...]` expressions. Merge these with entries from `cookiecutter.json`.
```

### 5. Show Input Wizard for Variables - [x]
```juni
Generate a form with inputs for all extracted variables. Display type hints and default values where applicable. Group undefined/dynamic variables in a separate section.
```

### 6. Run Cookiecutter with Input - [x]
```juni
Generate a temporary `cookiecutter.json` with the user input and invoke `cookiecutter` using a background process. Show output in a console window in the IDE.
```

### 7. Create Settings Panel - [x]
```juni
Add a settings panel under “Tools → Cortex Scaffolder” where users can define:
- Default output directory
- Known template repositories
- CLI binary path
```

### 8. Show Summary and Open Result - [x]
```juni
After generation, ask whether to open the generated folder as a new project or import it into the current one.
```

### 9. Extract Implicit Template Variables (Cortex-style) - [x]
```juni
Ensure variables like `cookiecutter['_@cortex_inputs'].git.repository.projectName` are parsed and mapped properly for user input. Highlight nested/dynamic ones in the UI.
```

### 10. Use Folder Picker for Target Directory - [x]
```juni
In the input form shown to the user, replace the plain text input for the target folder with a proper folder picker UI component.

Use `TextFieldWithBrowseButton` and attach a `FileChooserDescriptorFactory.createSingleFolderDescriptor()` to allow folder selection. Pre-fill it with the default output path if defined in settings.
```

### 11. Use FormBuilder for Wizard UI - [x]
```juni
Replace custom layout code in the variable input wizard with `FormBuilder` from IntelliJ's UI DSL.

Group variables with titles and descriptions where possible. Use consistent spacing and alignment. Ensure dynamic field rendering works well with FormBuilder layout constraints.
```

### 12. Replace Deprecated ActionUpdateThread.OLD_EDT - [x]
```juni
Update the `RunCortexTemplateAction` class to override `getActionUpdateThread()` and return the appropriate value.

Since this action involves UI interaction (shows a dialog), it should use `ActionUpdateThread.EDT`.

Replace any use of the default (deprecated `OLD_EDT`) with:

override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
}

This ensures compatibility with IntelliJ Platform 2025.2 and avoids deprecated API warnings.
```

### 13. Handle `_cortex_field_configurations` Metadata Field - [x]
```juni
Update the variable extraction and form rendering logic to recognize `_cortex_field_configurations` as a metadata-only field that describes other input variables.

Do not prompt the user for `_cortex_field_configurations` itself.

Instead, parse its content to:
- Provide help tooltips or UI hints for other variables
- Potentially override default rendering options (e.g., mark certain fields as hidden, readonly, or with dropdowns)

This structure is Cortex-specific and should be handled via a `CortexFieldEnhancer` helper class inside the `logic` module.
```

### 14. Fix EDT Violation When Showing Dialogs - [x]
```juni
Fix the RuntimeException: "Access is allowed from Event Dispatch Thread (EDT) only" caused by trying to show a `DialogWrapper` from a background thread.

Wrap the instantiation and display of `DialogWrapper` (e.g., `ConsoleOutputDialog`) inside:

    ApplicationManager.getApplication().invokeLater {
        val dialog = ConsoleOutputDialog(project, output)
        dialog.show()
    }

This ensures the dialog is created on the EDT and complies with IntelliJ threading rules.
```

### 15. Display Cookiecutter Output Even on Failure - [x]
```juni
Ensure that `ConsoleOutputDialog` displays output from both `stdout` and `stderr` even when Cookiecutter exits with code != 0.

Refactor the execution flow so that:
- The dialog is shown unconditionally (regardless of success/failure)
- Both output streams are passed and visible to the user
- Errors are logged via `Logger.error(...)` for debugging
- Only throw a RuntimeException *after* the dialog is shown
```

### 16. Provide Values for Nested Implicit Variables - [x]
```juni
Fix Cookiecutter runtime failure when variables like `cookiecutter['_@cortex_inputs'].git.repository.projectName` are used in the template path.

Ensure the plugin injects a properly nested structure in the answers file:

{
  "_@cortex_inputs": {
    "git": {
      "repository": {
        "projectName": "my-project"
      }
    }
  }
}

Extract required nested paths and dynamically build the correct hierarchy in the JSON passed to Cookiecutter.
```

### 17. Construct Nested `_@cortex_inputs` Hierarchy in `_cookiecutter` - [x]
```juni
To satisfy templates that reference `cookiecutter['_@cortex_inputs'].git.repository.projectName`, inject a nested structure under `_cookiecutter._@cortex_inputs` in the generated JSON.

For example:

{
  "_cookiecutter": {
    "_@cortex_inputs": {
      "git": {
        "repository": {
          "projectName": "value"
        }
      }
    }
  }
}

Dynamically build this tree from parsed variable paths. Do not flatten the structure.
```

### 18. Add Settings Entry in Plugin Menu - [x]
```juni
Add a new action under the "Cortex Scaffolder" menu to directly open the plugin's settings panel.

- Register a new `AnAction` named `OpenCortexSettingsAction`
- When triggered, it should call `ShowSettingsUtil.getInstance().showSettingsDialog(project, "Cortex Scaffolder")`
- Place this action under the same menu group as "Run Cortex Template" for consistency
```

### 19. Show Predefined Templates from Settings in Wizard - [x]
```juni
Update the template wizard dialog to list the templates defined in the plugin settings.

- Load the list of known templates (name + Git URL) from the plugin configuration
- Display them in a dropdown menu in the wizard
- When a template is selected, prefill the Git URL field accordingly
- Also allow custom URLs to be manually entered

Ensure the dropdown updates the internal model used to fetch and process the template.
```

### 20. Support Branch/Tag Selection Before Cloning Template - [x]
```juni
Update the wizard to let users select a Git branch or tag before cloning the template repository.

- After selecting a Git URL, use `git ls-remote` to list available branches and tags
- Present them in a dropdown
- When the user confirms, clone the repository to a temporary folder
- Immediately checkout the selected ref (branch/tag) using `git checkout <ref>` before calling `cookiecutter`
- Do not rely on `--checkout`, as the template is used from a local path

If no ref is selected, use the default HEAD.
```

### 21. Replace Internal API Usage - [x]
```juni
Audit and replace any usage of IntelliJ Platform APIs annotated with `@ApiStatus.Internal`.

Follow the Internal API Migration Guidelines and identify supported, stable alternatives for each usage.

Refactor the code to remove reliance on internal APIs to ensure forward compatibility with future IntelliJ versions.
```

### 22. Replace TextFieldWithBrowseButton.addBrowseFolderListener (Scheduled for Removal) - [x]
```juni
The method `TextFieldWithBrowseButton.addBrowseFolderListener(...)` is scheduled for removal.

Replace it using the recommended IntelliJ API:
- Use `TextBrowseFolderListener` with a `FileChooserDescriptor`
- Register it with `addActionListener(...)` instead of the deprecated shortcut method

Ensure compatibility with IntelliJ Platform 2025.2+.
```

### 23. Replace Deprecated ProcessAdapter Class - [x]
```juni
The class `ProcessAdapter` is deprecated.

Replace it with `ProcessListener` and override the required methods explicitly.

Update `CookiecutterRunner.runCookiecutter()` to use `ProcessHandler.addProcessListener(...)` with a proper implementation of `ProcessListener` instead of the deprecated `ProcessAdapter`.
```

### 24. Replace Deprecated capitalize(String) Extension - [x]
```juni
The Kotlin extension function `capitalize()` is deprecated.

Replace usages like `string.capitalize()` with the idiomatic alternative:

string.replaceFirstChar { it.titlecase() }

Update `TemplateParser.extractVariablesFromContent` accordingly.
```
