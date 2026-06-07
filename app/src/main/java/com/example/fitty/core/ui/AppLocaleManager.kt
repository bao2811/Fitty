package com.example.fitty.core.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object AppLocaleManager {
    fun applyLanguage(language: String?) {
        val normalized = language
            ?.trim()
            ?.lowercase(Locale.US)
            ?.takeIf { it == "en" || it == "vi" }
            .orEmpty()
        val locales = if (normalized.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(normalized)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun resolveStoredLanguage(context: Context): String {
        val locale = AppCompatDelegate.getApplicationLocales()[0]
            ?: context.resources.configuration.locales[0]
        return when {
            locale.language.equals("en", ignoreCase = true) -> "en"
            else -> "vi"
        }
    }
}
