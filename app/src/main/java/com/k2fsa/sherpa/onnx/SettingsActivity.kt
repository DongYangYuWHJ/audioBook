package com.k2fsa.sherpa.onnx

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import java.util.*

class SettingsActivity : AppCompatActivity() {
    private lateinit var languageSpinner: Spinner
    private lateinit var modelSpinner: Spinner
    private var tempLanguage: String = ""
    private var tempModel: String = ""
    private var hasChanges: Boolean = false
    private lateinit var editTextSid: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        editTextSid = findViewById(R.id.editTextSid)

        // 从 SharedPreferences 加载当前设置
        val preferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val currentSid = preferences.getInt("sid", 101)
        editTextSid.setText(currentSid.toString())


        languageSpinner = findViewById(R.id.languageSpinner)
        modelSpinner = findViewById(R.id.modelSpinner)
        val confirmButton = findViewById<Button>(R.id.confirmButton)
        
        // 初始化临时变量
        tempLanguage = LocaleHelper.getLanguage(this)
        tempModel = getSelectedModel()
        
        setupLanguageSpinner()
        setupModelSpinner()
        
        confirmButton.setOnClickListener {
            // 直接保存当前值
            val newSid = editTextSid.text.toString().toIntOrNull() ?: 101
            getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit()
                .putInt("sid", newSid)
                .apply()
            finish()
            if (hasChanges) {
                // 保存语言设置
                if (tempLanguage != LocaleHelper.getLanguage(this)) {
                    LocaleHelper.setLocale(this@SettingsActivity, tempLanguage)
                }
                
                // 保存模型设置
                if (tempModel != getSelectedModel()) {
                    saveSelectedModel(tempModel)
                }
                
                // 如果有任何更改，重启应用
                restartApp()
            } else {
                finish()
            }
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (hasChanges) {
            // 如果有未保存的更改，显示确认对话框
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.unsaved_changes))
                .setMessage(getString(R.string.save_changes_prompt))
                .setPositiveButton(getString(R.string.save)) { _, _ ->
                    // 保存更改并重启
                    LocaleHelper.setLocale(this, tempLanguage)
                    saveSelectedModel(tempModel)
                    restartApp()
                }
                .setNegativeButton(getString(R.string.discard)) { _, _ ->
                    // 放弃更改
                    finish()
                }
                .show()
        } else {
            super.onBackPressed()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf(
            getString(R.string.english) to "en",
            getString(R.string.chinese) to "zh"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languages.map { it.first }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        languageSpinner.adapter = adapter
        
        // Set current language selection
        val index = languages.indexOfFirst { it.second == tempLanguage }
        if (index != -1) {
            languageSpinner.setSelection(index)
        }

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLang = languages[position].second
                if (selectedLang != tempLanguage) {
                    tempLanguage = selectedLang
                    hasChanges = true
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun setupModelSpinner() {
        val models = arrayOf(
            getString(R.string.model_chinese) to "zh",
            getString(R.string.model_english) to "en",
//            getString(R.string.model_mix) to "mix"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            models.map { it.first }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        modelSpinner.adapter = adapter
        
        val index = models.indexOfFirst { it.second == tempModel }
        if (index != -1) {
            modelSpinner.setSelection(index)
        }

        modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedModel = models[position].second
                if (selectedModel != tempModel) {
                    tempModel = selectedModel
                    hasChanges = true
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun getSelectedModel(): String {
        val preferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        return preferences.getString("selected_model", "zh") ?: "zh"
    }

    private fun saveSelectedModel(model: String) {
        val preferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        preferences.edit().putString("selected_model", model).apply()
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
} 