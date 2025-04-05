package com.yourstore.app.ui.customer.catalog.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.yourstore.app.R
import com.yourstore.app.data.model.Product
import com.yourstore.app.databinding.ItemProductBinding
import com.yourstore.app.util.CurrencyFormatter

/**
 * Адаптер для отображения списка товаров в RecyclerView.
 */
class ProductAdapter(
    private val onProductClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
    }

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onProductClick(getItem(position))
                }
            }
        }

        fun bind(product: Product) {
            binding.apply {
                tvProductName.text = product.name

                // Отображение цены и скидки
                if (product.discountPrice != null && product.discountPrice < product.price) {
                    // Есть скидка, показываем обе цены
                    tvPrice.text = CurrencyFormatter.format(product.discountPrice)
                    tvOriginalPrice.isVisible = true
                    tvOriginalPrice.text = CurrencyFormatter.format(product.price)
                    tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    tvDiscount.isVisible = true

                    // Рассчитываем процент скидки
                    val discountPercent = ((product.price - product.discountPrice) / product.price * 100).toInt()
                    tvDiscount.text = itemView.context.getString(R.string.discount_percent, discountPercent)
                } else {
                    // Нет скидки, показываем только основную цену
                    tvPrice.text = CurrencyFormatter.format(product.price)
                    tvOriginalPrice.isVisible = false
                    tvDiscount.isVisible = false
                }

                // Отображение статуса наличия
                if (product.availableQuantity > 0) {
                    tvAvailability.text = itemView.context.getString(R.string.in_stock)
                    tvAvailability.setTextColor(itemView.context.getColor(R.color.available))
                    btnAddToCart.isEnabled = true
                } else {
                    tvAvailability.text = itemView.context.getString(R.string.out_of_stock)
                    tvAvailability.setTextColor(itemView.context.getColor(R.color.unavailable))
                    btnAddToCart.isEnabled = false
                }

                // Загрузка изображения
                if (product.images.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(product.images[0])
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(ivProductImage)
                } else {
                    ivProductImage.setImageResource(R.drawable.placeholder_image)
                }

                // Отображение рейтинга
                if (product.reviewCount > 0) {
                    ratingBar.rating = product.rating
                    tvRatingCount.text = itemView.context.getString(
                        R.string.rating_count_short,
                        product.reviewCount
                    )
                    ratingLayout.visibility = View.VISIBLE
                } else {
                    ratingLayout.visibility = View.GONE
                }

                // Обработка нажатия на кнопку "Добавить в корзину"
                btnAddToCart.setOnClickListener {
                    // Реализация добавления в корзину будет в другом месте
                    // Здесь только уведомляем о клике на товар
                    onProductClick(product)
                }

                // Отображение иконки "Избранное"
                ivFavorite.isSelected = product.isFavorite
                ivFavorite.setOnClickListener {
                    // Обработка добавления/удаления из избранного будет в другом месте
                }
            }
        }
    }

    /**
     * DiffUtil.Callback для оптимизации обновлений списка товаров
     */
    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}