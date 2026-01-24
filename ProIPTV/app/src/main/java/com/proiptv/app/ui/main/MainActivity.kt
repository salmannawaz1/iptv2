package com.proiptv.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.proiptv.app.R
import com.proiptv.app.databinding.ActivityMainBinding
import com.proiptv.app.ui.admin.AdminPanelActivity
import com.proiptv.app.ui.favorites.FavoritesFragment
import com.proiptv.app.ui.livetv.LiveTvFragment
import com.proiptv.app.ui.login.LoginActivity
import com.proiptv.app.ui.movies.MoviesFragment
import com.proiptv.app.ui.series.SeriesFragment
import com.proiptv.app.ui.settings.SettingsFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        setupBottomNavigation()
        
        if (savedInstanceState == null) {
            loadFragment(LiveTvFragment())
        }
        
        observeViewModel()
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_live_tv -> {
                    loadFragment(LiveTvFragment())
                    binding.toolbar.title = getString(R.string.nav_live_tv)
                    true
                }
                R.id.nav_movies -> {
                    loadFragment(MoviesFragment())
                    binding.toolbar.title = getString(R.string.nav_movies)
                    true
                }
                R.id.nav_series -> {
                    loadFragment(SeriesFragment())
                    binding.toolbar.title = getString(R.string.nav_series)
                    true
                }
                R.id.nav_favorites -> {
                    loadFragment(FavoritesFragment())
                    binding.toolbar.title = getString(R.string.nav_favorites)
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    binding.toolbar.title = getString(R.string.nav_settings)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                // Open search
                true
            }
            R.id.action_admin -> {
                startActivity(Intent(this, AdminPanelActivity::class.java))
                true
            }
            R.id.action_refresh -> {
                viewModel.refreshData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isLoggedOut.collectLatest { isLoggedOut ->
                if (isLoggedOut) {
                    navigateToLogin()
                }
            }
        }
    }
    
    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
