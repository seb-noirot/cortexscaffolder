package com.sebnoirot.cortexscaffolder.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.icons.AllIcons

/**
 * Factory for creating the Cortex Console tool window.
 * This replaces the programmatic registration of the tool window
 * which was using an internal API.
 */
class CortexConsoleToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // We don't create any content here
        // The content will be created dynamically when CookiecutterConsole.show() is called
    }

    override fun shouldBeAvailable(project: Project) = true
}
