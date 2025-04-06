package com.yourstore.app.ui.admin.products.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.yourstore.app.R
import com.yourstore.app.databinding.ItemProductImageBinding

/**
 * Адаптер для отображения списка изображений товара в режиме редактирования.
 * Позволяет удалять изображения.
 */
class ProductImageAdapter(
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<String, ProductImageAdapter.ImageViewHolder>(ImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemProductImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = getItem(position)
        holder.bind(imageUrl)
    }

    inner class ImageViewHolder(
        private val binding: ItemProductImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnRemoveImage.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
        }

        fun bind(imageUrl: String) {
            // Загружаем изображение с помощью Glide
            try {
                if (imageUrl.startsWith("http")) {
                    // Загрузка из сети
                    Glide.with(binding.root.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.ivProductImage)
                } else {
                    // Загрузка из локального Uri
                    Glide.with(binding.root.context)
                        .load(Uri.parse(imageUrl))
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.ivProductImage)
                }
            } catch (e: Exception) {
                binding.ivProductImage.setImageResource(R.drawable.placeholder_image)
            }
        }
    }

    /**
     * DiffUtil.Callback для оптимизации обновлений списка изображений
     */
    class ImageDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}