package com.example.norwinlabstools

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.norwinlabstools.databinding.FragmentBugReportBinding

class BugReportFragment : Fragment() {

    private var _binding: FragmentBugReportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBugReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val diagnosticInfo = getDiagnosticInfo()
        binding.tvDiagnosticInfo.text = diagnosticInfo

        binding.btnSendReport.setOnClickListener {
            val title = binding.editBugTitle.text.toString()
            val description = binding.editBugDescription.text.toString()

            if (title.isBlank() || description.isBlank()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendEmail(title, description, diagnosticInfo)
        }
    }

    private fun getDiagnosticInfo(): String {
        return """
            App Version: ${getVersionName()}
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            Build Fingerprint: ${Build.FINGERPRINT}
        """.trimIndent()
    }

    private fun getVersionName(): String? {
        return try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            pInfo.versionName
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun sendEmail(title: String, description: String, diagnostic: String) {
        val recipient = "norwinlabs@gmail.com" // Replace with your actual email
        val subject = "[BUG REPORT] $title"
        val body = """
            ISSUE DESCRIPTION:
            $description
            
            --------------------------------
            SYSTEM DIAGNOSTICS:
            $diagnostic
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            startActivity(Intent.createChooser(intent, "Send Bug Report..."))
        } catch (e: Exception) {
            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}