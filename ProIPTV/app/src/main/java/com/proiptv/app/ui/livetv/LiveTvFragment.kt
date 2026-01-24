package com.proiptv.app.ui.livetv

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.proiptv.app.data.model.LiveCategory
import com.proiptv.app.data.model.LiveStream
import com.proiptv.app.databinding.FragmentLiveTvBinding
import com.proiptv.app.ui.player.PlayerActivity
import com.proiptv.app.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LiveTvFragment : Fragment() {
    
    private var _binding: FragmentLiveTvBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: LiveTvViewModel by viewModels()
    
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var channelAdapter: ChannelAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveTvBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeViewModel()
        viewModel.loadData()
    }
    
    private fun setupRecyclerViews() {
        categoryAdapter = CategoryAdapter { category ->
            viewModel.selectCategory(category)
        }
        
        channelAdapter = ChannelAdapter(
            onChannelClick = { channel ->
                playChannel(channel)
            },
            onFavoriteClick = { channel ->
                viewModel.toggleFavorite(channel)
            }
        )
        
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
        
        binding.rvChannels.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = channelAdapter
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.shimmerLayout.visibility = View.VISIBLE
                        binding.shimmerLayout.startShimmer()
                        binding.rvCategories.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.shimmerLayout.stopShimmer()
                        binding.shimmerLayout.visibility = View.GONE
                        binding.rvCategories.visibility = View.VISIBLE
                        
                        val allCategory = LiveCategory("all", "All Channels", null)
                        val categories = listOf(allCategory) + (resource.data ?: emptyList())
                        categoryAdapter.submitList(categories)
                    }
                    is Resource.Error -> {
                        binding.shimmerLayout.stopShimmer()
                        binding.shimmerLayout.visibility = View.GONE
                        Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.channels.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.channelShimmer.visibility = View.VISIBLE
                        binding.channelShimmer.startShimmer()
                        binding.rvChannels.visibility = View.GONE
                        binding.tvNoChannels.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.channelShimmer.stopShimmer()
                        binding.channelShimmer.visibility = View.GONE
                        
                        val channels = resource.data ?: emptyList()
                        if (channels.isEmpty()) {
                            binding.rvChannels.visibility = View.GONE
                            binding.tvNoChannels.visibility = View.VISIBLE
                        } else {
                            binding.rvChannels.visibility = View.VISIBLE
                            binding.tvNoChannels.visibility = View.GONE
                            channelAdapter.submitList(channels)
                        }
                    }
                    is Resource.Error -> {
                        binding.channelShimmer.stopShimmer()
                        binding.channelShimmer.visibility = View.GONE
                        binding.tvNoChannels.visibility = View.VISIBLE
                        Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedCategory.collectLatest { category ->
                categoryAdapter.setSelectedCategory(category?.categoryId ?: "all")
            }
        }
    }
    
    private fun playChannel(channel: LiveStream) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_STREAM_ID, channel.streamId)
            putExtra(PlayerActivity.EXTRA_STREAM_NAME, channel.name)
            putExtra(PlayerActivity.EXTRA_STREAM_TYPE, "live")
            // Pass direct URL for M3U channels
            channel.directSource?.let { url ->
                putExtra(PlayerActivity.EXTRA_DIRECT_URL, url)
            }
        }
        startActivity(intent)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
