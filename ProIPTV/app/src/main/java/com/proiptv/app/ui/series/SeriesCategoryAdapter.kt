package com.proiptv.app.ui.series

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.proiptv.app.R
import com.proiptv.app.data.model.SeriesCategory
import com.proiptv.app.databinding.ItemCategoryBinding

class SeriesCategoryAdapter(
    private val onCategoryClick: (SeriesCategory) -> Unit
) : ListAdapter<SeriesCategory, SeriesCategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {
    
    private var selectedCategoryId: String = "all"
    
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
        
        fun bind(category: SeriesCategory) {
            binding.tvCategoryName.text = category.categoryName
            
            val isSelected = category.categoryId == selectedCategoryId
            
            if (isSelected) {
                binding.root.setCardBackgroundColor(binding.root.context.getColor(R.color.primary))
                binding.tvCategoryName.setTextColor(binding.root.context.getColor(R.color.text_on_primary))
            } else {
                binding.root.setCardBackgroundColor(binding.root.context.getColor(R.color.surface_variant))
                binding.tvCategoryName.setTextColor(binding.root.context.getColor(R.color.text_primary))
            }
            
            binding.root.setOnClickListener {
                val oldSelected = selectedCategoryId
                selectedCategoryId = category.categoryId
                
                currentList.forEachIndexed { index, cat ->
                    if (cat.categoryId == oldSelected || cat.categoryId == category.categoryId) {
                        notifyItemChanged(index)
                    }
                }
                
                onCategoryClick(category)
            }
        }
    }
    
    class CategoryDiffCallback : DiffUtil.ItemCallback<SeriesCategory>() {
        override fun areItemsTheSame(oldItem: SeriesCategory, newItem: SeriesCategory): Boolean {
            return oldItem.categoryId == newItem.categoryId
        }
        
        override fun areContentsTheSame(oldItem: SeriesCategory, newItem: SeriesCategory): Boolean {
            return oldItem == newItem
        }
    }
}
