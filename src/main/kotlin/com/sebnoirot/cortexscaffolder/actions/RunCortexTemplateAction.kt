package com.sebnoirot.cortexscaffolder.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.sebnoirot.cortexscaffolder.ui.TemplateWizardDialog

/**
 * Action to trigger the Cortex Template Wizard.
 * This action is registered in the Tools menu.
 */
class RunCortexTemplateAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        thisLogger().info("Run Cortex Template action triggered")

        // Open the template wizard dialog
        TemplateWizardDialog(project).show()
    }

    override fun update(e: AnActionEvent) {
        // Enable the action only when a project is open
        e.presentation.isEnabled = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
