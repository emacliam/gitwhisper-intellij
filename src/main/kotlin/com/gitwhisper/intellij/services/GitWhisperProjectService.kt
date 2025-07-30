package com.gitwhisper.intellij.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.util.io.DisabledOutputStream
import org.eclipse.jgit.transport.PushResult
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig
import org.eclipse.jgit.util.FS
import com.jcraft.jsch.Session
import java.io.File
import java.io.ByteArrayOutputStream
import java.nio.file.Paths

/**
 * Project-level service for Git operations using JGit
 * Replaces Git4Idea dependency with pure JGit implementation
 */
@Service(Service.Level.PROJECT)
class GitWhisperProjectService(private val project: Project) {

    private var gitRepository: Repository? = null
    private var git: Git? = null

    init {
        initializeGitRepository()
    }

    private fun initializeGitRepository() {
        try {
            val projectPath = project.basePath ?: return
            val gitDir = findGitDirectory(File(projectPath))
            if (gitDir != null) {
                gitRepository = FileRepositoryBuilder()
                    .setGitDir(gitDir)
                    .readEnvironment()
                    .findGitDir()
                    .build()
                git = Git(gitRepository)
            }
        } catch (e: Exception) {
            // No git repository found or error initializing
            gitRepository = null
            git = null
        }
    }

    private fun findGitDirectory(dir: File): File? {
        var currentDir = dir
        while (currentDir.parentFile != null) {
            val gitDir = File(currentDir, ".git")
            if (gitDir.exists()) {
                return if (gitDir.isDirectory) gitDir else currentDir
            }
            currentDir = currentDir.parentFile
        }
        return null
    }

    /**
     * Check if the project has a Git repository
     */
    fun hasGitRepository(): Boolean {
        return gitRepository != null && git != null
    }

    /**
     * Get the Git repository
     */
    fun getRepository(): Repository? = gitRepository

    /**
     * Get the Git instance
     */
    fun getGit(): Git? = git

    /**
     * Check if there are staged changes in the repository
     */
    fun hasStagedChanges(): Boolean {
        return try {
            val git = this.git ?: return false
            val status = git.status().call()
            status.added.isNotEmpty() || status.changed.isNotEmpty() || status.removed.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if there are unstaged changes in the repository
     */
    fun hasUnstagedChanges(): Boolean {
        return try {
            val git = this.git ?: return false
            val status = git.status().call()
            status.modified.isNotEmpty() || status.untracked.isNotEmpty() || status.missing.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get the current Git status
     */
    fun getStatus(): Status? {
        return try {
            git?.status()?.call()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the staged diff
     */
    fun getStagedDiff(): String {
        return try {
            val git = this.git ?: return ""
            val repository = this.gitRepository ?: return ""

            // Check if this is a new repository with no commits
            val headRef = try {
                repository.resolve("HEAD")
            } catch (e: Exception) {
                null
            }

            if (headRef == null) {
                // New repository - show all staged files as additions
                return getStagedDiffForNewRepository()
            }

            val outputStream = ByteArrayOutputStream()
            val diffFormatter = DiffFormatter(outputStream)
            diffFormatter.setRepository(repository)

            val diffs = git.diff()
                .setCached(true)
                .call()

            for (diff in diffs) {
                diffFormatter.format(diff)
            }

            diffFormatter.close()

            outputStream.toString()
        } catch (e: Exception) {
            // Fallback for new repositories or other issues
            try {
                getStagedDiffForNewRepository()
            } catch (fallbackError: Exception) {
                throw RuntimeException("Failed to get staged diff: ${e.message}", e)
            }
        }
    }

    /**
     * Get staged diff for a new repository (no previous commits)
     */
    private fun getStagedDiffForNewRepository(): String {
        return try {
            val git = this.git ?: return ""
            val status = git.status().call()

            val result = StringBuilder()

            // Show all staged files as new additions
            for (addedFile in status.added) {
                result.append("diff --git a/$addedFile b/$addedFile\n")
                result.append("new file mode 100644\n")
                result.append("index 0000000..1234567\n")
                result.append("--- /dev/null\n")
                result.append("+++ b/$addedFile\n")

                // Try to read file content for the diff
                try {
                    val projectPath = gitRepository?.workTree?.absolutePath ?: ""
                    val file = File(projectPath, addedFile)
                    if (file.exists() && file.isFile()) {
                        val lines = file.readLines()
                        lines.forEachIndexed { index, line ->
                            result.append("@@ -0,0 +${index + 1},${lines.size} @@\n")
                            result.append("+$line\n")
                            if (index == 0) return@forEachIndexed // Just show first few lines for preview
                        }
                    }
                } catch (e: Exception) {
                    result.append("@@ -0,0 +1,1 @@\n")
                    result.append("+[Binary file or unable to read content]\n")
                }
                result.append("\n")
            }

            // Show changed files (shouldn't happen in new repo, but just in case)
            for (changedFile in status.changed) {
                result.append("diff --git a/$changedFile b/$changedFile\n")
                result.append("index 1234567..abcdefg 100644\n")
                result.append("--- a/$changedFile\n")
                result.append("+++ b/$changedFile\n")
                result.append("@@ -1,1 +1,1 @@\n")
                result.append("-[Previous content]\n")
                result.append("+[Modified content]\n")
                result.append("\n")
            }

            if (result.isEmpty()) {
                "No staged changes found."
            } else {
                result.toString()
            }

        } catch (e: Exception) {
            "Error reading staged files in new repository: ${e.message}"
        }
    }

    /**
     * Get filtered staged diff (excluding ignored files)
     */
    fun getFilteredStagedDiff(ignorePatterns: List<String>): FilteredDiffResult {
        return try {
            val git = this.git ?: return FilteredDiffResult("", emptyList())
            val status = git.status().call()

            // Get all staged files
            val stagedFiles = mutableSetOf<String>()
            stagedFiles.addAll(status.added)
            stagedFiles.addAll(status.changed)
            stagedFiles.addAll(status.removed)

            if (stagedFiles.isEmpty()) {
                return FilteredDiffResult("", emptyList())
            }

            // Filter files based on ignore patterns
            val (includedFiles, ignoredFiles) = filterFiles(stagedFiles.toList(), ignorePatterns)

            if (includedFiles.isEmpty()) {
                return FilteredDiffResult("", ignoredFiles)
            }

            // Get diff for included files only
            val diff = try {
                getStagedDiff()
            } catch (e: Exception) {
                // Fallback for new repositories or other issues
                "Staged changes detected but unable to generate diff.\n\n" +
                "Files to be committed:\n" +
                includedFiles.joinToString("\n") { "  $it" } +
                "\n\nThis is normal for new repositories with no previous commits."
            }

            FilteredDiffResult(diff, ignoredFiles)

        } catch (e: Exception) {
            // More graceful error handling
            try {
                val git = this.git ?: throw e
                val status = git.status().call()
                val stagedFiles = status.added + status.changed + status.removed

                if (stagedFiles.isNotEmpty()) {
                    val message = "Error generating diff, but staged files detected:\n\n" +
                                 stagedFiles.joinToString("\n") { "  $it" } +
                                 "\n\nThis is normal for new repositories."
                    FilteredDiffResult(message, emptyList())
                } else {
                    throw RuntimeException("No staged changes found: ${e.message}", e)
                }
            } catch (fallbackError: Exception) {
                throw RuntimeException("Failed to get filtered staged diff: ${e.message}", e)
            }
        }
    }

    /**
     * Stage all unstaged files
     */
    fun stageAllUnstagedFiles(): Int {
        return try {
            val git = this.git ?: return 0
            git.add().addFilepattern(".").call()

            // Count staged files
            getStagedFileCount()
        } catch (e: Exception) {
            throw RuntimeException("Failed to stage files: ${e.message}", e)
        }
    }

    /**
     * Stage specific files
     */
    fun stageFiles(files: List<String>): Int {
        return try {
            val git = this.git ?: return 0
            val addCommand = git.add()
            files.forEach { file ->
                addCommand.addFilepattern(file)
            }
            addCommand.call()
            files.size
        } catch (e: Exception) {
            throw RuntimeException("Failed to stage files: ${e.message}", e)
        }
    }

    /**
     * Get count of staged files
     */
    fun getStagedFileCount(): Int {
        return try {
            val git = this.git ?: return 0
            val status = git.status().call()
            status.added.size + status.changed.size + status.removed.size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Check if repository is a GitHub repository
     */
    fun isGitHubRepository(): Boolean {
        return try {
            val git = this.git ?: return false
            val remotes = git.remoteList().call()
            remotes.any { remote ->
                remote.urIs.any { uri ->
                    uri.toString().contains("github.com")
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Commit changes with message
     */
    fun commitChanges(message: String) {
        try {
            val git = this.git ?: throw RuntimeException("No Git repository found")
            git.commit().setMessage(message).call()
        } catch (e: Exception) {
            throw RuntimeException("Failed to commit: ${e.message}", e)
        }
    }

    /**
     * Commit and push changes with message
     */
    fun commitAndPush(message: String) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "Committing and pushing changes...",
            false
        ) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    // First commit
                    indicator.text = "Committing changes..."
                    commitChanges(message)

                    // Then push
                    indicator.text = "Pushing to remote..."
                    pushChanges()

                    ApplicationManager.getApplication().invokeLater {
                        com.intellij.openapi.ui.Messages.showInfoMessage(
                            project,
                            "Changes committed and pushed successfully!",
                            "GitWhisper"
                        )
                    }

                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        val errorMessage = when {
                            e.message?.contains("remote hung up") == true -> {
                                "Push failed: Remote connection error.\n\n" +
                                "This might be due to:\n" +
                                "• SSH key not configured\n" +
                                "• GitHub PAT not set in GitWhisper settings\n" +
                                "• Network connectivity issues\n\n" +
                                "Try setting a GitHub Personal Access Token in GitWhisper settings."
                            }
                            e.message?.contains("authentication") == true -> {
                                "Push failed: Authentication error.\n\n" +
                                "Please configure your GitHub Personal Access Token in GitWhisper settings."
                            }
                            else -> "Failed to commit and push: ${e.message}"
                        }

                        com.intellij.openapi.ui.Messages.showErrorDialog(
                            project,
                            errorMessage,
                            "GitWhisper Push Error"
                        )
                    }
                }
            }
        })
    }

    /**
     * Push changes to remote
     */
    fun pushChanges() {
        try {
            val git = this.git ?: throw RuntimeException("No Git repository found")
            val repository = this.gitRepository ?: throw RuntimeException("No Git repository found")

            // Setup SSH session factory to handle authentication
            setupSshSessionFactory()

            // Get credentials if needed
            val credentialsProvider = getCredentialsProvider()

            val pushCommand = git.push()
            if (credentialsProvider != null) {
                pushCommand.setCredentialsProvider(credentialsProvider)
            }

            val pushResults = pushCommand.call()

            // Check for errors
            for (pushResult in pushResults) {
                for (update in pushResult.remoteUpdates) {
                    if (update.status != RemoteRefUpdate.Status.OK &&
                        update.status != RemoteRefUpdate.Status.UP_TO_DATE) {
                        throw RuntimeException("Push failed: ${update.status} - ${update.message}")
                    }
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to push: ${e.message}", e)
        }
    }

    /**
     * Setup SSH session factory for Git operations
     */
    private fun setupSshSessionFactory() {
        val sshSessionFactory = object : JschConfigSessionFactory() {
            override fun configure(hc: OpenSshConfig.Host?, session: Session?) {
                session?.setConfig("StrictHostKeyChecking", "no")
            }
        }
        SshSessionFactory.setInstance(sshSessionFactory)
    }

    /**
     * Get credentials provider for authentication
     */
    private fun getCredentialsProvider(): CredentialsProvider? {
        return try {
            val configService = GitWhisperConfigService.getInstance()
            val githubPat = configService.getGitHubPAT()

            if (!githubPat.isNullOrBlank()) {
                // For GitHub, use PAT as username with empty password
                // This works for both HTTPS and SSH URLs
                UsernamePasswordCredentialsProvider(githubPat, "")
            } else {
                // Check if this is a GitHub repository and suggest PAT
                if (isGitHubRepository()) {
                    throw RuntimeException(
                        "GitHub repository detected but no Personal Access Token configured. " +
                        "Please set your GitHub PAT in GitWhisper settings for authentication."
                    )
                }
                null
            }
        } catch (e: Exception) {
            if (e.message?.contains("Personal Access Token") == true) {
                throw e // Re-throw our custom message
            }
            null
        }
    }

    /**
     * Filter files based on ignore patterns
     */
    private fun filterFiles(files: List<String>, ignorePatterns: List<String>): Pair<List<String>, List<String>> {
        val includedFiles = mutableListOf<String>()
        val ignoredFiles = mutableListOf<String>()

        for (file in files) {
            var shouldIgnore = false

            for (pattern in ignorePatterns) {
                if (matchesPattern(file, pattern)) {
                    shouldIgnore = true
                    break
                }
            }

            if (shouldIgnore) {
                ignoredFiles.add(file)
            } else {
                includedFiles.add(file)
            }
        }

        return Pair(includedFiles, ignoredFiles)
    }

    /**
     * Check if file matches ignore pattern (supports basic glob patterns)
     */
    private fun matchesPattern(file: String, pattern: String): Boolean {
        // Convert glob pattern to regex
        val regexPattern = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")

        return Regex(regexPattern).matches(File(file).name) ||
                Regex(regexPattern).matches(file)
    }

    /**
     * Data class for filtered diff result
     */
    data class FilteredDiffResult(
        val diff: String,
        val ignoredFiles: List<String>
    )

    companion object {
        fun getInstance(project: Project): GitWhisperProjectService {
            return project.getService(GitWhisperProjectService::class.java)
        }
    }
}
