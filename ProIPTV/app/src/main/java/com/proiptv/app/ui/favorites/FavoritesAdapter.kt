package com.proiptv.app.ui.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.proiptv.app.R
import com.proiptv.app.data.local.FavoriteEntity
import com.proiptv.app.databinding.ItemFavoriteBinding

class FavoritesAdapter(
    private val onFavoriteClick: (FavoriteEntity) -> Unit,
    private val onRemoveClick: (FavoriteEntity) -> Unit
) : ListAdapter<FavoriteEntity, FavoritesAdapter.FavoriteViewHolder>(FavoriteDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class FavoriteViewHolder(
        private val binding: ItemFavoriteBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(favorite: FavoriteEntity) {
            binding.tvName.text = favorite.name
            binding.tvType.text = favorite.type.uppercase()
            
            val placeholder = when (favorite.type) {
                "live" -> R.drawable.ic_live_tv
                "vod" -> R.drawable.ic_movie
                "series" -> R.drawable.ic_series
                else -> R.drawable.ic_tv_logo
            }
            
            Glide.with(binding.root.context)
                .load(favorite.icon)
                .placeholder(placeholder)
                .error(placeholder)
                .centerCrop()
                .into(binding.ivIcon)
            
            binding.root.setOnClickListener {
                onFavoriteClick(favorite)
            }
            
            binding.btnRemove.setOnClickListener {
                onRemoveClick(favorite)
            }
        }
    }
    
    class FavoriteDiffCallback : DiffUtil.ItemCallback<FavoriteEntity>() {
        override fun areItemsTheSame(oldItem: FavoriteEntity, newItem: FavoriteEntity): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: FavoriteEntity, newItem: FavoriteEntity): Boolean {
            return oldItem == newItem
        }
    }
}
