package com.example.norwinlabstools

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.norwinlabstools.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val PREFS_NAME = "norwin_prefs"
    private val KEY_THEME = "app_theme"
    private val KEY_BIOMETRIC = "enable_biometric"
    private val KEY_AI_ANALYSIS = "enable_ai_analysis"
    private val KEY_API_KEY = "gemini_api_key"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Theme Setup
        val savedTheme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        when (savedTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.radioLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.radioDark.isChecked = true
            else -> binding.radioSystem.isChecked = true
        }

        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radio_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radio_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            prefs.edit().putInt(KEY_THEME, mode).apply()
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        // Biometric Switch
        binding.switchBiometric.isChecked = prefs.getBoolean(KEY_BIOMETRIC, false)
        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_BIOMETRIC, isChecked).apply()
        }

        // AI Analysis Switch
        binding.switchAiAnalysis.isChecked = prefs.getBoolean(KEY_AI_ANALYSIS, true)
        binding.switchAiAnalysis.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_AI_ANALYSIS, isChecked).apply()
        }

        // API Key Field
        binding.editApiKey.setText(prefs.getString(KEY_API_KEY, ""))
        binding.editApiKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString(KEY_API_KEY, s.toString()).apply()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}