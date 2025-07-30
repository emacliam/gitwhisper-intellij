package com.gitwhisper.intellij.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import com.gitwhisper.intellij.GitWhisperBundle
import com.gitwhisper.intellij.services.GitWhisperConfigService
import com.gitwhisper.intellij.services.GitWhisperProjectService
import java.awt.event.MouseEvent

/**
 * Status bar widget factory for GitWhisper
 */
class GitWhisperStatusBarWidgetFactory : StatusBarWidgetFactory {

    override fun getId(): String = "GitWhisperStatusBar"

    override fun getDisplayName(): String = GitWhisperBundle.message("plugin.name")

    override fun isAvailable(project: Project): Boolean {
        val gitService = GitWhisperProjectService.getInstance(project)
        return gitService.hasGitRepository()
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return GitWhisperStatusBarWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        // Nothing to dispose
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}

/**
 * Status bar widget for GitWhisper
 */
class GitWhisperStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.TextPresentation {

    private val configService = GitWhisperConfigService.getInstance()
    private val gitService = GitWhisperProjectService.getInstance(project)

    // Cache status to avoid EDT violations
    private var cachedText: String = "GitWhisper"
    private var cachedTooltip: String = "GitWhisper"
    private var lastUpdateTime: Long = 0
    private val updateInterval = 5000L // 5 seconds

    override fun ID(): String = "GitWhisperStatusBar"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) {
        updateStatusAsync()
    }

    override fun dispose() {
        // Nothing to dispose
    }

    override fun getText(): String {
        // Update status if needed
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime > updateInterval) {
            updateStatusAsync()
        }
        return cachedText
    }

    override fun getAlignment(): Float = 0.5f

    override fun getTooltipText(): String {
        return cachedTooltip
    }

    override fun getClickConsumer(): Consumer<MouseEvent>? {
        return Consumer { _ ->
            // Open configuration or trigger action based on status
            when {
                !gitService.hasGitRepository() -> {
                    // Do nothing for no git repo
                }
                configService.getDefaultModel().isBlank() || !configService.hasApiKey(configService.getDefaultModel()) -> {
                    // Open configuration
                    com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(
                        project,
                        com.gitwhisper.intellij.settings.GitWhisperConfigurable::class.java
                    )
                }
                else -> {
                    // Trigger generate commit action
                    val actionManager = com.intellij.openapi.actionSystem.ActionManager.getInstance()
                    val action = actionManager.getAction("GitWhisper.GenerateCommit")
                    if (action != null) {
                        val event = com.intellij.openapi.actionSystem.AnActionEvent.createFromDataContext(
                            com.intellij.openapi.actionSystem.ActionPlaces.STATUS_BAR_PLACE,
                            null,
                            com.intellij.openapi.actionSystem.impl.SimpleDataContext.getProjectContext(project)
                        )
                        action.actionPerformed(event)
                    }
                }
            }
        }
    }

    private fun updateStatusAsync() {
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val hasGit = gitService.hasGitRepository()
                val hasStagedChanges = if (hasGit) gitService.hasStagedChanges() else false
                val model = configService.getDefaultModel()
                val hasApiKey = if (model.isNotBlank()) {
                    try {
                        configService.hasApiKey(model)
                    } catch (e: Exception) {
                        false
                    }
                } else false

                val newText = when {
                    !hasGit -> "No Git"
                    !hasStagedChanges -> "GitWhisper: No Changes"
                    model.isBlank() -> "GitWhisper: No Model"
                    !hasApiKey -> "GitWhisper: No API Key"
                    else -> "GitWhisper: Ready"
                }

                val newTooltip = when {
                    !hasGit -> "No Git repository found"
                    !hasStagedChanges -> "No staged changes to analyze"
                    model.isBlank() -> "No default AI model configured. Click to configure."
                    !hasApiKey -> "No API key for $model. Click to configure."
                    else -> "GitWhisper is ready. Click to generate commit message."
                }

                // Update cache
                cachedText = newText
                cachedTooltip = newTooltip
                lastUpdateTime = System.currentTimeMillis()

            } catch (e: Exception) {
                cachedText = "GitWhisper: Error"
                cachedTooltip = "GitWhisper error: ${e.message}"
                lastUpdateTime = System.currentTimeMillis()
            }
        }
    }
}
