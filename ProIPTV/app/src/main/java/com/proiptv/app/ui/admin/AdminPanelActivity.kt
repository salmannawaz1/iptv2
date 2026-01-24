package com.proiptv.app.ui.admin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.proiptv.app.R
import com.proiptv.app.databinding.ActivityAdminPanelBinding
import com.proiptv.app.databinding.DialogAddUserBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminPanelActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAdminPanelBinding
    private val viewModel: AdminViewModel by viewModels()
    
    private lateinit var userAdapter: UserAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupTabs()
        setupRecyclerView()
        setupFab()
        observeViewModel()
        
        viewModel.loadUsers()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.admin_panel)
    }
    
    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.loadUsers()
                    1 -> viewModel.loadResellers()
                    2 -> viewModel.loadStatistics()
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }
    
    private fun setupRecyclerView() {
        userAdapter = UserAdapter(
            onEditClick = { user ->
                showEditUserDialog(user)
            },
            onDeleteClick = { user ->
                showDeleteConfirmation(user)
            },
            onToggleStatus = { user ->
                viewModel.toggleUserStatus(user)
            }
        )
        
        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(this@AdminPanelActivity)
            adapter = userAdapter
        }
    }
    
    private fun setupFab() {
        binding.fabAddUser.setOnClickListener {
            showAddUserDialog()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.users.collectLatest { users ->
                if (users.isEmpty()) {
                    binding.rvUsers.visibility = View.GONE
                    binding.emptyView.visibility = View.VISIBLE
                } else {
                    binding.rvUsers.visibility = View.VISIBLE
                    binding.emptyView.visibility = View.GONE
                    userAdapter.submitList(users)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.statistics.collectLatest { stats ->
                stats?.let {
                    binding.tvTotalUsers.text = "Total Users: ${it.totalUsers}"
                    binding.tvActiveUsers.text = "Active: ${it.activeUsers}"
                    binding.tvExpiredUsers.text = "Expired: ${it.expiredUsers}"
                    binding.tvTotalResellers.text = "Resellers: ${it.totalResellers}"
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { loading ->
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }
        
        lifecycleScope.launch {
            viewModel.message.collectLatest { message ->
                message?.let {
                    Toast.makeText(this@AdminPanelActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showAddUserDialog() {
        val dialogBinding = DialogAddUserBinding.inflate(layoutInflater)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.admin_add_user)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val username = dialogBinding.etUsername.text.toString()
                val password = dialogBinding.etPassword.text.toString()
                val maxConnections = dialogBinding.etMaxConnections.text.toString().toIntOrNull() ?: 1
                val expiryDays = dialogBinding.etExpiryDays.text.toString().toIntOrNull() ?: 30
                val isReseller = dialogBinding.switchReseller.isChecked
                
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    viewModel.addUser(username, password, maxConnections, expiryDays, isReseller)
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showEditUserDialog(user: AdminUser) {
        val dialogBinding = DialogAddUserBinding.inflate(layoutInflater)
        
        dialogBinding.etUsername.setText(user.username)
        dialogBinding.etUsername.isEnabled = false
        dialogBinding.etPassword.setText(user.password)
        dialogBinding.etMaxConnections.setText(user.maxConnections.toString())
        dialogBinding.etExpiryDays.setText("30")
        dialogBinding.switchReseller.isChecked = user.isReseller
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.admin_edit_user)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val password = dialogBinding.etPassword.text.toString()
                val maxConnections = dialogBinding.etMaxConnections.text.toString().toIntOrNull() ?: 1
                val isReseller = dialogBinding.switchReseller.isChecked
                
                viewModel.updateUser(user.copy(
                    password = password,
                    maxConnections = maxConnections,
                    isReseller = isReseller
                ))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showDeleteConfirmation(user: AdminUser) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.admin_delete_user)
            .setMessage("Are you sure you want to delete user '${user.username}'?")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteUser(user)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
