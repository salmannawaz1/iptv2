package com.proiptv.app.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.proiptv.app.data.local.FavoriteEntity
import com.proiptv.app.databinding.FragmentFavoritesBinding
import com.proiptv.app.ui.player.PlayerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : Fragment() {
    
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: FavoritesViewModel by viewModels()
    
    private lateinit var adapter: FavoritesAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        adapter = FavoritesAdapter(
            onFavoriteClick = { favorite ->
                playFavorite(favorite)
            },
            onRemoveClick = { favorite ->
                viewModel.removeFavorite(favorite.id)
            }
        )
        
        binding.rvFavorites.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = this@FavoritesFragment.adapter
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favorites.collectLatest { favorites ->
                if (favorites.isEmpty()) {
                    binding.rvFavorites.visibility = View.GONE
                    binding.emptyView.visibility = View.VISIBLE
                } else {
                    binding.rvFavorites.visibility = View.VISIBLE
                    binding.emptyView.visibility = View.GONE
                    adapter.submitList(favorites)
                }
            }
        }
    }
    
    private fun playFavorite(favorite: FavoriteEntity) {
        val streamId = favorite.id.substringAfter("_").toIntOrNull() ?: return
        
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_STREAM_ID, streamId)
            putExtra(PlayerActivity.EXTRA_STREAM_NAME, favorite.name)
            putExtra(PlayerActivity.EXTRA_STREAM_TYPE, favorite.type)
        }
        startActivity(intent)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
