package com.yourstore.app.ui.customer.catalog.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.yourstore.app.R
import com.yourstore.app.data.model.Category
import com.yourstore.app.databinding.ItemCategoryBinding

/**
 * Адаптер для отображения списка категорий в RecyclerView.
 */
class CategoryAdapter(
    private val onCategoryClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
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
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCategoryClick(getItem(position))
                }
            }
        }

        fun bind(category: Category) {
            binding.apply {
                // Установка названия категории
                tvCategoryName.text = category.name

                // Установка описания категории если оно есть
                if (category.description.isNotEmpty()) {
                    tvCategoryDescription.text = category.description
                } else {
                    tvCategoryDescription.text = itemView.context.getString(
                        R.string.category_items_count,
                        category.productCount
                    )
                }

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
            return oldItem == newItem
        }
    }
}