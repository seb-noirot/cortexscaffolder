package com.sebnoirot.cortexscaffolder.logic

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern
import com.intellij.json.JsonFileType
import com.intellij.openapi.util.text.StringUtil

/**
 * Utility class to parse Cookiecutter templates.
 */
class TemplateParser(private val project: Project) {

    // Pattern to match Cookiecutter variable expressions like {{ cookiecutter.variable_name }}
    private val cookiecutterPattern = Pattern.compile("\\{\\{\\s*cookiecutter(?:\\[['\"](.*?)['\"]\\]|\\.(\\w+))\\s*\\}\\}")

    // Pattern to match Cortex-style dynamic variables like {{ cookiecutter['_@cortex_inputs'].git.repository.projectName }}
    private val cortexPattern = Pattern.compile("\\{\\{\\s*cookiecutter\\[['\"](.*?)['\"]\\](?:\\.(\\w+))*\\s*\\}\\}")

    // Pattern to specifically extract the nested path in Cortex-style variables
    private val cortexNestedPattern = Pattern.compile("\\{\\{\\s*cookiecutter\\[['\"](.*?)['\"]\\](?:\\.(\\w+)(?:\\.(\\w+))*)?\\s*\\}\\}")

    /**
     * Data class to represent a template variable.
     */
    data class TemplateVariable(
        val name: String,
        val defaultValue: String? = null,
        val description: String? = null,
        val isDynamic: Boolean = false,
        val isHidden: Boolean = false,
        val isReadOnly: Boolean = false,
        val options: List<String>? = null
    )

    /**
     * Parses a Cookiecutter template directory and extracts all variables.
     *
     * @param templateDir The directory containing the Cookiecutter template
     * @param progressIndicator Optional progress indicator to update during parsing
     * @return A map of variable names to TemplateVariable objects
     */
    fun parseTemplate(templateDir: File, progressIndicator: ProgressIndicator? = null): Map<String, TemplateVariable> {
        thisLogger().info("Parsing template in directory: ${templateDir.absolutePath}")
        progressIndicator?.text = "Parsing template..."

        val variables = mutableMapOf<String, TemplateVariable>()

        // First, try to parse cookiecutter.json if it exists
        val cookiecutterJson = File(templateDir, "cookiecutter.json")
        if (cookiecutterJson.exists()) {
            parseJsonConfig(cookiecutterJson, variables)
        }

        // Then scan all text files for variable references
        scanFilesForVariables(templateDir.toPath(), variables, progressIndicator)

        thisLogger().info("Found ${variables.size} variables in template")
        return variables
    }

    /**
     * Parses the cookiecutter.json file to extract variables and their default values.
     * First removes the _cortex_field_configurations section, then uses a regex-based approach to extract key-value pairs.
     */
    private fun parseJsonConfig(jsonFile: File, variables: MutableMap<String, TemplateVariable>) {
        try {
            // Read the JSON content
            var jsonContent = jsonFile.readText()

            // Remove the _cortex_field_configurations section from the JSON
            // This regex matches the "_cortex_field_configurations" key and its entire value (object)
            val cortexFieldConfigPattern = "\"_cortex_field_configurations\"\\s*:\\s*\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\},?"
            jsonContent = jsonContent.replace(Regex(cortexFieldConfigPattern), "")

            // Clean up any trailing commas that might have been left after removing the section
            jsonContent = jsonContent.replace(Regex(",\\s*}"), "}")

            // Simple regex to extract key-value pairs from JSON
            val keyValuePattern = "\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]+)\"|([\\d.]+)|\\{|\\[|true|false|null)"
            val regex = Pattern.compile(keyValuePattern)
            val matcher = regex.matcher(jsonContent)

            while (matcher.find()) {
                val key = matcher.group(1)
                if (key != "_copy_without_render" && key != "_templates") {
                    val stringValue = matcher.group(2)
                    val numericValue = matcher.group(3)
                    val defaultValue = stringValue ?: numericValue

                    variables[key] = TemplateVariable(
                        name = key,
                        defaultValue = defaultValue,
                        isDynamic = false
                    )
                }
            }
        } catch (e: Exception) {
            thisLogger().warn("Failed to parse cookiecutter.json: ${e.message}")
        }
    }

    /**
     * Recursively scans all text files in the template directory for variable references.
     */
    private fun scanFilesForVariables(
        dir: Path,
        variables: MutableMap<String, TemplateVariable>,
        progressIndicator: ProgressIndicator?
    ) {
        Files.walk(dir).use { paths ->
            paths.filter { Files.isRegularFile(it) && isTextFile(it) }
                .forEach { file ->
                    progressIndicator?.text = "Scanning ${file.fileName}..."
                    try {
                        val content = Files.readString(file)
                        extractVariablesFromContent(content, variables)
                    } catch (e: Exception) {
                        thisLogger().warn("Failed to scan file ${file.fileName}: ${e.message}")
                    }
                }
        }
    }

    /**
     * Extracts variable references from file content.
     */
    private fun extractVariablesFromContent(content: String, variables: MutableMap<String, TemplateVariable>) {
        // Extract standard Cookiecutter variables
        val cookiecutterMatcher = cookiecutterPattern.matcher(content)
        while (cookiecutterMatcher.find()) {
            val bracketVar = cookiecutterMatcher.group(1)
            val dotVar = cookiecutterMatcher.group(2)
            val varName = bracketVar ?: dotVar

            if (varName != null && !variables.containsKey(varName) && !varName.startsWith("_@") && varName != "_cortex_field_configurations") {
                variables[varName] = TemplateVariable(
                    name = varName,
                    isDynamic = false
                )
            }
        }

        // Extract Cortex-style dynamic variables with improved nested path handling
        val cortexNestedMatcher = cortexNestedPattern.matcher(content)
        while (cortexNestedMatcher.find()) {
            val fullMatch = cortexNestedMatcher.group(0)
            val rootVar = cortexNestedMatcher.group(1)

            // Only process Cortex-style variables (those with _@cortex_inputs)
            if (rootVar != null && rootVar.contains("_@cortex_inputs")) {
                // Extract the full path after _@cortex_inputs
                val fullPath = fullMatch.substringAfter("_@cortex_inputs")

                // Create a clean variable name that preserves the nested structure
                val cleanPath = fullPath.replace(Regex("[\\[\\]'\"{} ]+"), "").trim('.')

                // Create a more user-friendly display name
                val displayName = cleanPath.split('.').joinToString(" ") { 
                    it.replace(Regex("([a-z])([A-Z])"), "$1 $2").capitalize() 
                }

                // Store the original path parts for proper JSON structure
                val pathParts = cleanPath.split('.')

                // Create a unique variable key that preserves the original path structure
                val varKey = "cortex_path:" + pathParts.joinToString(",")

                if (!variables.containsKey(varKey)) {
                    variables[varKey] = TemplateVariable(
                        name = displayName,
                        isDynamic = true,
                        description = "Cortex variable path: $fullPath\nOriginal expression: $fullMatch"
                    )
                }
            }
        }
    }

    /**
     * Checks if a file is likely to be a text file based on its extension.
     */
    private fun isTextFile(path: Path): Boolean {
        val filename = path.fileName.toString().lowercase()
        val textExtensions = listOf(
            ".txt", ".md", ".json", ".yml", ".yaml", ".py", ".java", ".kt", ".kts",
            ".xml", ".html", ".css", ".js", ".ts", ".sh", ".bat", ".properties"
        )

        return textExtensions.any { filename.endsWith(it) }
    }
}
