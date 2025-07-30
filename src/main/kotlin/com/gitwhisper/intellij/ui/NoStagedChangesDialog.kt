package com.gitwhisper.intellij.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.gitwhisper.intellij.GitWhisperBundle
import com.gitwhisper.intellij.services.GitWhisperProjectService
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * Custom dialog for handling "No staged changes" scenario
 * Provides options to stage changes and proceed
 */
class NoStagedChangesDialog(
    private val project: Project,
    private val gitService: GitWhisperProjectService
) : DialogWrapper(project) {

    private var userChoice: UserChoice = UserChoice.CANCEL
    private var stagedFilesInfo: String = ""

    enum class UserChoice {
        STAGE_ALL_AND_CONTINUE,
        OPEN_GIT_TOOL_WINDOW,
        CANCEL
    }

    init {
        title = GitWhisperBundle.message("plugin.name")
        setOKButtonText("Stage All and Continue")
        setCancelButtonText("Cancel")

        // Get unstaged files info BEFORE calling init()
        updateUnstagedFilesInfo()

        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(500, 300)
        
        // Main message
        val messageLabel = JBLabel(
            "<html><b>No staged changes found</b><br><br>" +
            "GitWhisper needs staged changes to generate a commit message. " +
            "You can stage all changes automatically or open the Git tool window to select specific files.</html>"
        )
        messageLabel.border = JBUI.Borders.empty(10)
        
        // Unstaged files info
        val filesInfoArea = JBTextArea()
        filesInfoArea.isEditable = false
        filesInfoArea.text = stagedFilesInfo
        filesInfoArea.background = panel.background
        filesInfoArea.border = JBUI.Borders.empty(5)
        
        val scrollPane = JBScrollPane(filesInfoArea)
        scrollPane.preferredSize = Dimension(480, 150)
        scrollPane.border = JBUI.Borders.compound(
            JBUI.Borders.emptyTop(10),
            JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
        )
        
        panel.add(messageLabel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }

    override fun createActions(): Array<Action> {
        val stageAllAction = object : AbstractAction("Stage All and Continue") {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                userChoice = UserChoice.STAGE_ALL_AND_CONTINUE
                close(OK_EXIT_CODE)
            }
        }
        
        val openGitToolAction = object : AbstractAction("Open Version Control") {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                userChoice = UserChoice.OPEN_GIT_TOOL_WINDOW
                close(OK_EXIT_CODE)
            }
        }
        
        val cancelAction = object : AbstractAction("Cancel") {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                userChoice = UserChoice.CANCEL
                close(CANCEL_EXIT_CODE)
            }
        }
        
        return arrayOf(stageAllAction, openGitToolAction, cancelAction)
    }

    private fun updateUnstagedFilesInfo() {
        stagedFilesInfo = try {
            val status = gitService.getStatus()
            if (status != null) {
                val unstagedFiles = mutableListOf<String>()

                // Modified files
                if (status.modified.isNotEmpty()) {
                    unstagedFiles.add("Modified files:")
                    status.modified.sorted().forEach { file ->
                        unstagedFiles.add("  M $file")
                    }
                    unstagedFiles.add("")
                }

                // Untracked files
                if (status.untracked.isNotEmpty()) {
                    unstagedFiles.add("Untracked files:")
                    status.untracked.sorted().forEach { file ->
                        unstagedFiles.add("  ? $file")
                    }
                    unstagedFiles.add("")
                }

                // Missing files
                if (status.missing.isNotEmpty()) {
                    unstagedFiles.add("Missing files:")
                    status.missing.sorted().forEach { file ->
                        unstagedFiles.add("  D $file")
                    }
                    unstagedFiles.add("")
                }

                if (unstagedFiles.isNotEmpty()) {
                    "Unstaged changes found:\n\n" + unstagedFiles.joinToString("\n")
                } else {
                    "No unstaged changes found.\n\nThe repository appears to be clean."
                }
            } else {
                "Unable to read Git status.\n\nPlease ensure this is a valid Git repository."
            }
        } catch (e: Exception) {
            "Error reading Git status: ${e.message}\n\nPlease check your Git repository."
        }
    }

    /**
     * Get the user's choice
     */
    fun getUserChoice(): UserChoice = userChoice

    /**
     * Show the dialog and return the user's choice
     */
    companion object {
        fun showDialog(project: Project, gitService: GitWhisperProjectService): UserChoice {
            val dialog = NoStagedChangesDialog(project, gitService)
            dialog.show()
            return dialog.getUserChoice()
        }
    }
}
