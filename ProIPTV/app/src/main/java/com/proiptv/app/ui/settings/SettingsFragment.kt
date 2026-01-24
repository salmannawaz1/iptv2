package com.proiptv.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.proiptv.app.databinding.FragmentSettingsBinding
import com.proiptv.app.ui.admin.AdminPanelActivity
import com.proiptv.app.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SettingsViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        binding.switchAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoPlay(isChecked)
        }
        
        binding.layoutQuality.setOnClickListener {
            showQualityDialog()
        }
        
        binding.layoutClearCache.setOnClickListener {
            showClearCacheDialog()
        }
        
        binding.layoutAdminPanel.setOnClickListener {
            startActivity(Intent(requireContext(), AdminPanelActivity::class.java))
        }
        
        binding.layoutLogout.setOnClickListener {
            showLogoutDialog()
        }
        
        binding.layoutAbout.setOnClickListener {
            showAboutDialog()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userInfo.collectLatest { info ->
                info?.let {
                    binding.tvUsername.text = it.username
                    binding.tvServerUrl.text = it.serverUrl
                    binding.tvExpiry.text = "Expires: ${it.expiry}"
                    binding.tvConnections.text = "Max Connections: ${it.maxConnections}"
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.autoPlay.collectLatest { enabled ->
                binding.switchAutoPlay.isChecked = enabled
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.defaultQuality.collectLatest { quality ->
                binding.tvQuality.text = quality.uppercase()
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoggedOut.collectLatest { loggedOut ->
                if (loggedOut) {
                    navigateToLogin()
                }
            }
        }
    }
    
    private fun showQualityDialog() {
        val qualities = arrayOf("Auto", "High", "Medium", "Low")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Default Quality")
            .setItems(qualities) { _, which ->
                viewModel.setDefaultQuality(qualities[which].lowercase())
            }
            .show()
    }
    
    private fun showClearCacheDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear Cache")
            .setMessage("This will clear all cached data. Continue?")
            .setPositiveButton("Clear") { _, _ ->
                viewModel.clearCache()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("About ProIPTV")
            .setMessage("Version 1.0.0\n\nA professional IPTV player with Xtream Codes API support.\n\nFeatures:\n• Live TV\n• Movies (VOD)\n• Series\n• EPG Support\n• Favorites\n• Admin Panel")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun navigateToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
