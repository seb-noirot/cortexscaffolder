package com.sebnoirot.cortexscaffolder.logic

import com.intellij.openapi.diagnostic.thisLogger
import java.util.regex.Pattern

/**
 * Helper class to enhance variable fields with metadata from _cortex_field_configurations.
 * This class processes the Cortex-specific metadata field to provide additional
 * information for rendering and validating input fields.
 */
class CortexFieldEnhancer {

    /**
     * Data class to represent field configuration metadata.
     */
    data class FieldConfiguration(
        val tooltip: String? = null,
        val isHidden: Boolean = false,
        val isReadOnly: Boolean = false,
        val options: List<String>? = null
    )

    // Pattern to match field configuration entries in JSON
    private val fieldConfigPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{([^}]+)\\}")

    // Patterns to match specific properties within a field configuration
    private val tooltipPattern = Pattern.compile("\"tooltip\"\\s*:\\s*\"([^\"]+)\"")
    private val hiddenPattern = Pattern.compile("\"hidden\"\\s*:\\s*(true|false)")
    private val readonlyPattern = Pattern.compile("\"readonly\"\\s*:\\s*(true|false)")
    private val optionsPattern = Pattern.compile("\"options\"\\s*:\\s*\\[([^\\]]+)\\]")
    private val optionValuePattern = Pattern.compile("\"([^\"]+)\"")

    /**
     * Processes the _cortex_field_configurations metadata and returns a map of field configurations.
     *
     * @param configJson The JSON string containing the field configurations
     * @return A map of variable names to their field configurations
     */
    fun processFieldConfigurations(configJson: String?): Map<String, FieldConfiguration> {
        if (configJson.isNullOrBlank()) {
            return emptyMap()
        }

        val configurations = mutableMapOf<String, FieldConfiguration>()

        try {
            val fieldConfigMatcher = fieldConfigPattern.matcher(configJson)

            while (fieldConfigMatcher.find()) {
                val fieldName = fieldConfigMatcher.group(1)
                val fieldConfigContent = fieldConfigMatcher.group(2)

                // Extract tooltip
                val tooltipMatcher = tooltipPattern.matcher(fieldConfigContent)
                val tooltip = if (tooltipMatcher.find()) tooltipMatcher.group(1) else null

                // Extract hidden flag
                val hiddenMatcher = hiddenPattern.matcher(fieldConfigContent)
                val isHidden = if (hiddenMatcher.find()) hiddenMatcher.group(1) == "true" else false

                // Extract readonly flag
                val readonlyMatcher = readonlyPattern.matcher(fieldConfigContent)
                val isReadOnly = if (readonlyMatcher.find()) readonlyMatcher.group(1) == "true" else false

                // Extract options
                val optionsMatcher = optionsPattern.matcher(fieldConfigContent)
                val options = if (optionsMatcher.find()) {
                    val optionsContent = optionsMatcher.group(1)
                    val optionValueMatcher = optionValuePattern.matcher(optionsContent)
                    val optionsList = mutableListOf<String>()

                    while (optionValueMatcher.find()) {
                        optionsList.add(optionValueMatcher.group(1))
                    }

                    if (optionsList.isEmpty()) null else optionsList
                } else null

                configurations[fieldName] = FieldConfiguration(
                    tooltip = tooltip,
                    isHidden = isHidden,
                    isReadOnly = isReadOnly,
                    options = options
                )
            }
        } catch (e: Exception) {
            thisLogger().warn("Failed to parse _cortex_field_configurations: ${e.message}")
        }

        return configurations
    }

    /**
     * Enhances the given variables with metadata from the field configurations.
     *
     * @param variables The map of variables to enhance
     * @param fieldConfigurations The map of field configurations
     * @return The enhanced variables map
     */
    fun enhanceVariables(
        variables: MutableMap<String, TemplateParser.TemplateVariable>,
        fieldConfigurations: Map<String, FieldConfiguration>
    ): MutableMap<String, TemplateParser.TemplateVariable> {

        // Enhance each variable with its field configuration
        fieldConfigurations.forEach { (name, config) ->
            variables[name]?.let { variable ->
                val enhancedDescription = buildString {
                    append(variable.description ?: "")
                    if (!variable.description.isNullOrBlank() && config.tooltip != null) {
                        append("\n")
                    }
                    if (config.tooltip != null) {
                        append(config.tooltip)
                    }
                }

                variables[name] = variable.copy(
                    description = enhancedDescription.ifBlank { null }
                )
            }
        }

        return variables
    }
}
