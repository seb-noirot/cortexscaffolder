package com.sebnoirot.cortexscaffolder.logic

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.file.Files

/**
 * Utility class to clone Git repositories.
 */
class GitCloner(private val project: Project) {

    /**
     * Lists all available branches and tags in a Git repository.
     *
     * @param gitUrl The URL of the Git repository
     * @param progressIndicator Optional progress indicator to update during the operation
     * @return A map of ref names to their types (branch or tag)
     */
    fun listBranchesAndTags(gitUrl: String, progressIndicator: ProgressIndicator? = null): Map<String, String> {
        thisLogger().info("Listing branches and tags for repository: $gitUrl")
        progressIndicator?.text = "Listing branches and tags..."

        // Build the Git ls-remote command
        val processBuilder = ProcessBuilder(
            "git",
            "ls-remote",
            "--heads",
            "--tags",
            gitUrl
        )

        // Set the working directory and redirect error stream
        processBuilder.directory(File(System.getProperty("user.home")))
        processBuilder.redirectErrorStream(true)

        // Start the process and wait for it to complete
        val process = processBuilder.start()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            val errorOutput = process.inputStream.bufferedReader().readText()
            thisLogger().error("Git ls-remote failed: $errorOutput")
            throw RuntimeException("Failed to list branches and tags for repository: $gitUrl. Error: $errorOutput")
        }

        // Parse the output to extract branch and tag names
        val output = process.inputStream.bufferedReader().readText()
        val result = mutableMapOf<String, String>()

        // Add the default HEAD option
        result["HEAD (default)"] = "HEAD"

        // Parse each line of the output
        output.lines().filter { it.isNotEmpty() }.forEach { line ->
            val parts = line.split("\t")
            if (parts.size == 2) {
                val ref = parts[1]
                when {
                    ref.startsWith("refs/heads/") -> {
                        val branchName = ref.removePrefix("refs/heads/")
                        result[branchName] = "branch"
                    }
                    ref.startsWith("refs/tags/") -> {
                        var tagName = ref.removePrefix("refs/tags/")
                        // Skip annotated tag objects (ends with ^{})
                        if (!tagName.endsWith("^{}")) {
                            result[tagName] = "tag"
                        }
                    }
                }
            }
        }

        thisLogger().info("Found ${result.size} branches and tags")
        return result
    }

    /**
     * Clones a Git repository to a temporary directory.
     *
     * @param gitUrl The URL of the Git repository to clone
     * @param ref Optional branch or tag to checkout after cloning
     * @param progressIndicator Optional progress indicator to update during the clone operation
     * @return The temporary directory where the repository was cloned
     */
    fun cloneToTemp(gitUrl: String, ref: String? = null, progressIndicator: ProgressIndicator? = null): File {
        thisLogger().info("Cloning repository: $gitUrl" + (ref?.let { ", ref: $it" } ?: ""))
        progressIndicator?.text = "Cloning repository..."

        // Create a temporary directory
        val tempDir = Files.createTempDirectory("cortex-template-").toFile()
        tempDir.deleteOnExit()

        // Build the Git clone command
        val processBuilder = ProcessBuilder(
            "git",
            "clone",
            gitUrl,
            tempDir.absolutePath
        )

        // Set the working directory and redirect error stream
        processBuilder.directory(File(System.getProperty("user.home")))
        processBuilder.redirectErrorStream(true)

        // Start the process and wait for it to complete
        val process = processBuilder.start()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            val errorOutput = process.inputStream.bufferedReader().readText()
            thisLogger().error("Git clone failed: $errorOutput")
            throw RuntimeException("Failed to clone repository: $gitUrl. Error: $errorOutput")
        }

        // If a specific ref was requested, checkout that ref
        if (ref != null && ref != "HEAD (default)") {
            progressIndicator?.text = "Checking out $ref..."

            // Determine the actual ref to checkout
            val refToCheckout = if (ref == "HEAD") "HEAD" else ref

            // Build the Git checkout command
            val checkoutProcessBuilder = ProcessBuilder(
                "git",
                "checkout",
                refToCheckout
            )

            // Set the working directory to the cloned repository
            checkoutProcessBuilder.directory(tempDir)
            checkoutProcessBuilder.redirectErrorStream(true)

            // Start the process and wait for it to complete
            val checkoutProcess = checkoutProcessBuilder.start()
            val checkoutExitCode = checkoutProcess.waitFor()

            if (checkoutExitCode != 0) {
                val errorOutput = checkoutProcess.inputStream.bufferedReader().readText()
                thisLogger().error("Git checkout failed: $errorOutput")
                throw RuntimeException("Failed to checkout ref: $ref. Error: $errorOutput")
            }

            thisLogger().info("Checked out ref: $ref")
        }

        thisLogger().info("Repository cloned successfully to: ${tempDir.absolutePath}")
        return tempDir
    }
}
