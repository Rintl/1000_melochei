package com.yourstore.app.ui.admin.categories.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.yourstore.app.R
import com.yourstore.app.data.model.Category
import com.yourstore.app.databinding.ItemAdminCategoryBinding

/**
 * Адаптер для отображения списка категорий в админ-панели.
 * Позволяет просматривать, редактировать и удалять категории.
 */
class AdminCategoryAdapter(
    private val onItemClick: (Category) -> Unit,
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit
) : ListAdapter<Category, AdminCategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemAdminCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)
        holder.bind(category)
    }

    inner class CategoryViewHolder(
        private val binding: ItemAdminCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.btnEditCategory.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(getItem(position))
                }
            }

            binding.btnDeleteCategory.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
        }

        fun bind(category: Category) {
            binding.apply {
                // Название категории
                tvCategoryName.text = category.name

                // Описание категории
                if (category.description.isNotEmpty()) {
                    tvCategoryDescription.text = category.description
                    tvCategoryDescription.isVisible = true
                } else {
                    tvCategoryDescription.isVisible = false
                }

                // Количество товаров
                tvProductCount.text = itemView.context.getString(
                    R.string.category_items_count,
                    category.productCount
                )

                // Статус категории (отображается/скрыта)
                tvCategoryStatus.text = if (category.isActive) {
                    itemView.context.getString(R.string.category_active)
                } else {
                    itemView.context.getString(R.string.category_inactive)
                }

                // Индикатор статуса
                statusIndicator.setBackgroundResource(
                    if (category.isActive) R.color.status_active else R.color.status_inactive
                )

                // Загрузка изображения категории
                if (category.imageUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(category.imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(ivCategoryImage)
                } else {
                    // Устанавливаем изображение по умолчанию, если нет URL
                    ivCategoryImage.setImageResource(R.drawable.placeholder_image)
                }
            }
        }
    }

    /**
     * DiffUtil.Callback для оптимизации обновлений списка категорий
     */
    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.name == newItem.name &&
                    oldItem.description == newItem.description &&
                    oldItem.imageUrl == newItem.imageUrl &&
                    oldItem.isActive == newItem.isActive &&
                    oldItem.productCount == newItem.productCount
        }
    }
}