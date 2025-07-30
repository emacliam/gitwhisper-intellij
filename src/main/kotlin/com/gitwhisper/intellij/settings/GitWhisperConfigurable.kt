package com.gitwhisper.intellij.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.*
import com.intellij.ui.layout.panel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.openapi.ui.Messages
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JPanel
import java.awt.BorderLayout as AWTBorderLayout
import com.gitwhisper.intellij.GitWhisperBundle
import com.gitwhisper.intellij.models.CommitGeneratorFactory
import com.gitwhisper.intellij.models.Languages
import com.gitwhisper.intellij.models.ModelVariants
import com.gitwhisper.intellij.services.GitWhisperConfigService
import java.awt.BorderLayout
import javax.swing.*

/**
 * Settings configurable for GitWhisper plugin
 */
class GitWhisperConfigurable : Configurable {

    private val configService = GitWhisperConfigService.getInstance()
    
    // UI Components
    private val modelComboBox = ComboBox<String>()
    private val variantComboBox = ComboBox<String>()
    private val languageComboBox = ComboBox<String>()
    private val alwaysAddCheckBox = JBCheckBox("Automatically stage unstaged changes")
    private val autoPushCheckBox = JBCheckBox("Automatically push commits after creation")
    private val ollamaUrlField = JBTextField()
    private val customOllamaVariantField = JBTextField()

    // Ignore patterns
    private val ignorePatternsArea = JBTextArea()
    private lateinit var addPatternButton: JButton
    private lateinit var removePatternButton: JButton
    
    // API Key fields
    private val apiKeyFields = mutableMapOf<String, JBPasswordField>()
    private val removeKeyButtons = mutableMapOf<String, JButton>()
    private val githubPatField = JBPasswordField()
    private lateinit var removeGithubPatButton: JButton
    
    private var mainPanel: JPanel? = null

    override fun getDisplayName(): String {
        return GitWhisperBundle.message("settings.title")
    }

    override fun createComponent(): JComponent {
        if (mainPanel == null) {
            mainPanel = createMainPanel()
        }
        return mainPanel!!
    }

    private fun createMainPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // Create tabbed pane
        val tabbedPane = JBTabbedPane()
        
        // General settings tab
        tabbedPane.addTab("General", createGeneralPanel())
        
        // API Keys tab
        tabbedPane.addTab("API Keys", createApiKeysPanel())

        // File Filters tab
        tabbedPane.addTab("File Filters", createIgnorePatternsPanel())

        // Advanced tab
        tabbedPane.addTab("Advanced", createAdvancedPanel())
        
        panel.add(tabbedPane, BorderLayout.CENTER)
        
        return panel
    }

    private fun createGeneralPanel(): JPanel {
        // Setup model combo box
        val implementedModels = CommitGeneratorFactory.getImplementedModels()
        implementedModels.forEach { modelComboBox.addItem(it) }
        
        modelComboBox.addActionListener {
            updateVariantComboBox()
        }
        
        // Setup language combo box
        val languages = Languages.getAllLanguages()
        languages.forEach { languageComboBox.addItem(it.displayName) }
        
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Default AI Model:", modelComboBox)
            .addLabeledComponent("Model Variant:", variantComboBox)
            .addLabeledComponent("Commit Language:", languageComboBox)
            .addComponent(alwaysAddCheckBox)
            .addComponent(autoPushCheckBox)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun createApiKeysPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        // Add API key fields for each model
        val models = CommitGeneratorFactory.getAvailableModels()
        for (model in models) {
            val requirements = CommitGeneratorFactory.getModelRequirements(model)
            if (requirements.requiresApiKey) {
                val field = JBPasswordField()
                apiKeyFields[model] = field

                val removeButton = JButton("Remove")
                removeKeyButtons[model] = removeButton

                removeButton.addActionListener {
                    field.text = ""
                    configService.removeApiKey(model)
                }

                val keyPanel = JPanel(AWTBorderLayout())
                keyPanel.add(field, AWTBorderLayout.CENTER)
                keyPanel.add(removeButton, AWTBorderLayout.EAST)

                val label = JBLabel("$model API Key:")
                val formPanel = FormBuilder.createFormBuilder()
                    .addLabeledComponent(label, keyPanel)
                    .panel

                panel.add(formPanel)
            }
        }

        // GitHub PAT
        removeGithubPatButton = JButton("Remove")
        removeGithubPatButton.addActionListener {
            githubPatField.text = ""
            configService.removeGitHubPAT()
        }

        val githubKeyPanel = JPanel(AWTBorderLayout())
        githubKeyPanel.add(githubPatField, AWTBorderLayout.CENTER)
        githubKeyPanel.add(removeGithubPatButton, AWTBorderLayout.EAST)

        val githubFormPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("GitHub Personal Access Token:", githubKeyPanel)
            .panel
        panel.add(githubFormPanel)
        
        return panel
    }

    private fun createIgnorePatternsPanel(): JPanel {
        val panel = JPanel(AWTBorderLayout())

        // Description
        val descLabel = JBLabel(
            "<html>Specify file patterns to ignore when generating commit messages.<br>" +
            "Use glob patterns like *.log, package-lock.json, etc.<br>" +
            "One pattern per line.</html>"
        )
        descLabel.border = JBUI.Borders.empty(10)

        // Text area for patterns
        ignorePatternsArea.rows = 10
        ignorePatternsArea.columns = 40
        val scrollPane = JBScrollPane(ignorePatternsArea)
        scrollPane.preferredSize = Dimension(400, 200)

        // Buttons panel
        val buttonsPanel = JPanel()
        addPatternButton = JButton("Add Pattern")
        removePatternButton = JButton("Reset to Defaults")

        addPatternButton.addActionListener {
            val pattern = Messages.showInputDialog(
                panel,
                "Enter file pattern to ignore:",
                "Add Ignore Pattern",
                Messages.getQuestionIcon()
            )
            if (!pattern.isNullOrBlank()) {
                val currentText = ignorePatternsArea.text
                val newText = if (currentText.isBlank()) {
                    pattern
                } else {
                    "$currentText\n$pattern"
                }
                ignorePatternsArea.text = newText
            }
        }

        removePatternButton.addActionListener {
            val defaultPatterns = listOf(
                "package-lock.json",
                "yarn.lock",
                "pnpm-lock.yaml",
                "*.log",
                "*.tmp",
                "*.temp",
                ".DS_Store",
                "Thumbs.db"
            )
            ignorePatternsArea.text = defaultPatterns.joinToString("\n")
        }

        buttonsPanel.add(addPatternButton)
        buttonsPanel.add(removePatternButton)

        panel.add(descLabel, AWTBorderLayout.NORTH)
        panel.add(scrollPane, AWTBorderLayout.CENTER)
        panel.add(buttonsPanel, AWTBorderLayout.SOUTH)

        return panel
    }

    private fun createAdvancedPanel(): JPanel {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Ollama Base URL:", ollamaUrlField)
            .addLabeledComponent("Custom Ollama Variant:", customOllamaVariantField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun updateVariantComboBox() {
        val selectedModel = modelComboBox.selectedItem as? String ?: return
        
        variantComboBox.removeAllItems()
        val variants = ModelVariants.getVariantsWithCustom(
            selectedModel, 
            configService.getCustomOllamaVariant()
        )
        
        variants.forEach { variant ->
            variantComboBox.addItem(variant.displayName)
        }
        
        // Select default variant
        val defaultVariant = ModelVariants.getDefaultVariant(selectedModel)
        val defaultVariantDisplay = variants.find { it.name == defaultVariant }?.displayName
        if (defaultVariantDisplay != null) {
            variantComboBox.selectedItem = defaultVariantDisplay
        }
    }

    override fun isModified(): Boolean {
        val currentModel = configService.getDefaultModel()
        val currentVariant = configService.getDefaultVariant()
        val currentLanguage = configService.getLanguage()
        val currentAlwaysAdd = configService.shouldAlwaysAdd()
        val currentAutoPush = configService.shouldAutoPush()
        val currentOllamaUrl = configService.getOllamaBaseUrl()
        val currentCustomOllama = configService.getCustomOllamaVariant()
        val currentIgnorePatterns = configService.getIgnorePatterns()
        val newIgnorePatterns = ignorePatternsArea.text.split("\n").map { it.trim() }.filter { it.isNotBlank() }

        return (modelComboBox.selectedItem as? String) != currentModel ||
               getSelectedVariantName() != currentVariant ||
               getSelectedLanguage()?.displayName != currentLanguage.displayName ||
               alwaysAddCheckBox.isSelected != currentAlwaysAdd ||
               autoPushCheckBox.isSelected != currentAutoPush ||
               ollamaUrlField.text != currentOllamaUrl ||
               customOllamaVariantField.text != currentCustomOllama ||
               newIgnorePatterns != currentIgnorePatterns ||
               hasApiKeyChanges()
    }

    private fun hasApiKeyChanges(): Boolean {
        // Check if any API key field has content (indicating user wants to set a key)
        for ((_, field) in apiKeyFields) {
            val newKey = String(field.password)
            if (newKey.isNotBlank() && newKey != "••••••••") {
                return true
            }
        }

        val newPat = String(githubPatField.password)
        return newPat.isNotBlank() && newPat != "••••••••"
    }

    override fun apply() {
        // Save general settings
        val selectedModel = modelComboBox.selectedItem as? String
        val selectedVariant = getSelectedVariantName()
        
        if (selectedModel != null && selectedVariant != null) {
            configService.setDefaults(selectedModel, selectedVariant)
        }
        
        val selectedLanguage = getSelectedLanguage()
        if (selectedLanguage != null) {
            configService.setLanguage(selectedLanguage)
        }
        
        configService.setShouldAlwaysAdd(alwaysAddCheckBox.isSelected)
        configService.setShouldAutoPush(autoPushCheckBox.isSelected)
        configService.setOllamaBaseUrl(ollamaUrlField.text)
        configService.setCustomOllamaVariant(customOllamaVariantField.text)

        // Save ignore patterns
        val ignorePatterns = ignorePatternsArea.text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        configService.setIgnorePatterns(ignorePatterns)
        
        // Save API keys
        for ((model, field) in apiKeyFields) {
            val apiKey = String(field.password)
            if (apiKey.isNotBlank()) {
                configService.setApiKey(model, apiKey)
            }
        }
        
        val githubPat = String(githubPatField.password)
        if (githubPat.isNotBlank()) {
            configService.setGitHubPAT(githubPat)
        }
    }

    override fun reset() {
        // Load current settings
        val currentModel = configService.getDefaultModel()
        val currentVariant = configService.getDefaultVariant()
        val currentLanguage = configService.getLanguage()
        
        modelComboBox.selectedItem = currentModel
        updateVariantComboBox()
        
        // Select current variant
        val variants = ModelVariants.getVariantsWithCustom(currentModel, configService.getCustomOllamaVariant())
        val currentVariantDisplay = variants.find { it.name == currentVariant }?.displayName
        if (currentVariantDisplay != null) {
            variantComboBox.selectedItem = currentVariantDisplay
        }
        
        languageComboBox.selectedItem = currentLanguage.displayName
        alwaysAddCheckBox.isSelected = configService.shouldAlwaysAdd()
        autoPushCheckBox.isSelected = configService.shouldAutoPush()
        ollamaUrlField.text = configService.getOllamaBaseUrl()
        customOllamaVariantField.text = configService.getCustomOllamaVariant()

        // Load ignore patterns
        val ignorePatterns = configService.getIgnorePatterns()
        ignorePatternsArea.text = ignorePatterns.joinToString("\n")
        
        // Load API keys asynchronously to avoid EDT violations
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val apiKeyStates = mutableMapOf<String, String>()
                for ((model, _) in apiKeyFields) {
                    val hasKey = configService.hasApiKey(model)
                    apiKeyStates[model] = if (hasKey) "••••••••" else ""
                }
                val githubPatText = if (configService.getGitHubPAT()?.isNotBlank() == true) "••••••••" else ""

                ApplicationManager.getApplication().invokeLater {
                    for ((model, field) in apiKeyFields) {
                        val currentText = field.text
                        val expectedText = apiKeyStates[model] ?: ""
                        // Only update if the field is empty or shows placeholder
                        if (currentText.isEmpty() || currentText == "••••••••") {
                            field.text = expectedText
                        }
                    }

                    val currentGithubText = githubPatField.text
                    if (currentGithubText.isEmpty() || currentGithubText == "••••••••") {
                        githubPatField.text = githubPatText
                    }
                }
            } catch (e: Exception) {
                // Ignore errors when loading API keys in reset
            }
        }
    }

    private fun getSelectedVariantName(): String? {
        val selectedModel = modelComboBox.selectedItem as? String ?: return null
        val selectedVariantDisplay = variantComboBox.selectedItem as? String ?: return null
        
        val variants = ModelVariants.getVariantsWithCustom(selectedModel, configService.getCustomOllamaVariant())
        return variants.find { it.displayName == selectedVariantDisplay }?.name
    }

    private fun getSelectedLanguage() = Languages.findByDisplayName(languageComboBox.selectedItem as? String ?: "")
}
