package com.proiptv.app.ui.series

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.proiptv.app.R
import com.proiptv.app.data.model.Episode
import com.proiptv.app.databinding.DialogSeriesDetailBinding
import com.proiptv.app.ui.player.PlayerActivity
import com.proiptv.app.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SeriesDetailDialog : BottomSheetDialogFragment() {
    
    private var _binding: DialogSeriesDetailBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SeriesViewModel by viewModels()
    
    private var seriesId: Int = 0
    private var seriesName: String = ""
    
    private lateinit var episodeAdapter: EpisodeAdapter
    
    companion object {
        private const val ARG_SERIES_ID = "series_id"
        private const val ARG_SERIES_NAME = "series_name"
        
        fun newInstance(seriesId: Int, seriesName: String): SeriesDetailDialog {
            return SeriesDetailDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SERIES_ID, seriesId)
                    putString(ARG_SERIES_NAME, seriesName)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seriesId = arguments?.getInt(ARG_SERIES_ID) ?: 0
        seriesName = arguments?.getString(ARG_SERIES_NAME) ?: ""
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSeriesDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.tvSeriesName.text = seriesName
        
        episodeAdapter = EpisodeAdapter { episode ->
            playEpisode(episode)
        }
        
        binding.rvEpisodes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = episodeAdapter
        }
        
        observeViewModel()
        viewModel.loadSeriesInfo(seriesId)
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.seriesInfo.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.contentLayout.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.contentLayout.visibility = View.VISIBLE
                        
                        resource.data?.let { info ->
                            info.info?.let { seriesInfo ->
                                binding.tvSeriesName.text = seriesInfo.name
                                binding.tvPlot.text = seriesInfo.plot ?: "No description available"
                                binding.tvGenre.text = seriesInfo.genre ?: ""
                                binding.tvCast.text = seriesInfo.cast ?: ""
                                
                                Glide.with(requireContext())
                                    .load(seriesInfo.cover)
                                    .placeholder(R.drawable.ic_movie_placeholder)
                                    .into(binding.ivCover)
                            }
                            
                            // Load first season episodes by default
                            info.episodes?.let { episodesMap ->
                                val firstSeason = episodesMap.keys.firstOrNull()
                                firstSeason?.let { season ->
                                    episodesMap[season]?.let { episodes ->
                                        episodeAdapter.submitList(episodes)
                                    }
                                }
                            }
                            
                            // Setup season tabs
                            info.seasons?.let { seasons ->
                                binding.seasonChipGroup.removeAllViews()
                                seasons.forEach { season ->
                                    val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                                        text = "Season ${season.seasonNumber}"
                                        isCheckable = true
                                        setOnClickListener {
                                            info.episodes?.get(season.seasonNumber.toString())?.let { eps ->
                                                episodeAdapter.submitList(eps)
                                            }
                                        }
                                    }
                                    binding.seasonChipGroup.addView(chip)
                                }
                            }
                        }
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.tvPlot.text = resource.message
                    }
                    null -> {}
                }
            }
        }
    }
    
    private fun playEpisode(episode: Episode) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_STREAM_ID, episode.id?.toIntOrNull() ?: 0)
            putExtra(PlayerActivity.EXTRA_STREAM_NAME, "${seriesName} - ${episode.title}")
            putExtra(PlayerActivity.EXTRA_STREAM_TYPE, "series")
            putExtra(PlayerActivity.EXTRA_CONTAINER_EXT, episode.containerExtension)
        }
        startActivity(intent)
        dismiss()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
