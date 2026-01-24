package com.proiptv.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.proiptv.app.databinding.ActivityLoginBinding
import com.proiptv.app.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    
    private var isXtreamMode = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        checkExistingLogin()
        setupUI()
        observeViewModel()
    }
    
    private fun checkExistingLogin() {
        lifecycleScope.launch {
            viewModel.isLoggedIn.collectLatest { isLoggedIn ->
                if (isLoggedIn) {
                    navigateToMain()
                }
            }
        }
    }
    
    private fun setupUI() {
        updateLoginMode()
        
        binding.btnSwitchMode.setOnClickListener {
            isXtreamMode = !isXtreamMode
            updateLoginMode()
        }
        
        binding.btnLogin.setOnClickListener {
            if (isXtreamMode) {
                loginWithXtream()
            } else {
                loginWithM3U()
            }
        }
    }
    
    private fun updateLoginMode() {
        if (isXtreamMode) {
            binding.xtreamInputs.visibility = View.VISIBLE
            binding.m3uInputs.visibility = View.GONE
            binding.btnSwitchMode.text = "Switch to M3U"
            binding.tvLoginTitle.text = "Xtream Codes Login"
        } else {
            binding.xtreamInputs.visibility = View.GONE
            binding.m3uInputs.visibility = View.VISIBLE
            binding.btnSwitchMode.text = "Switch to Xtream"
            binding.tvLoginTitle.text = "M3U Playlist Login"
        }
    }
    
    private fun loginWithXtream() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val rememberMe = binding.cbRememberMe.isChecked
        
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModel.loginWithXtream(username, password, rememberMe)
    }
    
    private fun loginWithM3U() {
        val m3uUrl = binding.etM3uUrl.text.toString().trim()
        val rememberMe = binding.cbRememberMe.isChecked
        
        if (m3uUrl.isEmpty()) {
            Toast.makeText(this, "Please enter M3U URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModel.loginWithM3U(m3uUrl, rememberMe)
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.loginState.collectLatest { state ->
                when (state) {
                    is LoginState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                    }
                    is LoginState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnLogin.isEnabled = false
                    }
                    is LoginState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                        navigateToMain()
                    }
                    is LoginState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
