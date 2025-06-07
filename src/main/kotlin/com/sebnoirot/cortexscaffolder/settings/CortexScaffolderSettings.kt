package com.sebnoirot.cortexscaffolder.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Persistent settings for the Cortex Scaffolder plugin.
 */
@State(
    name = "com.sebnoirot.cortexscaffolder.settings.CortexScaffolderSettings",
    storages = [Storage("cortexScaffolderSettings.xml")]
)
class CortexScaffolderSettings : PersistentStateComponent<CortexScaffolderSettings> {

    // Default output directory for generated templates
    var defaultOutputDirectory: String = ""
    
    // Known template repositories (name -> URL)
    var knownTemplates: MutableMap<String, String> = mutableMapOf()
    
    // Path to the Cookiecutter CLI binary
    var cookiecutterPath: String = "cookiecutter"
    
    override fun getState(): CortexScaffolderSettings = this

    override fun loadState(state: CortexScaffolderSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        /**
         * Gets the settings instance for the current project.
         */
        fun getInstance(project: Project): CortexScaffolderSettings {
            return project.getService(CortexScaffolderSettings::class.java)
        }
    }
}