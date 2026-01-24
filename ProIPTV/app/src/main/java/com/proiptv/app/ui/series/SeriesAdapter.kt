package com.proiptv.app.ui.series

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.proiptv.app.R
import com.proiptv.app.data.model.Series
import com.proiptv.app.databinding.ItemMovieBinding

class SeriesAdapter(
    private val onSeriesClick: (Series) -> Unit,
    private val onFavoriteClick: (Series) -> Unit
) : ListAdapter<Series, SeriesAdapter.SeriesViewHolder>(SeriesDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesViewHolder {
        val binding = ItemMovieBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SeriesViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class SeriesViewHolder(
        private val binding: ItemMovieBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(series: Series) {
            binding.tvMovieName.text = series.name
            
            series.rating5Based?.let { rating ->
                binding.tvRating.text = String.format("%.1f", rating)
            }
            
            Glide.with(binding.root.context)
                .load(series.cover)
                .placeholder(R.drawable.ic_movie_placeholder)
                .error(R.drawable.ic_movie_placeholder)
                .centerCrop()
                .into(binding.ivPoster)
            
            binding.ivFavorite.setImageResource(
                if (series.isFavorite) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_border
            )
            
            binding.root.setOnClickListener {
                onSeriesClick(series)
            }
            
            binding.ivFavorite.setOnClickListener {
                onFavoriteClick(series)
            }
        }
    }
    
    class SeriesDiffCallback : DiffUtil.ItemCallback<Series>() {
        override fun areItemsTheSame(oldItem: Series, newItem: Series): Boolean {
            return oldItem.seriesId == newItem.seriesId
        }
        
        override fun areContentsTheSame(oldItem: Series, newItem: Series): Boolean {
            return oldItem == newItem
        }
    }
}
