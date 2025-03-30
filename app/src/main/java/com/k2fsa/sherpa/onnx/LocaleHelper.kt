package com.k2fsa.sherpa.onnx

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import java.util.*

object LocaleHelper {
    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"

    fun onAttach(context: Context): Context {
        val lang = getPersistedLanguage(context)
        return setLocale(context, lang)
    }

    fun getLanguage(context: Context): String {
        return getPersistedLanguage(context)
    }

    fun setLocale(context: Context, language: String): Context {
        persist(context, language)
        return updateResources(context, language)
    }

    private fun getPersistedLanguage(context: Context): String {
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return preferences.getString(SELECTED_LANGUAGE, Locale.getDefault().language) ?: "en"
    }

    private fun persist(context: Context, language: String) {
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        preferences.edit().putString(SELECTED_LANGUAGE, language).apply()
    }

    @Suppress("DEPRECATION")
    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }
} 