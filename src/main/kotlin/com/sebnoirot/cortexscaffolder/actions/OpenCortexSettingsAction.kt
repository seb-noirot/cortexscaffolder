package com.sebnoirot.cortexscaffolder.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project

/**
 * Action to open the Cortex Scaffolder settings panel.
 * This action is registered in the Tools menu.
 */
class OpenCortexSettingsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        thisLogger().info("Open Cortex Scaffolder Settings action triggered")

        // Open the settings dialog for Cortex Scaffolder
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Cortex Scaffolder")
    }

    override fun update(e: AnActionEvent) {
        // Enable the action only when a project is open
        e.presentation.isEnabled = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}