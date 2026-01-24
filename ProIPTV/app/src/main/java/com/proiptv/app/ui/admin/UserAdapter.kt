package com.proiptv.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.proiptv.app.R
import com.proiptv.app.databinding.ItemAdminUserBinding
import java.text.SimpleDateFormat
import java.util.*

class UserAdapter(
    private val onEditClick: (AdminUser) -> Unit,
    private val onDeleteClick: (AdminUser) -> Unit,
    private val onToggleStatus: (AdminUser) -> Unit
) : ListAdapter<AdminUser, UserAdapter.UserViewHolder>(UserDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemAdminUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class UserViewHolder(
        private val binding: ItemAdminUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        fun bind(user: AdminUser) {
            binding.tvUsername.text = user.username
            binding.tvMaxConnections.text = "Connections: ${user.maxConnections}"
            binding.tvExpiry.text = "Expires: ${dateFormat.format(Date(user.expiryDate))}"
            
            val now = System.currentTimeMillis()
            val isExpired = user.expiryDate <= now
            
            val statusText: String
            val statusColor: Int
            
            when {
                isExpired -> {
                    statusText = "EXPIRED"
                    statusColor = binding.root.context.getColor(R.color.admin_expired)
                }
                !user.isActive -> {
                    statusText = "DISABLED"
                    statusColor = binding.root.context.getColor(R.color.admin_disabled)
                }
                else -> {
                    statusText = "ACTIVE"
                    statusColor = binding.root.context.getColor(R.color.admin_active)
                }
            }
            
            binding.tvStatus.text = statusText
            binding.tvStatus.setTextColor(statusColor)
            binding.statusIndicator.setBackgroundColor(statusColor)
            
            if (user.isReseller) {
                binding.tvUserType.text = "RESELLER"
                binding.tvUserType.setTextColor(binding.root.context.getColor(R.color.primary))
            } else {
                binding.tvUserType.text = "USER"
                binding.tvUserType.setTextColor(binding.root.context.getColor(R.color.text_secondary))
            }
            
            binding.btnEdit.setOnClickListener {
                onEditClick(user)
            }
            
            binding.btnDelete.setOnClickListener {
                onDeleteClick(user)
            }
            
            binding.btnToggleStatus.setOnClickListener {
                onToggleStatus(user)
            }
            
            binding.btnToggleStatus.setImageResource(
                if (user.isActive) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
    }
    
    class UserDiffCallback : DiffUtil.ItemCallback<AdminUser>() {
        override fun areItemsTheSame(oldItem: AdminUser, newItem: AdminUser): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: AdminUser, newItem: AdminUser): Boolean {
            return oldItem == newItem
        }
    }
}
