package com.proiptv.app.ui.movies

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.proiptv.app.R
import com.proiptv.app.data.model.VodStream
import com.proiptv.app.databinding.ItemMovieBinding

class MovieAdapter(
    private val onMovieClick: (VodStream) -> Unit,
    private val onFavoriteClick: (VodStream) -> Unit
) : ListAdapter<VodStream, MovieAdapter.MovieViewHolder>(MovieDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovieViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class MovieViewHolder(
        private val binding: ItemMovieBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(movie: VodStream) {
            binding.tvMovieName.text = movie.name
            
            movie.rating5Based?.let { rating ->
                binding.tvRating.text = String.format("%.1f", rating)
            }
            
            Glide.with(binding.root.context)
                .load(movie.streamIcon)
                .placeholder(R.drawable.ic_movie_placeholder)
                .error(R.drawable.ic_movie_placeholder)
                .centerCrop()
                .into(binding.ivPoster)
            
            binding.ivFavorite.setImageResource(
                if (movie.isFavorite) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_border
            )
            
            binding.root.setOnClickListener {
                onMovieClick(movie)
            }
            
            binding.ivFavorite.setOnClickListener {
                onFavoriteClick(movie)
            }
        }
    }
    
    class MovieDiffCallback : DiffUtil.ItemCallback<VodStream>() {
        override fun areItemsTheSame(oldItem: VodStream, newItem: VodStream): Boolean {
            return oldItem.streamId == newItem.streamId
        }
        
        override fun areContentsTheSame(oldItem: VodStream, newItem: VodStream): Boolean {
            return oldItem == newItem
        }
    }
}
