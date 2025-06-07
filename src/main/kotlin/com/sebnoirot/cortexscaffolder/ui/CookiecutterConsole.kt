package com.sebnoirot.cortexscaffolder.ui

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.ui.content.ContentFactory
import javax.swing.JComponent

/**
 * Console window to show the output of the Cookiecutter process.
 */
class CookiecutterConsole(private val project: Project) {

    private val consoleView: ConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console

    /**
     * Shows the console in the tool window.
     */
    fun show() {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            var toolWindow = toolWindowManager.getToolWindow("Cortex Scaffolder")

            if (toolWindow == null) {
                // If the tool window doesn't exist, create it
                toolWindow = toolWindowManager.registerToolWindow(RegisterToolWindowTask("Cortex Scaffolder"))
            }

            // Create a content for the tool window
            val contentFactory = ContentFactory.getInstance()
            val content = contentFactory.createContent(consoleView.component, "Cookiecutter Output", false)

            // Add the content to the tool window
            toolWindow.contentManager.addContent(content)
            toolWindow.show()
        }
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

    /**
     * Returns the console component.
     */
    fun getComponent(): JComponent {
        return consoleView.component
    }
}
