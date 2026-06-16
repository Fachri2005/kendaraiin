package com.example.myapplication

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val SELECTED_LANGUAGE = "app_lang"
    private const val DEFAULT_LANGUAGE = "id" // Menggunakan 'id' sebagai standar modern

    fun onAttach(context: Context): Context {
        val lang = getPersistedData(context, DEFAULT_LANGUAGE)
        return setLocale(context, lang)
    }

    fun setLocale(context: Context, language: String): Context {
        persist(context, language)
        return updateResources(context, language)
    }

    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        // createConfigurationContext direkomendasikan untuk API 17+ (minSdk proyek ini 24)
        return context.createConfigurationContext(configuration)
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
