///*
// * Created by Samyak kamble on 8/14/24, 11:33 AM
// *  Copyright (c) 2024 . All rights reserved.
// *  Last modified 8/14/24, 11:33 AM
// */

package com.samyak2403.iptvmine.screens

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.samyak2403.iptvmine.MainActivity
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.adapter.ChannelsAdapter
import com.samyak2403.iptvmine.model.Channel
import com.samyak2403.iptvmine.provider.ChannelsProvider
import com.samyak2403.iptvmine.utils.TvUtils

class HomeFragment : Fragment() {

    private lateinit var channelsProvider: ChannelsProvider
    private lateinit var searchEditText: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var chipGroup: ChipGroup
    private lateinit var adapter: ChannelsAdapter

    private var debounceHandler: Handler? = null
    private var allChannels: List<Channel> = emptyList()
    private var selectedCategory: String? = "All"
    private var isTvMode: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        channelsProvider = ViewModelProvider(this)[ChannelsProvider::class.java]
        progressBar = view.findViewById(R.id.progressBar)
        recyclerView = view.findViewById(R.id.recyclerView)
        chipGroup = view.findViewById(R.id.categoryChipGroup)

        // Get search EditText from MainActivity
        searchEditText = (activity as? MainActivity)?.getSearchEditText() ?: EditText(requireContext())

        adapter = ChannelsAdapter { channel: Channel ->
            PlayerActivity.start(requireContext(), channel)
        }

        // Detect TV mode
        isTvMode = TvUtils.isTvMode(requireContext())
        
        // Use GridLayoutManager for TV, LinearLayoutManager for mobile
        recyclerView.layoutManager = if (isTvMode) {
            GridLayoutManager(requireContext(), TvUtils.getGridSpanCount(requireContext()))
        } else {
            LinearLayoutManager(requireContext())
        }
        recyclerView.adapter = adapter
        
        // Setup TV-specific RecyclerView configuration
        if (isTvMode) {
            TvUtils.setupRecyclerViewForTv(recyclerView)
            setupTvFocusNavigation()
        }

        setupObservers()
        fetchData()

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                debounceHandler?.removeCallbacksAndMessages(null)
                debounceHandler = Handler(Looper.getMainLooper())
                debounceHandler?.postDelayed({
                    filterChannels(s.toString())
                }, 300)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        return view
    }

    private fun setupObservers() {
        channelsProvider.channels.observe(viewLifecycleOwner) { data ->
            allChannels = data
            progressBar.visibility = View.GONE
            if (searchEditText.text.toString().isEmpty() && selectedCategory == "All") {
                adapter.updateChannels(data)
            }
        }

        channelsProvider.filteredChannels.observe(viewLifecycleOwner) { data ->
            adapter.updateChannels(data)
        }

        channelsProvider.categories.observe(viewLifecycleOwner) { categories ->
            updateCategoryChips(categories)
        }

        channelsProvider.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        channelsProvider.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchData() {
        progressBar.visibility = View.VISIBLE
        channelsProvider.fetchM3UFile()
    }

    private fun filterChannels(query: String) {
        if (query.isEmpty() && (selectedCategory.isNullOrEmpty() || selectedCategory == "All")) {
            adapter.updateChannels(allChannels)
        } else {
            channelsProvider.filterChannelsByQueryAndCategory(query, selectedCategory)
        }
    }

    private fun updateCategoryChips(categories: List<String>) {
        chipGroup.removeAllViews()
        
        categories.forEachIndexed { index, category ->
            val chip = Chip(requireContext()).apply {
                text = category
                isCheckable = true
                isChecked = category == selectedCategory
                
                // TV-specific styling
                if (isTvMode) {
                    isFocusable = true
                    isFocusableInTouchMode = false
                    textSize = 18f
                    chipMinHeight = 56f
                    chipStartPadding = 24f
                    chipEndPadding = 24f
                    
                    // Set focus change listener for TV
                    setOnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) {
                            v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start()
                            (v as Chip).setChipBackgroundColorResource(R.color.tv_focus_blue_light)
                            v.setChipStrokeColorResource(R.color.tv_focus_blue)
                            v.chipStrokeWidth = 4f
                        } else {
                            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                            (v as Chip).chipStrokeWidth = 0f
                            if (!v.isChecked) {
                                v.setChipBackgroundColorResource(android.R.color.transparent)
                            }
                        }
                    }
                    
                    // Set next focus for navigation
                    nextFocusDownId = R.id.recyclerView
                }
                
                setOnClickListener {
                    selectedCategory = category
                    val query = searchEditText.text.toString()
                    channelsProvider.filterChannelsByQueryAndCategory(query, category)
                }
            }
            chipGroup.addView(chip)
        }
        
        // Request focus on first chip for TV
        if (isTvMode && chipGroup.childCount > 0) {
            chipGroup.getChildAt(0)?.requestFocus()
        }
    }
    
    /**
     * Setup TV-specific focus navigation between chips and RecyclerView
     */
    private fun setupTvFocusNavigation() {
        // When RecyclerView gets focus, ensure first visible item is focused
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE && isTvMode) {
                    // Ensure focused item is visible
                    val focusedChild = recyclerView.focusedChild
                    focusedChild?.let {
                        recyclerView.smoothScrollToPosition(
                            recyclerView.getChildAdapterPosition(it)
                        )
                    }
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        debounceHandler?.removeCallbacksAndMessages(null)
    }
}