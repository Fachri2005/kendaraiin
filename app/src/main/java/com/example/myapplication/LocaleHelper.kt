package com.example.myapplication

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val SELECTED_LANGUAGE = "app_lang"
    private const val DEFAULT_LANGUAGE = "in"

    fun onAttach(context: Context): Context {
        val lang = getPersistedData(context, DEFAULT_LANGUAGE)
        return setLocale(context, lang)
    }

    fun setLocale(context: Context, language: String): Context {
        persist(context, language)
        return updateResources(context, language)
    }

    private fun updateResources(context: Context, language: String): Context {
        // Normalisasi: Folder Anda bernama 'values-in', jadi paksa pakai kode 'in'
        val localeCode = if (language == "id") "in" else language
        val locale = Locale(localeCode)
        Locale.setDefault(locale)

        val res = context.resources
        val config = Configuration(res.configuration)
        
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        // Update configuration lama untuk memastikan resource global ikut berubah
        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)

        return context.createConfigurationContext(config)
    }

    fun getLanguage(context: Context): String {
        return getPersistedData(context, DEFAULT_LANGUAGE)
    }

    private fun getPersistedData(context: Context, defaultLanguage: String): String {
        val preferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return preferences.getString(SELECTED_LANGUAGE, defaultLanguage) ?: defaultLanguage
    }

    private fun persist(context: Context, language: String) {
        val preferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        preferences.edit().putString(SELECTED_LANGUAGE, language).apply()
    }
}
