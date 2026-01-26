package com.samyak2403.iptvmine.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.databinding.ActivityBottomBarSelectionBinding
import com.samyak2403.iptvmine.utils.BottomBarManager
import com.samyak2403.iptvmine.utils.BottomBarType
import com.samyak2403.iptvmine.utils.TapTargetHelper
import com.samyak2403.iptvmine.utils.ThemeManager

class BottomBarSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBottomBarSelectionBinding
    private lateinit var adapter: BottomBarAdapter
    private var selectedType: BottomBarType = BottomBarType.SMOOTH_BOTTOM_BAR

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityBottomBarSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupToolbar()
        setupRecyclerView()
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
        setSupportActionBar(binding.appBarLayout)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Select Bottom Bar Style"
        }
        binding.appBarLayout.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupRecyclerView() {
        selectedType = BottomBarManager.getSelectedBottomBar(this)
        
        adapter = BottomBarAdapter(
            BottomBarManager.getAllBottomBarTypes(),
            selectedType
        ) { type ->
            selectedType = type
            BottomBarManager.setSelectedBottomBar(this, type)
            adapter.updateSelection(type)
            android.widget.Toast.makeText(
                this,
                "${type.displayName} selected",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter
        
        // Show onboarding after RecyclerView is ready
        binding.recyclerView.post {
            showOnboardingIfNeeded()
        }
    }


    /**
     * Show onboarding for bottom bar selection
     */
    private fun showOnboardingIfNeeded() {
        if (!TapTargetHelper.isBottomBarSelectionOnboardingShown(this) && 
            binding.recyclerView.childCount > 0) {
            // Get the first item in the RecyclerView
            val firstItem = binding.recyclerView.getChildAt(0)
            
            firstItem?.let {
                TapTargetHelper.showBottomBarSelectionOnboarding(
                    activity = this,
                    firstItemView = it,
                    onComplete = {
                        // Onboarding completed
                    }
                )
            }
        }
    }

    /**
     * Adapter for bottom bar selection grid
     */
    inner class BottomBarAdapter(
        private val items: List<BottomBarType>,
        private var selectedType: BottomBarType,
        private val onItemClick: (BottomBarType) -> Unit
    ) : RecyclerView.Adapter<BottomBarAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.bottomBarImage)
            val titleText: TextView = view.findViewById(R.id.bottomBarTitle)
            val selectedIndicator: View = view.findViewById(R.id.selectedIndicator)
            val cardView: View = view.findViewById(R.id.cardView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_bottom_bar_selection, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            
            holder.titleText.text = item.displayName
            holder.imageView.setImageResource(getBottomBarPreviewImage(item))
            
            // Show/hide selection indicator
            holder.selectedIndicator.visibility = 
                if (item == selectedType) View.VISIBLE else View.GONE
            
            // Set card stroke for selected item
            val cardView = holder.cardView as? com.google.android.material.card.MaterialCardView
            cardView?.strokeWidth = if (item == selectedType) 4 else 0
            cardView?.strokeColor = if (item == selectedType) 
                getColor(R.color.colorPrimary) else 0
            
            holder.itemView.setOnClickListener {
                onItemClick(item)
            }
        }

        override fun getItemCount(): Int = items.size

        fun updateSelection(type: BottomBarType) {
            val oldPosition = items.indexOf(selectedType)
            val newPosition = items.indexOf(type)
            selectedType = type
            notifyItemChanged(oldPosition)
            notifyItemChanged(newPosition)
        }

        private fun getBottomBarPreviewImage(type: BottomBarType): Int {
            return when (type) {
                BottomBarType.SMOOTH_BOTTOM_BAR -> R.drawable.preview_smooth_bottom_bar
                BottomBarType.ANIMATED_BOTTOM_BAR -> R.drawable.preview_animated_bottom_bar
                BottomBarType.CHIP_NAVIGATION_BAR -> R.drawable.preview_chip_navigation_bar
            }
        }
    }
}
