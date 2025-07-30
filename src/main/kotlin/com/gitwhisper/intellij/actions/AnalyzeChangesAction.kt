package com.gitwhisper.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.testFramework.LightVirtualFile
import com.gitwhisper.intellij.GitWhisperBundle
import com.gitwhisper.intellij.models.CommitGeneratorFactory
import com.gitwhisper.intellij.models.ModelVariants
import com.gitwhisper.intellij.services.GitWhisperConfigService
import com.gitwhisper.intellij.services.GitWhisperProjectService
import com.gitwhisper.intellij.ui.NoStagedChangesDialog
import com.gitwhisper.intellij.utils.ApiException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * Action to analyze staged changes with AI
 * Based on the analyzeChanges function from the reference implementation
 */
class AnalyzeChangesAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            GitWhisperBundle.message("message.analyzing.changes"),
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val future = CompletableFuture<Unit>()

                    ApplicationManager.getApplication().executeOnPooledThread {
                        try {
                            analyzeChanges(project, indicator)
                            future.complete(Unit)
                        } catch (e: Exception) {
                            future.completeExceptionally(e)
                        }
                    }

                    future.get()
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "Error: ${e.message}",
                            GitWhisperBundle.message("notification.error.title")
                        )
                    }
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val gitService = project?.let { GitWhisperProjectService.getInstance(it) }
        
        e.presentation.isEnabled = project != null && 
                                  gitService?.hasGitRepository() == true
    }

    private fun analyzeChanges(project: Project, indicator: ProgressIndicator) {
        try {
            val configService = GitWhisperConfigService.getInstance()
            val gitService = GitWhisperProjectService.getInstance(project)

            // Check if we're in a git repository
            if (!gitService.hasGitRepository()) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        GitWhisperBundle.message("message.no.git.repository"),
                        GitWhisperBundle.message("notification.error.title")
                    )
                }
                return
            }

            // Check for staged changes
            val hasStagedChanges = gitService.hasStagedChanges()
            if (!hasStagedChanges) {
                // Check if there are unstaged changes and offer to stage them
                val hasUnstagedChanges = gitService.hasUnstagedChanges()
                if (hasUnstagedChanges) {
                    // Show custom dialog with staging options
                    val choice = CompletableFuture<NoStagedChangesDialog.UserChoice>()
                    ApplicationManager.getApplication().invokeLater {
                        val result = NoStagedChangesDialog.showDialog(project, gitService)
                        choice.complete(result)
                    }
                    val choiceResult = choice.get()

                    when (choiceResult) {
                        NoStagedChangesDialog.UserChoice.STAGE_ALL_AND_CONTINUE -> {
                            indicator.text = "Staging all changes..."
                            val stagedCount = gitService.stageAllUnstagedFiles()
                            ApplicationManager.getApplication().invokeLater {
                                Messages.showInfoMessage(
                                    project,
                                    "$stagedCount files staged.",
                                    GitWhisperBundle.message("plugin.name")
                                )
                            }
                        }
                        NoStagedChangesDialog.UserChoice.OPEN_GIT_TOOL_WINDOW -> {
                            ApplicationManager.getApplication().invokeLater {
                                val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                                // Try multiple possible tool window names
                                val vcsToolWindow = toolWindowManager.getToolWindow("Version Control")
                                    ?: toolWindowManager.getToolWindow("Git")
                                    ?: toolWindowManager.getToolWindow("VCS")

                                if (vcsToolWindow != null) {
                                    vcsToolWindow.activate(null)
                                } else {
                                    // Fallback: open VCS operations popup
                                    val actionManager = com.intellij.openapi.actionSystem.ActionManager.getInstance()
                                    val vcsAction = actionManager.getAction("Vcs.Operations.Popup")
                                    if (vcsAction != null) {
                                        val event = com.intellij.openapi.actionSystem.AnActionEvent.createFromDataContext(
                                            com.intellij.openapi.actionSystem.ActionPlaces.UNKNOWN,
                                            null,
                                            com.intellij.openapi.actionSystem.impl.SimpleDataContext.getProjectContext(project)
                                        )
                                        vcsAction.actionPerformed(event)
                                    }
                                }
                            }
                            return
                        }
                        NoStagedChangesDialog.UserChoice.CANCEL -> return
                    }
                } else {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            GitWhisperBundle.message("message.no.staged.changes"),
                            GitWhisperBundle.message("notification.error.title")
                        )
                    }
                    return
                }
            }

            // Get model configuration
            val (modelName, variant, apiKey) = getModelConfiguration(configService, project)
                ?: return

            // Get staged diff with file filtering
            indicator.text = "Getting staged changes..."
            val ignorePatterns = configService.getIgnoredFiles()
            val diffResult = gitService.getFilteredStagedDiff(ignorePatterns)
            val diff = diffResult.diff

            if (diff.isBlank()) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "No changes detected in staged files.",
                        GitWhisperBundle.message("notification.error.title")
                    )
                }
                return
            }

            // Show filtering summary if files were ignored
            if (diffResult.ignoredFiles.isNotEmpty()) {
                val message = "Ignored ${diffResult.ignoredFiles.size} file(s): ${
                    diffResult.ignoredFiles.take(3).joinToString(", ")
                }${if (diffResult.ignoredFiles.size > 3) " and ${diffResult.ignoredFiles.size - 3} more" else ""}"
                
                ApplicationManager.getApplication().invokeLater {
                    Messages.showInfoMessage(project, message, GitWhisperBundle.message("plugin.name"))
                }
            }

            // Analyze changes
            indicator.text = "Analyzing changes using $modelName..."
            
            try {
                val options = mutableMapOf<String, Any>()
                options["variant"] = variant
                if (modelName == "ollama") {
                    options["baseUrl"] = configService.getOllamaBaseUrl()
                }
                val generator = CommitGeneratorFactory.create(modelName, apiKey, options)
                
                val language = configService.getLanguage()

                // Generate analysis in background thread
                val executor = Executors.newSingleThreadExecutor()
                val future = CompletableFuture.supplyAsync({
                    generator.analyzeChanges(diff, language)
                }, executor)

                val analysis = future.get()

                // Show analysis in a new document
                ApplicationManager.getApplication().invokeLater {
                    showAnalysisResult(project, analysis)
                }

            } catch (e: ApiException) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        e.getUserMessage(),
                        GitWhisperBundle.message("notification.error.title")
                    )
                }
            }

        } catch (e: Exception) {
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(
                    project,
                    "Error: ${e.message}",
                    GitWhisperBundle.message("notification.error.title")
                )
            }
        }
    }

    private fun getModelConfiguration(
        configService: GitWhisperConfigService,
        project: Project
    ): Triple<String, String, String?>? {
        // Get default model and variant from config
        val (defaultModel, defaultVariant) = configService.getDefaultModelAndVariant()
        var modelName = defaultModel
        var variant = defaultVariant

        // If no defaults, prompt user to select
        if (modelName.isBlank()) {
            val implementedModels = CommitGeneratorFactory.getImplementedModels()
            modelName = implementedModels.firstOrNull() ?: return null
            variant = ModelVariants.getDefaultVariant(modelName)
        }

        // Get API key
        var apiKey = configService.getApiKey(modelName)
        if (apiKey.isNullOrBlank()) {
            apiKey = configService.getEnvironmentApiKey(modelName)
        }

        // If no API key and model requires one, prompt user
        val requirements = CommitGeneratorFactory.getModelRequirements(modelName)
        if (apiKey.isNullOrBlank() && requirements.requiresApiKey) {
            val keyFuture = CompletableFuture<String?>()
            ApplicationManager.getApplication().invokeLater {
                val result = Messages.showPasswordDialog(
                    "Enter API key for $modelName:",
                    GitWhisperBundle.message("plugin.name")
                )
                keyFuture.complete(result)
            }
            val inputKey = keyFuture.get()

            if (!inputKey.isNullOrBlank()) {
                configService.setApiKey(modelName, inputKey)
                apiKey = inputKey
            } else {
                return null
            }
        }

        // For models that don't require API key, ensure we still have a value
        if (!requirements.requiresApiKey && apiKey.isNullOrBlank()) {
            apiKey = "" // Set empty string for models that don't need API key
        }

        return Triple(modelName, variant, apiKey)
    }

    private fun showAnalysisResult(project: Project, analysis: String) {
        try {
            // Create a virtual file with the analysis content
            val virtualFile = LightVirtualFile("GitWhisper_Analysis.md", analysis)
            virtualFile.isWritable = false
            
            // Open the file in the editor
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
            
            Messages.showInfoMessage(
                project,
                GitWhisperBundle.message("notification.analysis.success"),
                GitWhisperBundle.message("plugin.name")
            )
        } catch (e: Exception) {
            // Fallback: show in a dialog
            Messages.showInfoMessage(
                project,
                analysis,
                GitWhisperBundle.message("dialog.analysis.title")
            )
        }
    }
}
