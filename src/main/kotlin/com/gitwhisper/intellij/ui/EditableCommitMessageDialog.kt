package com.gitwhisper.intellij.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.gitwhisper.intellij.GitWhisperBundle
import com.gitwhisper.intellij.services.GitWhisperConfigService
import com.gitwhisper.intellij.services.GitWhisperProjectService
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * Dialog for editing commit message before committing
 */
class EditableCommitMessageDialog(
    private val project: Project,
    private val initialMessage: String,
    private val gitService: GitWhisperProjectService
) : DialogWrapper(project) {

    private var userChoice: UserChoice = UserChoice.CANCEL
    private lateinit var messageArea: JBTextArea

    enum class UserChoice {
        COMMIT_ONLY,
        COMMIT_AND_PUSH,
        CANCEL
    }

    init {
        title = "Edit Commit Message"
        setOKButtonText("Commit Only")
        setCancelButtonText("Cancel")
        
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(600, 400)
        
        // Header
        val headerLabel = JBLabel(
            "<html><b>Edit your commit message</b><br>" +
            "You can modify the AI-generated message before committing.</html>"
        )
        headerLabel.border = JBUI.Borders.empty(10)
        
        // Message area
        messageArea = JBTextArea()
        messageArea.text = initialMessage
        messageArea.lineWrap = true
        messageArea.wrapStyleWord = true
        messageArea.rows = 15
        messageArea.columns = 60
        
        val scrollPane = JBScrollPane(messageArea)
        scrollPane.preferredSize = Dimension(580, 300)
        scrollPane.border = JBUI.Borders.compound(
            JBUI.Borders.emptyTop(10),
            JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
        )
        
        // Tips
        val tipsLabel = JBLabel(
            "<html><i>Tips:</i><br>" +
            "• Keep the first line under 50 characters<br>" +
            "• Use imperative mood (\"Add feature\" not \"Added feature\")<br>" +
            "• Separate subject from body with a blank line</html>"
        )
        tipsLabel.border = JBUI.Borders.empty(10)
        
        panel.add(headerLabel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        panel.add(tipsLabel, BorderLayout.SOUTH)
        
        return panel
    }

    override fun createActions(): Array<Action> {
        val configService = GitWhisperConfigService.getInstance()
        val shouldAutoPush = configService.shouldAutoPush()
        val isGitHubRepo = gitService.isGitHubRepository()
        val hasGitHubPat = configService.getGitHubPAT()?.isNotBlank() == true
        
        val commitOnlyAction = object : AbstractAction("Commit Only") {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                userChoice = UserChoice.COMMIT_ONLY
                close(OK_EXIT_CODE)
            }
        }
        
        val commitAndPushAction = object : AbstractAction("Commit And Push") {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                userChoice = UserChoice.COMMIT_AND_PUSH
                close(OK_EXIT_CODE)
            }
        }
        
        val cancelAction = object : AbstractAction("Cancel") {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                userChoice = UserChoice.CANCEL
                close(CANCEL_EXIT_CODE)
            }
        }
        
        // Show warning if trying to push without proper auth
        if (isGitHubRepo && !hasGitHubPat) {
            commitAndPushAction.putValue(Action.NAME, "Commit And Push (⚠️ No GitHub PAT)")
        }
        
        return if (shouldAutoPush) {
            arrayOf(commitAndPushAction, commitOnlyAction, cancelAction)
        } else {
            arrayOf(commitOnlyAction, commitAndPushAction, cancelAction)
        }
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return messageArea
    }

    /**
     * Get the edited commit message
     */
    fun getCommitMessage(): String = messageArea.text.trim()

    /**
     * Get the user's choice
     */
    fun getUserChoice(): UserChoice = userChoice

    /**
     * Show the dialog and return the result
     */
    companion object {
        fun showDialog(
            project: Project, 
            initialMessage: String,
            gitService: GitWhisperProjectService
        ): Pair<UserChoice, String> {
            val dialog = EditableCommitMessageDialog(project, initialMessage, gitService)
            dialog.show()
            return Pair(dialog.getUserChoice(), dialog.getCommitMessage())
        }
    }
}
