package com.proiptv.app.ui.movies

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
import com.proiptv.app.data.model.VodCategory
import com.proiptv.app.data.model.VodStream
import com.proiptv.app.databinding.FragmentMoviesBinding
import com.proiptv.app.ui.player.PlayerActivity
import com.proiptv.app.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MoviesFragment : Fragment() {
    
    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MoviesViewModel by viewModels()
    
    private lateinit var categoryAdapter: MovieCategoryAdapter
    private lateinit var movieAdapter: MovieAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeViewModel()
        viewModel.loadData()
    }
    
    private fun setupRecyclerViews() {
        categoryAdapter = MovieCategoryAdapter { category ->
            viewModel.selectCategory(category)
        }
        
        movieAdapter = MovieAdapter(
            onMovieClick = { movie ->
                playMovie(movie)
            },
            onFavoriteClick = { movie ->
                viewModel.toggleFavorite(movie)
            }
        )
        
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
        
        binding.rvMovies.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = movieAdapter
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
                        
                        val allCategory = VodCategory("all", "All Movies", null)
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
            viewModel.movies.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.movieShimmer.visibility = View.VISIBLE
                        binding.movieShimmer.startShimmer()
                        binding.rvMovies.visibility = View.GONE
                        binding.tvNoMovies.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.movieShimmer.stopShimmer()
                        binding.movieShimmer.visibility = View.GONE
                        
                        val movies = resource.data ?: emptyList()
                        if (movies.isEmpty()) {
                            binding.rvMovies.visibility = View.GONE
                            binding.tvNoMovies.visibility = View.VISIBLE
                        } else {
                            binding.rvMovies.visibility = View.VISIBLE
                            binding.tvNoMovies.visibility = View.GONE
                            movieAdapter.submitList(movies)
                        }
                    }
                    is Resource.Error -> {
                        binding.movieShimmer.stopShimmer()
                        binding.movieShimmer.visibility = View.GONE
                        binding.tvNoMovies.visibility = View.VISIBLE
                        Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun playMovie(movie: VodStream) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_STREAM_ID, movie.streamId)
            putExtra(PlayerActivity.EXTRA_STREAM_NAME, movie.name)
            putExtra(PlayerActivity.EXTRA_STREAM_TYPE, "vod")
            putExtra(PlayerActivity.EXTRA_CONTAINER_EXT, movie.containerExtension)
        }
        startActivity(intent)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
