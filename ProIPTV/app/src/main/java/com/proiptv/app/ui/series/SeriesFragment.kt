package com.proiptv.app.ui.series

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
import com.proiptv.app.data.model.Series
import com.proiptv.app.data.model.SeriesCategory
import com.proiptv.app.databinding.FragmentSeriesBinding
import com.proiptv.app.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SeriesFragment : Fragment() {
    
    private var _binding: FragmentSeriesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SeriesViewModel by viewModels()
    
    private lateinit var categoryAdapter: SeriesCategoryAdapter
    private lateinit var seriesAdapter: SeriesAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeriesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeViewModel()
        viewModel.loadData()
    }
    
    private fun setupRecyclerViews() {
        categoryAdapter = SeriesCategoryAdapter { category ->
            viewModel.selectCategory(category)
        }
        
        seriesAdapter = SeriesAdapter(
            onSeriesClick = { series ->
                showSeriesDetail(series)
            },
            onFavoriteClick = { series ->
                viewModel.toggleFavorite(series)
            }
        )
        
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
        
        binding.rvSeries.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = seriesAdapter
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.shimmerLayout.visibility = View.VISIBLE
                        binding.shimmerLayout.startShimmer()
                    }
                    is Resource.Success -> {
                        binding.shimmerLayout.stopShimmer()
                        binding.shimmerLayout.visibility = View.GONE
                        
                        val allCategory = SeriesCategory("all", "All Series", null)
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
            viewModel.series.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.seriesShimmer.visibility = View.VISIBLE
                        binding.seriesShimmer.startShimmer()
                        binding.rvSeries.visibility = View.GONE
                        binding.tvNoSeries.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.seriesShimmer.stopShimmer()
                        binding.seriesShimmer.visibility = View.GONE
                        
                        val seriesList = resource.data ?: emptyList()
                        if (seriesList.isEmpty()) {
                            binding.rvSeries.visibility = View.GONE
                            binding.tvNoSeries.visibility = View.VISIBLE
                        } else {
                            binding.rvSeries.visibility = View.VISIBLE
                            binding.tvNoSeries.visibility = View.GONE
                            seriesAdapter.submitList(seriesList)
                        }
                    }
                    is Resource.Error -> {
                        binding.seriesShimmer.stopShimmer()
                        binding.seriesShimmer.visibility = View.GONE
                        binding.tvNoSeries.visibility = View.VISIBLE
                        Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun showSeriesDetail(series: Series) {
        val dialog = SeriesDetailDialog.newInstance(series.seriesId, series.name ?: "")
        dialog.show(parentFragmentManager, "series_detail")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
