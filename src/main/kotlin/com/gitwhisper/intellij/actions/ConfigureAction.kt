package com.gitwhisper.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.gitwhisper.intellij.settings.GitWhisperConfigurable

/**
 * Action to open GitWhisper configuration
 */
class ConfigureAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        ShowSettingsUtil.getInstance().showSettingsDialog(
            project,
            GitWhisperConfigurable::class.java
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
