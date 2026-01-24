package com.proiptv.app.ui.livetv

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.proiptv.app.R
import com.proiptv.app.data.model.LiveStream
import com.proiptv.app.databinding.ItemChannelBinding

class ChannelAdapter(
    private val onChannelClick: (LiveStream) -> Unit,
    private val onFavoriteClick: (LiveStream) -> Unit
) : ListAdapter<LiveStream, ChannelAdapter.ChannelViewHolder>(ChannelDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChannelViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ChannelViewHolder(
        private val binding: ItemChannelBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(channel: LiveStream) {
            binding.tvChannelName.text = channel.name
            
            Glide.with(binding.root.context)
                .load(channel.streamIcon)
                .placeholder(R.drawable.ic_tv_logo)
                .error(R.drawable.ic_tv_logo)
                .centerCrop()
                .into(binding.ivChannelLogo)
            
            binding.ivFavorite.setImageResource(
                if (channel.isFavorite) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_border
            )
            
            binding.root.setOnClickListener {
                onChannelClick(channel)
            }
            
            binding.ivFavorite.setOnClickListener {
                onFavoriteClick(channel)
            }
        }
    }
    
    class ChannelDiffCallback : DiffUtil.ItemCallback<LiveStream>() {
        override fun areItemsTheSame(oldItem: LiveStream, newItem: LiveStream): Boolean {
            return oldItem.streamId == newItem.streamId
        }
        
        override fun areContentsTheSame(oldItem: LiveStream, newItem: LiveStream): Boolean {
            return oldItem == newItem
        }
    }
}
