package com.sebnoirot.cortexscaffolder.ui

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Dialog to show the console output of a process.
 */
class ConsoleOutputDialog(
    private val project: Project,
    dialogTitle: String
) : DialogWrapper(project) {

    private val consoleView: ConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console

    init {
        title = dialogTitle
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(800, 600)

        // Add a label at the top
        val label = JBLabel("Process Output:")
        label.border = JBUI.Borders.empty(5, 10)
        panel.add(label, BorderLayout.NORTH)

        // Add the console view
        panel.add(consoleView.component, BorderLayout.CENTER)

        return panel
    }

    override fun createActions(): Array<Action> {
        // Only show the close button
        return arrayOf(okAction)
    }

    /**
     * Prints a message to the console.
     */
    fun print(message: String, isError: Boolean = false) {
        val contentType = if (isError) ConsoleViewContentType.ERROR_OUTPUT else ConsoleViewContentType.NORMAL_OUTPUT
        consoleView.print(message, contentType)
    }

    /**
     * Prints a message to the console with a newline.
     */
    fun println(message: String, isError: Boolean = false) {
        print("$message\n", isError)
    }

    /**
     * Clears the console.
     */
    fun clear() {
        consoleView.clear()
    }
}
