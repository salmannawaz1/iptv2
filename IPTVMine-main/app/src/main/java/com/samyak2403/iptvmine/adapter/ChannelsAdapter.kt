/*
 * Created by Samyak Kamble on 8/14/24, 12:18 PM
 *  Copyright (c) 2024. All rights reserved.
 *  Last modified 8/14/24, 12:18 PM
 */

package com.samyak2403.iptvmine.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.model.Channel
import com.samyak2403.iptvmine.utils.TvUtils

class ChannelsAdapter(
    private val onChannelClicked: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelsAdapter.ChannelViewHolder>() {

    private var channels: MutableList<Channel> = mutableListOf()
    private var isTvMode: Boolean = false
    private var selectedPosition: Int = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        // Detect TV mode on first view holder creation
        isTvMode = TvUtils.isTvMode(parent.context)
        
        // Use TV layout for TV mode, mobile layout otherwise
        val layoutId = if (isTvMode) {
            R.layout.item_channel_televison
        } else {
            R.layout.item_channel
        }
        
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(channels[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = channels.size

    fun updateChannels(newChannels: List<Channel>) {
        val diffResult = DiffUtil.calculateDiff(ChannelDiffCallback(channels, newChannels))
        channels.clear()
        channels.addAll(newChannels)
        diffResult.dispatchUpdatesTo(this)
    }

    fun getChannels(): List<Channel> = channels

    inner class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val logoImageView: ImageView? = itemView.findViewById(R.id.logoImageView)
        private val channelIcon: ImageView? = itemView.findViewById(R.id.channelIcon)
        private val nameTextView: TextView? = itemView.findViewById(R.id.nameTextView)
        private val channelName: TextView? = itemView.findViewById(R.id.channelName)
        private val categoryTextView: TextView? = itemView.findViewById(R.id.categoryTextView)
        private val channelCategory: TextView? = itemView.findViewById(R.id.channelCategory)

        fun bind(channel: Channel, isSelected: Boolean) {
            // Support both mobile and TV layouts
            nameTextView?.text = channel.name
            channelName?.text = channel.name
            categoryTextView?.text = channel.category
            channelCategory?.text = channel.category
            
            // Load image into appropriate ImageView
            val targetImageView = logoImageView ?: channelIcon
            targetImageView?.let {
                Glide.with(itemView.context)
                    .load(channel.logoUrl)
                    .placeholder(R.drawable.ic_tv)
                    .error(R.drawable.ic_tv)
                    .into(it)
            }

            // Enable focus for TV navigation
            itemView.isFocusable = true
            itemView.isFocusableInTouchMode = false
            itemView.isSelected = isSelected
            
            itemView.setOnClickListener {
                // Update selection
                val previousPosition = selectedPosition
                selectedPosition = bindingAdapterPosition
                
                // Notify changes for visual update
                if (previousPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousPosition)
                }
                notifyItemChanged(selectedPosition)
                
                // Trigger callback
                onChannelClicked(channel)
            }
            
            // Handle focus change for TV with enhanced visual feedback
            itemView.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    // Scale up and elevate when focused for better UX
                    view.animate()
                        .scaleX(if (isTvMode) 1.08f else 1.05f)
                        .scaleY(if (isTvMode) 1.08f else 1.05f)
                        .setDuration(200)
                        .start()
                    view.elevation = if (isTvMode) 16f else 8f
                    
                    // Bring to front for TV
                    if (isTvMode) {
                        view.bringToFront()
                        (view.parent as? View)?.invalidate()
                    }
                } else {
                    // Scale back to normal
                    view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(200)
                        .start()
                    view.elevation = if (isTvMode) 4f else 0f
                }
            }
            
            // Handle key events for TV (Enter/D-pad center to select)
            if (isTvMode) {
                itemView.setOnKeyListener { _, keyCode, event ->
                    if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER -> {
                                itemView.performClick()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
            }
        }
    }

    private class ChannelDiffCallback(
        private val oldList: List<Channel>,
        private val newList: List<Channel>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Assuming the name is unique for each channel
            return oldList[oldItemPosition].name == newList[newItemPosition].name
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Check if contents are the same, including the logoUrl and streamUrl
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
