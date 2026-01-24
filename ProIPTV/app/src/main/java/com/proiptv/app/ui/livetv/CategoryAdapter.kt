package com.proiptv.app.ui.livetv

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.proiptv.app.R
import com.proiptv.app.data.model.LiveCategory
import com.proiptv.app.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onCategoryClick: (LiveCategory) -> Unit
) : ListAdapter<LiveCategory, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {
    
    private var selectedCategoryId: String = "all"
    
    fun setSelectedCategory(categoryId: String) {
        val oldSelected = selectedCategoryId
        selectedCategoryId = categoryId
        
        currentList.forEachIndexed { index, category ->
            if (category.categoryId == oldSelected || category.categoryId == categoryId) {
                notifyItemChanged(index)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(category: LiveCategory) {
            binding.tvCategoryName.text = category.categoryName
            
            val isSelected = category.categoryId == selectedCategoryId
            binding.root.isSelected = isSelected
            
            if (isSelected) {
                binding.root.setCardBackgroundColor(
                    binding.root.context.getColor(R.color.primary)
                )
                binding.tvCategoryName.setTextColor(
                    binding.root.context.getColor(R.color.text_on_primary)
                )
            } else {
                binding.root.setCardBackgroundColor(
                    binding.root.context.getColor(R.color.surface_variant)
                )
                binding.tvCategoryName.setTextColor(
                    binding.root.context.getColor(R.color.text_primary)
                )
            }
            
            binding.root.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }
    
    class CategoryDiffCallback : DiffUtil.ItemCallback<LiveCategory>() {
        override fun areItemsTheSame(oldItem: LiveCategory, newItem: LiveCategory): Boolean {
            return oldItem.categoryId == newItem.categoryId
        }
        
        override fun areContentsTheSame(oldItem: LiveCategory, newItem: LiveCategory): Boolean {
            return oldItem == newItem
        }
    }
}
