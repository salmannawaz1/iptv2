package com.samyak2403.iptvmine.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.samyak2403.iptvmine.databinding.ActivityFeedbackBinding
import com.samyak2403.iptvmine.utils.ThemeManager

class FeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackBinding
    private var selectedFeedbackType = "Bug Report"
    
    companion object {
        private const val FEEDBACK_EMAIL = "iptvmineofficel@gmail.com"
    }

    private val feedbackTypes = listOf(
        "Bug Report",
        "Feature Request",
        "Channel Request",
        "Performance Issue",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupToolbar()
        setupSpinner()
        setupClickListeners()
    }


    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            binding.statusBarBackground.updateLayoutParams {
                height = systemBars.top
            }
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Send Feedback"
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, feedbackTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFeedbackType.adapter = adapter

        binding.spinnerFeedbackType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedFeedbackType = feedbackTypes[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupClickListeners() {
        binding.btnSubmit.setOnClickListener {
            sendFeedbackEmail()
        }

        binding.textOpenBrowser.setOnClickListener {
            sendFeedbackEmail()
        }
    }

    private fun sendFeedbackEmail() {
        val title = binding.editTitle.text.toString().trim()
        val description = binding.editDescription.text.toString().trim()

        if (title.isEmpty()) {
            binding.editTitle.error = "Title is required"
            return
        }

        if (description.isEmpty()) {
            binding.editDescription.error = "Description is required"
            return
        }

        val subject = "[IPTVmine - $selectedFeedbackType] $title"
        val body = buildEmailBody(description)

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            startActivity(Intent.createChooser(intent, "Send feedback via email"))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildEmailBody(description: String): String {
        val sb = StringBuilder()
        sb.appendLine("Description:")
        sb.appendLine(description)
        sb.appendLine()

        if (binding.checkboxDeviceInfo.isChecked) {
            val (versionName, versionCode) = getAppVersion()
            sb.appendLine("-------------------")
            sb.appendLine("Device Info:")
            sb.appendLine("App Version: $versionName ($versionCode)")
            sb.appendLine("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            sb.appendLine("Feedback Type: $selectedFeedbackType")
        }

        return sb.toString()
    }

    private fun getAppVersion(): Pair<String, Long> {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            Pair(versionName, versionCode)
        } catch (e: PackageManager.NameNotFoundException) {
            Pair("Unknown", 0L)
        }
    }
}
