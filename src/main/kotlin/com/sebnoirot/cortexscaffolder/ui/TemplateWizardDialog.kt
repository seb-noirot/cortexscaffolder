package com.sebnoirot.cortexscaffolder.ui

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.sebnoirot.cortexscaffolder.logic.CookiecutterRunner
import com.sebnoirot.cortexscaffolder.logic.GitCloner
import com.sebnoirot.cortexscaffolder.logic.TemplateParser
import com.sebnoirot.cortexscaffolder.settings.CortexScaffolderSettings
import com.sebnoirot.cortexscaffolder.ui.CookiecutterConsole
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JComboBox
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.SwingUtilities
import java.awt.event.ItemEvent
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import com.intellij.openapi.project.ProjectManager

/**
 * Dialog to collect template information from the user.
 * Asks for a Git URL of the template and the target folder.
 */
class TemplateWizardDialog(private val project: Project) : DialogWrapper(project) {

    private val templateComboBox = JComboBox<String>()
    private val gitUrlField = JBTextField(40)
    private val branchTagComboBox = JComboBox<String>()
    private val refreshBranchesButton = JButton("Refresh")
    private val targetFolderField = TextFieldWithBrowseButton()

    private val gitCloner = GitCloner(project)
    private val templateParser = TemplateParser(project)
    private val cookiecutterRunner = CookiecutterRunner(project)

    // Map to store template names and URLs
    private val templatesMap = mutableMapOf<String, String>()

    // Map to store branch/tag names and their types
    private val branchTagMap = mutableMapOf<String, String>()

    /**
     * Shows a dialog asking the user whether to open the generated folder as a new project or import it into the current one.
     * 
     * @param generatedDir The directory where the template was generated
     */
    private fun showOpenProjectDialog(generatedDir: File) {
        val options = arrayOf("Open as New Project", "Import into Current Project", "Do Nothing")
        val choice = Messages.showDialog(
            project,
            "Template generated successfully at: ${generatedDir.absolutePath}\n\nWhat would you like to do with the generated project?",
            "Template Generation Complete",
            options,
            0, // Default option (Open as New Project)
            Messages.getInformationIcon()
        )

        when (choice) {
            0 -> openAsNewProject(generatedDir)
            1 -> importIntoCurrentProject(generatedDir)
            // For option 2 (Do Nothing), we don't need to do anything
        }
    }

    /**
     * Opens the generated directory as a new project.
     * 
     * @param generatedDir The directory to open as a new project
     */
    private fun openAsNewProject(generatedDir: File) {
        val projectPath = generatedDir.absolutePath
        ProjectUtil.openOrImport(projectPath, project, false)
    }

    /**
     * Imports the generated directory into the current project.
     * 
     * @param generatedDir The directory to import into the current project
     */
    private fun importIntoCurrentProject(generatedDir: File) {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(generatedDir)
        if (virtualFile != null) {
            // Refresh the VFS to ensure the IDE sees the new files
            virtualFile.refresh(false, true)

            // Open the directory in the Project view
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        } else {
            Messages.showErrorDialog(project, "Could not find the generated directory in the file system", "Import Failed")
        }
    }

    /**
     * Fetches branches and tags for the given Git URL and populates the branch/tag dropdown.
     */
    private fun fetchBranchesAndTags() {
        val gitUrl = gitUrlField.text
        if (gitUrl.isBlank()) {
            Messages.showErrorDialog(project, "Git URL cannot be empty", "Validation Error")
            return
        }

        // Disable the refresh button while fetching
        refreshBranchesButton.isEnabled = false
        refreshBranchesButton.text = "Fetching..."

        // Run the fetch operation in a background task
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Fetching Branches and Tags", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    // Fetch branches and tags
                    indicator.text = "Listing branches and tags..."
                    val branchesAndTags = gitCloner.listBranchesAndTags(gitUrl, indicator)

                    // Update the UI on the EDT
                    SwingUtilities.invokeLater {
                        // Clear the current map and model
                        branchTagMap.clear()
                        val model = DefaultComboBoxModel<String>()

                        // Add the default HEAD option
                        model.addElement("HEAD (default)")
                        branchTagMap["HEAD (default)"] = "HEAD"

                        // Add branches first, then tags
                        val branches = branchesAndTags.filter { it.value == "branch" }.keys.sorted()
                        val tags = branchesAndTags.filter { it.value == "tag" }.keys.sorted()

                        // Add branches with a prefix
                        branches.forEach { branch ->
                            val displayName = "Branch: $branch"
                            model.addElement(displayName)
                            branchTagMap[displayName] = branch
                        }

                        // Add tags with a prefix
                        tags.forEach { tag ->
                            val displayName = "Tag: $tag"
                            model.addElement(displayName)
                            branchTagMap[displayName] = tag
                        }

                        // Update the combo box
                        branchTagComboBox.model = model
                        branchTagComboBox.selectedItem = "HEAD (default)"

                        // Re-enable the refresh button
                        refreshBranchesButton.isEnabled = true
                        refreshBranchesButton.text = "Refresh"
                    }

                } catch (e: Exception) {
                    // Handle errors
                    SwingUtilities.invokeLater {
                        Messages.showErrorDialog(project, "Error fetching branches and tags: ${e.message}", "Fetch Failed")
                        refreshBranchesButton.isEnabled = true
                        refreshBranchesButton.text = "Refresh"
                    }
                }
            }
        })
    }

    init {
        title = "Run Cortex Template"

        // Configure the target folder field with a folder chooser
        targetFolderField.addBrowseFolderListener(
            "Select Target Folder",
            "Choose the folder where the template will be generated",
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )

        // Configure the refresh button
        refreshBranchesButton.addActionListener {
            fetchBranchesAndTags()
        }

        // Initialize the branch/tag combo box
        val branchTagModel = DefaultComboBoxModel<String>()
        branchTagModel.addElement("HEAD (default)")
        branchTagMap["HEAD (default)"] = "HEAD"
        branchTagComboBox.model = branchTagModel

        // Get settings and load known templates
        val settings = CortexScaffolderSettings.getInstance(project)

        // Pre-fill with default output path if defined in settings
        if (settings.defaultOutputDirectory.isNotBlank()) {
            targetFolderField.text = settings.defaultOutputDirectory
        }

        // Load known templates from settings
        templatesMap.clear()
        templatesMap["-- Custom URL --"] = ""  // Add a default option for custom URL
        templatesMap.putAll(settings.knownTemplates)  // Add all templates from settings

        // Set up the template combo box
        val model = DefaultComboBoxModel<String>()
        model.addElement("-- Custom URL --")  // Add the default option

        // Add all template names from the map
        for (name in templatesMap.keys) {
            if (name != "-- Custom URL --") {  // Skip the default option we already added
                model.addElement(name)
            }
        }

        templateComboBox.model = model

        // Add listener to update the Git URL field when a template is selected
        templateComboBox.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                val selectedTemplate = event.item as String
                val templateUrl = templatesMap[selectedTemplate] ?: ""

                // Only update the Git URL field if a template is selected (not the default option)
                if (selectedTemplate != "-- Custom URL --") {
                    gitUrlField.text = templateUrl
                    gitUrlField.isEnabled = false  // Disable editing when a template is selected

                    // Fetch branches and tags for the selected template
                    fetchBranchesAndTags()
                } else {
                    gitUrlField.text = ""  // Clear the field for custom URL
                    gitUrlField.isEnabled = true  // Enable editing for custom URL

                    // Clear the branch/tag combo box
                    branchTagMap.clear()
                    val branchTagModel = DefaultComboBoxModel<String>()
                    branchTagModel.addElement("HEAD (default)")
                    branchTagMap["HEAD (default)"] = "HEAD"
                    branchTagComboBox.model = branchTagModel
                }
            }
        }

        // Initialize with the default option
        templateComboBox.selectedItem = "-- Custom URL --"
        gitUrlField.isEnabled = true

        init()
    }

    override fun createCenterPanel(): JComponent {
        // Create a panel for the branch/tag selection with the refresh button
        val branchTagPanel = JPanel()
        branchTagPanel.layout = java.awt.BorderLayout()
        branchTagPanel.add(branchTagComboBox, java.awt.BorderLayout.CENTER)
        branchTagPanel.add(refreshBranchesButton, java.awt.BorderLayout.EAST)

        val formBuilder = FormBuilder.createFormBuilder()
            .addLabeledComponent("Template:", templateComboBox)
            .addTooltip("Select a predefined template or choose 'Custom URL' to enter a Git URL manually")
            .addLabeledComponent("Git URL:", gitUrlField)
            .addTooltip("URL of the Git repository containing the template")
            .addLabeledComponent("Branch/Tag:", branchTagPanel)
            .addTooltip("Select a branch or tag to checkout (use Refresh to fetch the list)")
            .addLabeledComponent("Target Folder:", targetFolderField)
            .addTooltip("Folder where the template will be generated")
            .addComponentFillVertically(JPanel(), 0)

        return formBuilder.panel
    }

    override fun doOKAction() {
        val selectedTemplate = templateComboBox.selectedItem as String
        val gitUrl = if (selectedTemplate == "-- Custom URL --") {
            // For custom URL, use the text from the Git URL field
            gitUrlField.text
        } else {
            // For predefined template, use the URL from the templates map
            templatesMap[selectedTemplate] ?: ""
        }
        val targetFolder = targetFolderField.text

        // Get the selected branch/tag
        val selectedBranchTag = branchTagComboBox.selectedItem as String
        val branchTagRef = branchTagMap[selectedBranchTag]

        if (gitUrl.isBlank()) {
            Messages.showErrorDialog(project, "Git URL cannot be empty", "Validation Error")
            return
        }

        if (targetFolder.isBlank()) {
            Messages.showErrorDialog(project, "Target folder cannot be empty", "Validation Error")
            return
        }

        // Close the dialog
        super.doOKAction()

        // Run the template cloning and parsing in a background task
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Processing Template", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    // Clone the repository with the selected branch/tag
                    indicator.text = "Cloning repository..."
                    if (branchTagRef != null && branchTagRef != "HEAD") {
                        indicator.text = "Cloning repository and checking out $selectedBranchTag..."
                    }
                    indicator.fraction = 0.1
                    val templateDir = gitCloner.cloneToTemp(gitUrl, branchTagRef, indicator)

                    // Parse the template
                    indicator.text = "Parsing template..."
                    indicator.fraction = 0.5
                    val variables = templateParser.parseTemplate(templateDir, indicator)

                    // Log the results
                    thisLogger().info("Found ${variables.size} variables in template")
                    variables.forEach { (name, variable) ->
                        thisLogger().info("Variable: $name, Default: ${variable.defaultValue}, Dynamic: ${variable.isDynamic}")
                    }

                    // Show input wizard for variables on the EDT
                    val targetFolderValue = targetFolder
                    val templateDirValue = templateDir

                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        val inputDialog = VariableInputDialog(project, variables, targetFolderValue)
                        if (inputDialog.showAndGet()) {
                            val userInputs = inputDialog.getVariableValues()
                            thisLogger().info("User provided values for ${userInputs.size} variables")

                            // Run Cookiecutter with input in a background task
                            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Running Cookiecutter", true) {
                                override fun run(indicator: ProgressIndicator) {
                                    // Create and show the cookiecutter console in the tool window
                                    val cookiecutterConsole = CookiecutterConsole(project)
                                    cookiecutterConsole.show()
                                    cookiecutterConsole.println("Starting Cookiecutter generation...")

                                    try {
                                        // Run Cookiecutter with the user input
                                        val generatedDir = cookiecutterRunner.runCookiecutter(
                                            templateDirValue,
                                            userInputs,
                                            targetFolderValue,
                                            indicator
                                        ) { message, isError ->
                                            // Update the console output
                                            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                                                cookiecutterConsole.println(message, isError)
                                            }
                                        }

                                        // Show success message and options to open the generated folder
                                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                                            cookiecutterConsole.println("\nTemplate generated successfully at: ${generatedDir.absolutePath}")

                                            // Show a dialog with options to open the generated folder
                                            showOpenProjectDialog(generatedDir)
                                        }

                                    } catch (e: Exception) {
                                        // Log the full exception with stack trace
                                        thisLogger().error("Error running Cookiecutter", e)

                                        // Get the detailed error message
                                        val errorMessage = e.message ?: "Unknown error"

                                        // Display error in the console
                                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                                            // Print a separator for better visibility
                                            cookiecutterConsole.println("\n========== ERROR ==========", true)
                                            cookiecutterConsole.println("Error running Cookiecutter:", true)

                                            // Split the error message by newlines to format it nicely in the console
                                            errorMessage.split("\n").forEach { line ->
                                                cookiecutterConsole.println(line, true)
                                            }

                                            // Print the stack trace for debugging
                                            cookiecutterConsole.println("\nStack trace:", true)
                                            e.stackTrace.take(10).forEach { stackElement ->
                                                cookiecutterConsole.println("  at $stackElement", true)
                                            }

                                            // Show error dialog after ensuring console output is visible
                                            Messages.showErrorDialog(
                                                project, 
                                                "Cookiecutter failed. See the console output for details.", 
                                                "Cookiecutter Failed"
                                            )
                                        }
                                    }
                                }
                            })
                        } else {
                            thisLogger().info("User cancelled the variable input dialog")
                        }
                    }

                } catch (e: Exception) {
                    thisLogger().error("Error processing template", e)
                    Messages.showErrorDialog(project, "Error: ${e.message}", "Template Processing Failed")
                }
            }
        })
    }
}
