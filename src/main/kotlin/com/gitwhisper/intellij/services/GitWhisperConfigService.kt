package com.gitwhisper.intellij.services

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil
import com.gitwhisper.intellij.models.Language
import com.gitwhisper.intellij.models.Languages

/**
 * Configuration service for GitWhisper plugin
 * Manages settings and secure storage of API keys
 */
@Service(Service.Level.APP)
@State(
    name = "GitWhisperConfig",
    storages = [Storage("gitwhisper.xml")]
)
class GitWhisperConfigService : PersistentStateComponent<GitWhisperConfigService.State> {

    data class State(
        var defaultModel: String = "openai",
        var defaultVariant: String = "gpt-4o",
        var language: String = "en;US",
        var alwaysAdd: Boolean = false,
        var autoPush: Boolean = false,
        var ollamaBaseUrl: String = "http://localhost:11434",
        var customOllamaVariant: String = "",
        var ignoredFiles: MutableList<String> = mutableListOf(
            "package-lock.json",
            "yarn.lock",
            "pnpm-lock.yaml",
            "*.log",
            "*.tmp",
            "*.temp",
            ".DS_Store",
            "Thumbs.db"
        )
    )

    private var myState = State()

    companion object {
        fun getInstance(): GitWhisperConfigService {
            return ApplicationManager.getApplication().getService(GitWhisperConfigService::class.java)
        }

        private const val CREDENTIAL_SERVICE_NAME = "GitWhisper"
    }

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    // API Key Management
    fun setApiKey(modelName: String, apiKey: String) {
        val credentialAttributes = createCredentialAttributes(modelName)
        val credentials = Credentials(modelName, apiKey)
        PasswordSafe.instance.set(credentialAttributes, credentials)
    }

    fun getApiKey(modelName: String): String? {
        return try {
            val credentialAttributes = createCredentialAttributes(modelName)
            PasswordSafe.instance.getPassword(credentialAttributes)
        } catch (e: Exception) {
            null
        }
    }

    fun hasApiKey(modelName: String): Boolean {
        return try {
            getApiKey(modelName)?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }

    private fun createCredentialAttributes(modelName: String): CredentialAttributes {
        return CredentialAttributes(
            generateServiceName(CREDENTIAL_SERVICE_NAME, modelName)
        )
    }

    // GitHub PAT Management
    fun setGitHubPAT(pat: String) {
        val credentialAttributes = createCredentialAttributes("github-pat")
        val credentials = Credentials("github-pat", pat)
        PasswordSafe.instance.set(credentialAttributes, credentials)
    }

    fun removeApiKey(modelName: String) {
        val credentialAttributes = createCredentialAttributes(modelName)
        PasswordSafe.instance.set(credentialAttributes, null)
    }

    fun removeGitHubPAT() {
        val credentialAttributes = createCredentialAttributes("github-pat")
        PasswordSafe.instance.set(credentialAttributes, null)
    }

    fun getGitHubPAT(): String? {
        return try {
            val credentialAttributes = createCredentialAttributes("github-pat")
            PasswordSafe.instance.getPassword(credentialAttributes)
        } catch (e: Exception) {
            null
        }
    }

    // Configuration getters and setters
    fun getDefaultModel(): String = myState.defaultModel
    fun setDefaultModel(model: String) {
        myState.defaultModel = model
    }

    fun getDefaultVariant(): String = myState.defaultVariant
    fun setDefaultVariant(variant: String) {
        myState.defaultVariant = variant
    }

    fun setDefaults(model: String, variant: String) {
        myState.defaultModel = model
        myState.defaultVariant = variant
    }

    fun getDefaultModelAndVariant(): Pair<String, String> {
        return Pair(myState.defaultModel, myState.defaultVariant)
    }

    fun getLanguage(): Language {
        return Languages.parseLanguageString(myState.language)
    }

    fun setLanguage(language: Language) {
        myState.language = Languages.languageToString(language)
    }

    fun shouldAlwaysAdd(): Boolean = myState.alwaysAdd
    fun setShouldAlwaysAdd(alwaysAdd: Boolean) {
        myState.alwaysAdd = alwaysAdd
    }

    fun shouldAutoPush(): Boolean = myState.autoPush
    fun setShouldAutoPush(autoPush: Boolean) {
        myState.autoPush = autoPush
    }

    fun getOllamaBaseUrl(): String = myState.ollamaBaseUrl
    fun setOllamaBaseUrl(url: String) {
        myState.ollamaBaseUrl = url
    }

    fun getCustomOllamaVariant(): String = myState.customOllamaVariant
    fun setCustomOllamaVariant(variant: String) {
        myState.customOllamaVariant = variant
    }

    fun getIgnorePatterns(): List<String> = myState.ignoredFiles.toList()
    fun setIgnorePatterns(patterns: List<String>) {
        myState.ignoredFiles.clear()
        myState.ignoredFiles.addAll(patterns)
    }

    fun addIgnorePattern(pattern: String) {
        if (!myState.ignoredFiles.contains(pattern)) {
            myState.ignoredFiles.add(pattern)
        }
    }

    fun removeIgnorePattern(pattern: String) {
        myState.ignoredFiles.remove(pattern)
    }

    fun getIgnoredFiles(): List<String> = myState.ignoredFiles.toList()
    fun setIgnoredFiles(files: List<String>) {
        myState.ignoredFiles.clear()
        myState.ignoredFiles.addAll(files)
    }

    // Environment API key fallback
    fun getEnvironmentApiKey(modelName: String): String? {
        return when (modelName.lowercase()) {
            "openai" -> System.getenv("OPENAI_API_KEY")
            "claude" -> System.getenv("ANTHROPIC_API_KEY")
            "gemini" -> System.getenv("GOOGLE_API_KEY")
            "github" -> System.getenv("GITHUB_TOKEN")
            "grok" -> System.getenv("XAI_API_KEY")
            "llama" -> System.getenv("LLAMA_API_KEY")
            "deepseek" -> System.getenv("DEEPSEEK_API_KEY")
            else -> null
        }
    }

    // Configuration summary for display
    fun getConfigSummary(): Map<String, Any> {
        val models = listOf("openai", "claude", "gemini", "grok", "llama", "deepseek", "github", "ollama")
        val hasApiKeys = models.associateWith { hasApiKey(it) }

        return mapOf(
            "defaultModel" to myState.defaultModel,
            "defaultVariant" to myState.defaultVariant,
            "language" to getLanguage(),
            "alwaysAdd" to myState.alwaysAdd,
            "autoPush" to myState.autoPush,
            "ollamaBaseUrl" to myState.ollamaBaseUrl,
            "customOllamaVariant" to myState.customOllamaVariant,
            "ignoredFiles" to myState.ignoredFiles,
            "hasApiKeys" to hasApiKeys
        )
    }
}
