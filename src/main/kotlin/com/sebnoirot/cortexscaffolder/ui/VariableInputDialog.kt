package com.sebnoirot.cortexscaffolder.ui

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.sebnoirot.cortexscaffolder.logic.TemplateParser.TemplateVariable
import javax.swing.JComboBox
import javax.swing.*
import java.awt.BorderLayout
import java.awt.GridLayout

/**
 * Dialog to collect input for template variables from the user.
 * Displays all extracted variables with their default values and descriptions.
 * Groups dynamic variables in a separate section.
 */
class VariableInputDialog(
    private val project: Project,
    private val variables: Map<String, TemplateVariable>,
    private val targetFolder: String
) : DialogWrapper(project) {

    private val inputFields = mutableMapOf<String, InputComponent>()

    // Interface to represent any input component
    private interface InputComponent {
        fun getText(): String
    }

    // Wrapper for JBTextField
    private class TextFieldComponent(private val textField: JBTextField) : InputComponent {
        override fun getText(): String = textField.text
    }

    // Wrapper for JComboBox
    private class ComboBoxComponent(private val comboBox: JComboBox<String>) : InputComponent {
        override fun getText(): String = comboBox.selectedItem as String
    }

    init {
        title = "Template Variables"
        init()
    }

    override fun createCenterPanel(): javax.swing.JComponent {
        // Filter out hidden variables
        val visibleVars = variables.filter { !it.value.isHidden }

        // Separate standard and dynamic variables
        val standardVars = visibleVars.filter { !it.value.isDynamic }
        val dynamicVars = visibleVars.filter { it.value.isDynamic }

        // Start building the form
        var formBuilder = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Please provide values for the following template variables:"))
            .addVerticalGap(10)

        // Add standard variables section
        if (standardVars.isNotEmpty()) {
            val standardLabel = JBLabel("Standard Variables")
            standardLabel.font = UIUtil.getLabelFont().deriveFont(UIUtil.getLabelFont().size + 2f)
            formBuilder = formBuilder.addComponent(standardLabel)
                .addVerticalGap(5)

            formBuilder = addVariablesToFormBuilder(formBuilder, standardVars)
        }

        // Add dynamic variables section
        if (dynamicVars.isNotEmpty()) {
            val dynamicLabel = JBLabel("Dynamic Variables (Cortex-style)")
            dynamicLabel.font = UIUtil.getLabelFont().deriveFont(UIUtil.getLabelFont().size + 2f)
            formBuilder = formBuilder.addVerticalGap(10)
                .addComponent(dynamicLabel)
                .addVerticalGap(5)

            formBuilder = addVariablesToFormBuilder(formBuilder, dynamicVars)
        }

        // Create a scroll pane for the form
        val panel = formBuilder.panel
        val scrollPane = JBScrollPane(panel)
        scrollPane.preferredSize = JBUI.size(500, 400)

        return scrollPane
    }

    private fun addVariablesToFormBuilder(formBuilder: FormBuilder, variables: Map<String, TemplateVariable>): FormBuilder {
        var builder = formBuilder

        variables.forEach { (name, variable) ->
            // Create a panel for this variable with a border if it's dynamic
            val variablePanel = JPanel(BorderLayout())
            if (variable.isDynamic) {
                variablePanel.border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIUtil.getLabelForeground().brighter(), 1, true),
                    JBUI.Borders.empty(5)
                )
                variablePanel.background = UIUtil.getPanelBackground().brighter()
            }

            // Create a variable form for this variable
            val variableFormBuilder = FormBuilder.createFormBuilder()

            // Create a label for the variable
            val displayName = if (variable.isDynamic) variable.name else name
            val label = JBLabel("$displayName:")
            if (variable.isDynamic) {
                label.font = UIUtil.getLabelFont().deriveFont(java.awt.Font.BOLD)
            }

            // Create the appropriate input component based on variable configuration
            val inputComponent: javax.swing.JComponent

            if (variable.options != null && variable.options.isNotEmpty()) {
                // Create a dropdown for options
                val comboBox = JComboBox(variable.options.toTypedArray())
                variable.defaultValue?.let { defaultValue ->
                    val index = variable.options.indexOf(defaultValue)
                    if (index >= 0) {
                        comboBox.selectedIndex = index
                    }
                }
                inputComponent = comboBox
                inputFields[name] = ComboBoxComponent(comboBox)
            } else {
                // Create a text field for the input
                val textField = JBTextField(variable.defaultValue ?: "")
                textField.isEditable = !variable.isReadOnly
                inputComponent = textField
                inputFields[name] = TextFieldComponent(textField)
            }

            // Add the label and input component to the variable form
            variableFormBuilder.addLabeledComponent(label, inputComponent)

            // Add a description if available
            variable.description?.let {
                val descLines = it.split("\n")
                for (line in descLines) {
                    val descLabel = JBLabel(line)
                    descLabel.foreground = UIUtil.getContextHelpForeground()
                    variableFormBuilder.addComponentToRightColumn(descLabel, 0)
                }
            }

            // Add the variable form to the variable panel
            variablePanel.add(variableFormBuilder.panel, BorderLayout.CENTER)

            // Add the variable panel to the main form
            builder = builder.addComponent(variablePanel)
                .addVerticalGap(10)
        }

        return builder
    }

    /**
     * Returns a map of variable names to their user-provided values.
     */
    fun getVariableValues(): Map<String, String> {
        return inputFields.mapValues { it.value.getText() }
    }
}
