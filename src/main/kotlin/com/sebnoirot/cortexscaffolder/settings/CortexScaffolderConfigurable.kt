package com.sebnoirot.cortexscaffolder.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.*
import javax.swing.table.DefaultTableModel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.io.File

/**
 * Settings panel for the Cortex Scaffolder plugin.
 */
class CortexScaffolderConfigurable(private val project: Project) : Configurable {

    private var mainPanel: JPanel? = null
    private var defaultOutputDirField: JBTextField? = null
    private var cookiecutterPathField: JBTextField? = null
    private var templatesTableModel: DefaultTableModel? = null
    private var templatesTable: JBTable? = null
    
    // Original settings values for comparison
    private var originalSettings: CortexScaffolderSettings? = null
    
    override fun getDisplayName(): String = "Cortex Scaffolder"

    override fun createComponent(): JComponent {
        // Create the main panel
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(10)
        
        // Create fields for default output directory and cookiecutter path
        defaultOutputDirField = JBTextField()
        cookiecutterPathField = JBTextField()
        
        // Create a browse button for the default output directory
        val browseOutputDirButton = JButton("Browse...")
        browseOutputDirButton.addActionListener { browseFolderDialog(defaultOutputDirField) }
        
        // Create a browse button for the cookiecutter path
        val browseCookiecutterButton = JButton("Browse...")
        browseCookiecutterButton.addActionListener { browseFileDialog(cookiecutterPathField) }
        
        // Create a panel for the default output directory with the field and button
        val outputDirPanel = JPanel(BorderLayout())
        outputDirPanel.add(defaultOutputDirField, BorderLayout.CENTER)
        outputDirPanel.add(browseOutputDirButton, BorderLayout.EAST)
        
        // Create a panel for the cookiecutter path with the field and button
        val cookiecutterPathPanel = JPanel(BorderLayout())
        cookiecutterPathPanel.add(cookiecutterPathField, BorderLayout.CENTER)
        cookiecutterPathPanel.add(browseCookiecutterButton, BorderLayout.EAST)
        
        // Create a table for known templates
        templatesTableModel = DefaultTableModel(arrayOf("Name", "URL"), 0)
        templatesTable = JBTable(templatesTableModel)
        templatesTable?.preferredScrollableViewportSize = Dimension(500, 200)
        
        // Create buttons to add and remove templates
        val addTemplateButton = JButton("Add")
        addTemplateButton.addActionListener { addTemplate() }
        
        val removeTemplateButton = JButton("Remove")
        removeTemplateButton.addActionListener { removeTemplate() }
        
        // Create a panel for the template buttons
        val templateButtonsPanel = JPanel()
        templateButtonsPanel.add(addTemplateButton)
        templateButtonsPanel.add(removeTemplateButton)
        
        // Create a panel for the templates table and buttons
        val templatesPanel = JPanel(BorderLayout())
        templatesPanel.add(JScrollPane(templatesTable), BorderLayout.CENTER)
        templatesPanel.add(templateButtonsPanel, BorderLayout.SOUTH)
        
        // Create a section title for templates
        val templatesTitle = JBLabel("Known Template Repositories")
        templatesTitle.font = UIUtil.getLabelFont().deriveFont(UIUtil.getLabelFont().size + 2f)
        templatesTitle.border = JBUI.Borders.empty(10, 0, 5, 0)
        
        // Build the form
        val formBuilder = FormBuilder.createFormBuilder()
            .addLabeledComponent("Default Output Directory:", outputDirPanel)
            .addTooltip("The default directory where templates will be generated")
            .addLabeledComponent("Cookiecutter CLI Path:", cookiecutterPathPanel)
            .addTooltip("Path to the cookiecutter CLI executable (leave as 'cookiecutter' to use from PATH)")
            .addComponentFillVertically(JPanel(), 10)
            
        // Add the form to the main panel
        panel.add(formBuilder.panel, BorderLayout.NORTH)
        
        // Add the templates section
        val southPanel = JPanel(BorderLayout())
        southPanel.add(templatesTitle, BorderLayout.NORTH)
        southPanel.add(templatesPanel, BorderLayout.CENTER)
        panel.add(southPanel, BorderLayout.CENTER)
        
        mainPanel = panel
        return panel
    }

    override fun isModified(): Boolean {
        val settings = CortexScaffolderSettings.getInstance(project)
        
        // Check if default output directory or cookiecutter path has changed
        if (defaultOutputDirField?.text != settings.defaultOutputDirectory ||
            cookiecutterPathField?.text != settings.cookiecutterPath) {
            return true
        }
        
        // Check if templates have changed
        val currentTemplates = getTemplatesFromTable()
        if (currentTemplates.size != settings.knownTemplates.size) {
            return true
        }
        
        // Check if any template has changed
        for ((name, url) in currentTemplates) {
            if (settings.knownTemplates[name] != url) {
                return true
            }
        }
        
        return false
    }

    override fun apply() {
        val settings = CortexScaffolderSettings.getInstance(project)
        
        // Update settings with values from the form
        settings.defaultOutputDirectory = defaultOutputDirField?.text ?: ""
        settings.cookiecutterPath = cookiecutterPathField?.text ?: "cookiecutter"
        
        // Update templates
        settings.knownTemplates.clear()
        settings.knownTemplates.putAll(getTemplatesFromTable())
    }

    override fun reset() {
        val settings = CortexScaffolderSettings.getInstance(project)
        originalSettings = settings
        
        // Reset form values from settings
        defaultOutputDirField?.text = settings.defaultOutputDirectory
        cookiecutterPathField?.text = settings.cookiecutterPath
        
        // Reset templates table
        templatesTableModel?.rowCount = 0
        for ((name, url) in settings.knownTemplates) {
            templatesTableModel?.addRow(arrayOf(name, url))
        }
    }
    
    /**
     * Shows a folder selection dialog and updates the text field with the selected path.
     */
    private fun browseFolderDialog(field: JBTextField?) {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        fileChooser.dialogTitle = "Select Directory"
        
        // Set the current directory if the field has a value
        field?.text?.let { 
            val currentDir = File(it)
            if (currentDir.exists() && currentDir.isDirectory) {
                fileChooser.currentDirectory = currentDir
            }
        }
        
        val result = fileChooser.showOpenDialog(mainPanel)
        if (result == JFileChooser.APPROVE_OPTION) {
            field?.text = fileChooser.selectedFile.absolutePath
        }
    }
    
    /**
     * Shows a file selection dialog and updates the text field with the selected path.
     */
    private fun browseFileDialog(field: JBTextField?) {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        fileChooser.dialogTitle = "Select File"
        
        // Set the current directory if the field has a value
        field?.text?.let { 
            val currentFile = File(it)
            if (currentFile.exists() && currentFile.isFile) {
                fileChooser.selectedFile = currentFile
            }
        }
        
        val result = fileChooser.showOpenDialog(mainPanel)
        if (result == JFileChooser.APPROVE_OPTION) {
            field?.text = fileChooser.selectedFile.absolutePath
        }
    }
    
    /**
     * Adds a new template to the table.
     */
    private fun addTemplate() {
        // Show a dialog to enter the template name and URL
        val nameField = JTextField(20)
        val urlField = JTextField(40)
        
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel("Template Name:"))
        panel.add(nameField)
        panel.add(Box.createVerticalStrut(10))
        panel.add(JLabel("Template URL:"))
        panel.add(urlField)
        
        val result = JOptionPane.showConfirmDialog(
            mainPanel,
            panel,
            "Add Template",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )
        
        if (result == JOptionPane.OK_OPTION) {
            val name = nameField.text.trim()
            val url = urlField.text.trim()
            
            if (name.isNotEmpty() && url.isNotEmpty()) {
                templatesTableModel?.addRow(arrayOf(name, url))
            }
        }
    }
    
    /**
     * Removes the selected template from the table.
     */
    private fun removeTemplate() {
        val selectedRow = templatesTable?.selectedRow
        if (selectedRow != null && selectedRow >= 0) {
            templatesTableModel?.removeRow(selectedRow)
        }
    }
    
    /**
     * Gets the templates from the table as a map.
     */
    private fun getTemplatesFromTable(): Map<String, String> {
        val templates = mutableMapOf<String, String>()
        
        templatesTableModel?.let { model ->
            for (i in 0 until model.rowCount) {
                val name = model.getValueAt(i, 0) as? String ?: continue
                val url = model.getValueAt(i, 1) as? String ?: continue
                
                if (name.isNotEmpty() && url.isNotEmpty()) {
                    templates[name] = url
                }
            }
        }
        
        return templates
    }
}