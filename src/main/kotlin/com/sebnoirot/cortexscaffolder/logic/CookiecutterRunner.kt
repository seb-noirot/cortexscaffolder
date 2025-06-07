package com.sebnoirot.cortexscaffolder.logic

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.io.FileWriter
import java.nio.charset.StandardCharsets

/**
 * Utility class to run Cookiecutter with user input.
 */
class CookiecutterRunner(private val project: Project) {

    /**
     * Runs Cookiecutter with the provided user input.
     *
     * @param templateDir The directory containing the Cookiecutter template
     * @param userInputs Map of variable names to user-provided values
     * @param targetFolder The folder where the template will be generated
     * @param progressIndicator Optional progress indicator to update during the process
     * @param outputConsumer A consumer for the process output
     * @return The directory where the template was generated
     */
    fun runCookiecutter(
        templateDir: File,
        userInputs: Map<String, String>,
        targetFolder: String,
        progressIndicator: ProgressIndicator? = null,
        outputConsumer: (String, Boolean) -> Unit
    ): File {
        thisLogger().info("Running Cookiecutter with user input")
        progressIndicator?.text = "Preparing Cookiecutter..."
        progressIndicator?.fraction = 0.6

        // Overwrite cookiecutter.json in the template directory with user input
        overwriteCookiecutterJson(templateDir, userInputs)

        // Ensure target folder exists
        val targetDir = File(targetFolder)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        // Build the Cookiecutter command
        progressIndicator?.text = "Running Cookiecutter..."
        progressIndicator?.fraction = 0.7

        val commandLine = GeneralCommandLine()
            .withExePath("cookiecutter")
            .withParameters(
                "--no-input",
                "--output-dir", targetDir.absolutePath,
                templateDir.absolutePath
            )
            .withCharset(StandardCharsets.UTF_8)
            .withRedirectErrorStream(true)

        thisLogger().error("Running command: ${commandLine.commandLineString}")
        outputConsumer("Running: ${commandLine.commandLineString}", false)

        try {
            // Start the process
            val processHandler = OSProcessHandler(commandLine)

            // Collect all output for logging purposes
            val outputBuffer = StringBuilder()
            val errorBuffer = StringBuilder()

            // Attach a process listener to capture output
            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    val text = event.text
                    if (text != null) {
                        val isError = outputType == ProcessOutputTypes.STDERR
                        outputConsumer(text, isError)
                        thisLogger().info(if (isError) "ERROR: $text" else text)

                        // Store output for error reporting
                        if (isError) {
                            errorBuffer.append(text)
                        } else {
                            outputBuffer.append(text)
                        }
                    }
                }
            })

            // Start the process and wait for it to complete
            processHandler.startNotify()
            progressIndicator?.text = "Generating template..."
            progressIndicator?.fraction = 0.8

            processHandler.waitFor()
            val exitCode = processHandler.exitCode ?: -1

            if (exitCode != 0) {
                val errorOutput = errorBuffer.toString().trim()
                val fullOutput = outputBuffer.toString().trim()

                // Log detailed error information
                thisLogger().error("Cookiecutter failed with exit code: $exitCode")
                thisLogger().error("Error output: $errorOutput")
                thisLogger().error("Full output: $fullOutput")

                // Include error details in the exception message
                val errorMessage = buildString {
                    append("Cookiecutter failed with exit code: $exitCode")
                    if (errorOutput.isNotEmpty()) {
                        append("\nError details: $errorOutput")
                    }
                    if (fullOutput.isNotEmpty() && errorOutput.isEmpty()) {
                        append("\nOutput: $fullOutput")
                    }
                }

                throw RuntimeException(errorMessage)
            }

            progressIndicator?.text = "Template generated successfully"
            progressIndicator?.fraction = 1.0

            // Try to determine the generated directory
            // Cookiecutter typically creates a directory with the project name
            val projectName = userInputs["project_name"] ?: userInputs.values.firstOrNull() ?: "generated"
            val generatedDir = File(targetDir, projectName)

            return if (generatedDir.exists() && generatedDir.isDirectory) {
                generatedDir
            } else {
                // If we can't determine the exact directory, return the target directory
                targetDir
            }

        } catch (e: ExecutionException) {
            thisLogger().error("Failed to run Cookiecutter", e)
            outputConsumer("ERROR: Failed to run Cookiecutter: ${e.message}", true)
            throw RuntimeException("Failed to run Cookiecutter: ${e.message}", e)
        }
    }

    /**
     * Overwrites the cookiecutter.json file in the template directory with the user input.
     */
    private fun overwriteCookiecutterJson(templateDir: File, userInputs: Map<String, String>) {
        val cookiecutterJsonFile = File(templateDir, "cookiecutter.json")

        // Separate regular variables from nested Cortex variables
        val regularVars = mutableMapOf<String, String>()
        val nestedVars = mutableMapOf<String, String>()

        userInputs.forEach { (key, value) ->
            if (key.startsWith("cortex_") || key.startsWith("cortex_path:")) {
                nestedVars[key] = value
            } else {
                regularVars[key] = value
            }
        }

        // Create a JSON string manually
        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{\n")

        // Add regular variables at the root level
        regularVars.entries.forEachIndexed { index, (key, value) ->
            jsonBuilder.append("    \"").append(escapeJson(key)).append("\": \"").append(escapeJson(value)).append("\"")
            // Add comma if there are more entries or if we have nested variables to add
            if (index < regularVars.size - 1 || nestedVars.isNotEmpty()) {
                jsonBuilder.append(",")
            }
            jsonBuilder.append("\n")
        }

        // Process nested Cortex variables
        if (nestedVars.isNotEmpty()) {
            // Build nested structure once and reuse
            val nestedStructure = buildNestedStructure(nestedVars)

            // Create the _@cortex_inputs structure at the root level
            jsonBuilder.append("    \"_@cortex_inputs\": {\n")

            // Add each top-level key in the nested structure
            nestedStructure.entries.forEachIndexed { index, (key, value) ->
                jsonBuilder.append("      \"").append(escapeJson(key)).append("\": ")
                appendNestedValue(jsonBuilder, value, 6) // 6 spaces indentation
                if (index < nestedStructure.size - 1) {
                    jsonBuilder.append(",")
                }
                jsonBuilder.append("\n")
            }

            jsonBuilder.append("    }\n")
        }

        jsonBuilder.append("}\n")

        // Get the JSON content
        val jsonContent = jsonBuilder.toString()

        // Write the JSON to the cookiecutter.json file in the template directory
        FileWriter(cookiecutterJsonFile).use { writer ->
            writer.write(jsonContent)
        }

        // Log the JSON content for debugging
        thisLogger().error("Overwrote cookiecutter.json file: ${cookiecutterJsonFile.absolutePath}")
        thisLogger().error("JSON content: $jsonContent")
    }

    /**
     * Builds a nested structure from Cortex variable keys.
     * Handles both legacy underscore format and new path-based format:
     * - Legacy: "cortex_git_repository_projectName" becomes { "git": { "repository": { "projectName": value } } }
     * - New: "cortex_path:git,repository,projectName" becomes { "git": { "repository": { "projectName": value } } }
     */
    private fun buildNestedStructure(nestedVars: Map<String, String>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        nestedVars.forEach { (key, value) ->
            // Check if this is a path-based key
            if (key.startsWith("cortex_path:")) {
                // Extract the path parts from the comma-separated list
                val parts = key.removePrefix("cortex_path:").split(",")

                // Start with the root map
                var current: MutableMap<String, Any> = result

                // Build the nested structure for all but the last part
                for (i in 0 until parts.size - 1) {
                    val part = parts[i]
                    if (!current.containsKey(part)) {
                        current[part] = mutableMapOf<String, Any>()
                    }
                    @Suppress("UNCHECKED_CAST")
                    current = current[part] as MutableMap<String, Any>
                }

                // Add the value at the leaf node
                if (parts.isNotEmpty()) {
                    current[parts.last()] = value
                }
            } else {
                // Legacy format: Remove the "cortex_" prefix and split by underscore
                val parts = key.removePrefix("cortex_").split("_")

                // Start with the root map
                var current: MutableMap<String, Any> = result

                // Build the nested structure for all but the last part
                for (i in 0 until parts.size - 1) {
                    val part = parts[i]
                    if (!current.containsKey(part)) {
                        current[part] = mutableMapOf<String, Any>()
                    }
                    @Suppress("UNCHECKED_CAST")
                    current = current[part] as MutableMap<String, Any>
                }

                // Add the value at the leaf node
                current[parts.last()] = value
            }
        }

        return result
    }

    /**
     * Recursively appends a nested value to the JSON builder with proper indentation.
     */
    private fun appendNestedValue(builder: StringBuilder, value: Any, indentLevel: Int) {
        val indent = " ".repeat(indentLevel)

        when (value) {
            is Map<*, *> -> {
                builder.append("{\n")

                @Suppress("UNCHECKED_CAST")
                val map = value as Map<String, Any>
                map.entries.forEachIndexed { index, (k, v) ->
                    builder.append("$indent  \"").append(escapeJson(k)).append("\": ")
                    appendNestedValue(builder, v, indentLevel + 2)
                    if (index < map.size - 1) {
                        builder.append(",")
                    }
                    builder.append("\n")
                }

                builder.append("$indent}")
            }
            is String -> {
                builder.append("\"").append(escapeJson(value)).append("\"")
            }
            else -> {
                builder.append(value.toString())
            }
        }
    }

    /**
     * Escapes special characters in a JSON string.
     */
    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
