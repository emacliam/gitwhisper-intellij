package com.gitwhisper.intellij.models

/**
 * Represents a language for commit messages
 * Based on the Language model from the reference implementation
 */
data class Language(
    val code: String,
    val countryCode: String,
    val name: String,
    val displayName: String
)

/**
 * Predefined languages for commit messages
 */
object Languages {
    val english = Language("en", "US", "English", "English (US)")
    val spanish = Language("es", "ES", "Spanish", "Español (ES)")
    val french = Language("fr", "FR", "French", "Français (FR)")
    val german = Language("de", "DE", "German", "Deutsch (DE)")
    val italian = Language("it", "IT", "Italian", "Italiano (IT)")
    val portuguese = Language("pt", "BR", "Portuguese", "Português (BR)")
    val russian = Language("ru", "RU", "Russian", "Русский (RU)")
    val chinese = Language("zh", "CN", "Chinese", "中文 (CN)")
    val japanese = Language("ja", "JP", "Japanese", "日本語 (JP)")
    val korean = Language("ko", "KR", "Korean", "한국어 (KR)")
    val dutch = Language("nl", "NL", "Dutch", "Nederlands (NL)")
    val polish = Language("pl", "PL", "Polish", "Polski (PL)")
    val czech = Language("cs", "CZ", "Czech", "Čeština (CZ)")
    val hungarian = Language("hu", "HU", "Hungarian", "Magyar (HU)")
    val romanian = Language("ro", "RO", "Romanian", "Română (RO)")
    val swedish = Language("sv", "SE", "Swedish", "Svenska (SE)")
    val norwegian = Language("no", "NO", "Norwegian", "Norsk (NO)")
    val danish = Language("da", "DK", "Danish", "Dansk (DK)")
    val finnish = Language("fi", "FI", "Finnish", "Suomi (FI)")
    val greek = Language("el", "GR", "Greek", "Ελληνικά (GR)")
    val turkish = Language("tr", "TR", "Turkish", "Türkçe (TR)")
    val arabic = Language("ar", "SA", "Arabic", "العربية (SA)")
    val hebrew = Language("he", "IL", "Hebrew", "עברית (IL)")
    val hindi = Language("hi", "IN", "Hindi", "हिन्दी (IN)")
    val thai = Language("th", "TH", "Thai", "ไทย (TH)")
    val vietnamese = Language("vi", "VN", "Vietnamese", "Tiếng Việt (VN)")
    val indonesian = Language("id", "ID", "Indonesian", "Bahasa Indonesia (ID)")
    val malay = Language("ms", "MY", "Malay", "Bahasa Melayu (MY)")
    val ukrainian = Language("uk", "UA", "Ukrainian", "Українська (UA)")
    val bulgarian = Language("bg", "BG", "Bulgarian", "Български (BG)")
    val croatian = Language("hr", "HR", "Croatian", "Hrvatski (HR)")
    val serbian = Language("sr", "RS", "Serbian", "Српски (RS)")
    val slovak = Language("sk", "SK", "Slovak", "Slovenčina (SK)")
    val slovenian = Language("sl", "SI", "Slovenian", "Slovenščina (SI)")
    val lithuanian = Language("lt", "LT", "Lithuanian", "Lietuvių (LT)")
    val latvian = Language("lv", "LV", "Latvian", "Latviešu (LV)")
    val estonian = Language("et", "EE", "Estonian", "Eesti (EE)")

    /**
     * Get all available languages
     */
    fun getAllLanguages(): List<Language> {
        return listOf(
            english, spanish, french, german, italian, portuguese, russian,
            chinese, japanese, korean, dutch, polish, czech, hungarian,
            romanian, swedish, norwegian, danish, finnish, greek, turkish,
            arabic, hebrew, hindi, thai, vietnamese, indonesian, malay,
            ukrainian, bulgarian, croatian, serbian, slovak, slovenian,
            lithuanian, latvian, estonian
        )
    }

    /**
     * Parse language string in format "code;countryCode"
     */
    fun parseLanguageString(languageString: String): Language {
        val parts = languageString.split(";")
        if (parts.size != 2) {
            return english // Default fallback
        }

        val code = parts[0]
        val countryCode = parts[1]

        return getAllLanguages().find { 
            it.code == code && it.countryCode == countryCode 
        } ?: english
    }

    /**
     * Convert language to string format "code;countryCode"
     */
    fun languageToString(language: Language): String {
        return "${language.code};${language.countryCode}"
    }

    /**
     * Find language by display name
     */
    fun findByDisplayName(displayName: String): Language? {
        return getAllLanguages().find { it.displayName == displayName }
    }

    /**
     * Find language by code
     */
    fun findByCode(code: String): Language? {
        return getAllLanguages().find { it.code == code }
    }
}
