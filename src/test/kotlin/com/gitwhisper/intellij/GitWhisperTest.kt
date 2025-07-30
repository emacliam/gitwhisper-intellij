package com.gitwhisper.intellij

import com.gitwhisper.intellij.models.Languages
import com.gitwhisper.intellij.models.ModelVariants
import com.gitwhisper.intellij.utils.CommitUtils
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Basic tests for GitWhisper functionality
 */
class GitWhisperTest {

    @Test
    fun testLanguageSupport() {
        val english = Languages.english
        assertEquals("en", english.code)
        assertEquals("US", english.countryCode)
        assertEquals("English", english.name)
        
        val allLanguages = Languages.getAllLanguages()
        assertTrue(allLanguages.isNotEmpty())
        assertTrue(allLanguages.contains(english))
    }

    @Test
    fun testLanguageParsing() {
        val languageString = "en;US"
        val parsed = Languages.parseLanguageString(languageString)
        assertEquals(Languages.english, parsed)
        
        val converted = Languages.languageToString(parsed)
        assertEquals(languageString, converted)
    }

    @Test
    fun testModelVariants() {
        val openaiVariants = ModelVariants.getVariants("openai")
        assertTrue(openaiVariants.isNotEmpty())
        
        val defaultVariant = ModelVariants.getDefaultVariant("openai")
        assertEquals("gpt-4o", defaultVariant)
        
        assertTrue(ModelVariants.isModelSupported("openai"))
        assertTrue(ModelVariants.isModelSupported("claude"))
    }

    @Test
    fun testCommitPromptGeneration() {
        val diff = """
            diff --git a/test.kt b/test.kt
            index 1234567..abcdefg 100644
            --- a/test.kt
            +++ b/test.kt
            @@ -1,3 +1,4 @@
             fun main() {
            +    println("Hello World")
             }
        """.trimIndent()
        
        val prompt = CommitUtils.getCommitPrompt(diff, Languages.english)
        assertNotNull(prompt)
        assertTrue(prompt.contains("diff"))
        assertTrue(prompt.contains("conventional"))
    }

    @Test
    fun testAnalysisPromptGeneration() {
        val diff = """
            diff --git a/test.kt b/test.kt
            index 1234567..abcdefg 100644
            --- a/test.kt
            +++ b/test.kt
            @@ -1,3 +1,4 @@
             fun main() {
            +    println("Hello World")
             }
        """.trimIndent()
        
        val prompt = CommitUtils.getAnalysisPrompt(diff, Languages.english)
        assertNotNull(prompt)
        assertTrue(prompt.contains("analyze"))
        assertTrue(prompt.contains("diff"))
    }

    @Test
    fun testBundleMessages() {
        val pluginName = GitWhisperBundle.message("plugin.name")
        assertEquals("GitWhisper", pluginName)
        
        val generateAction = GitWhisperBundle.message("action.generate.commit")
        assertEquals("Generate AI Commit Message", generateAction)
    }
}
