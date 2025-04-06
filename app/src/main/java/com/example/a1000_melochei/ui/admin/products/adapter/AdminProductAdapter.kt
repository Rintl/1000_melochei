package com.yourstore.app.ui.admin.products.adapter

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
import com.yourstore.app.databinding.ItemAdminProductBinding
import com.yourstore.app.util.CurrencyFormatter

/**
 * Адаптер для отображения списка товаров в административной панели.
 * Отличается от обычного ProductAdapter наличием кнопок управления
 * (редактирование, удаление, изменение статуса).
 */
class AdminProductAdapter(
    private val onItemClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit,
    private val onStatusToggleClick: (Product, Boolean) -> Unit
) : ListAdapter<Product, AdminProductAdapter.AdminProductViewHolder>(AdminProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminProductViewHolder {
        val binding = ItemAdminProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AdminProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
    }

    inner class AdminProductViewHolder(
        private val binding: ItemAdminProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.btnDelete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }

            binding.switchAvailability.setOnCheckedChangeListener { _, isChecked ->
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val product = getItem(position)
                    // Проверяем, изменилось ли состояние
                    if ((isChecked && product.availableQuantity <= 0) ||
                        (!isChecked && product.availableQuantity > 0)) {
                        onStatusToggleClick(product, isChecked)
                    }
                }
            }
        }

        fun bind(product: Product) {
            binding.apply {
                // Название товара
                tvProductName.text = product.name

                // SKU
                tvSku.text = product.sku

                // Категория
                tvCategory.text = product.categoryName

                // Отображение цены и скидки
                if (product.discountPrice != null && product.discountPrice < product.price) {
                    // Есть скидка, показываем обе цены
                    tvPrice.text = CurrencyFormatter.format(product.discountPrice)
                    tvOriginalPrice.isVisible = true
                    tvOriginalPrice.text = CurrencyFormatter.format(product.price)
                    tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    ivDiscount.visibility = View.VISIBLE
                } else {
                    // Нет скидки, показываем только основную цену
                    tvPrice.text = CurrencyFormatter.format(product.price)
                    tvOriginalPrice.isVisible = false
                    ivDiscount.visibility = View.GONE
                }

                // Отображение статуса наличия
                val isInStock = product.availableQuantity > 0
                tvStock.text = if (isInStock) {
                    itemView.context.getString(R.string.in_stock_with_count, product.availableQuantity)
                } else {
                    itemView.context.getString(R.string.out_of_stock)
                }

                // Цвет статуса
                val textColor = if (isInStock) {
                    itemView.context.getColor(R.color.available)
                } else {
                    itemView.context.getColor(R.color.unavailable)
                }
                tvStock.setTextColor(textColor)

                // Установка переключателя наличия, не вызывая обработчик
                switchAvailability.setOnCheckedChangeListener(null)
                switchAvailability.isChecked = isInStock
                switchAvailability.setOnCheckedChangeListener { _, isChecked ->
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onStatusToggleClick(getItem(position), isChecked)
                    }
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

                // Отображение индикатора популярности
                val isPopular = product.soldCount > 10 || product.viewCount > 100
                ivPopular.isVisible = isPopular
            }
        }
    }

    /**
     * DiffUtil.Callback для оптимизации обновлений списка товаров
     */
    class AdminProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.name == newItem.name &&
                    oldItem.price == newItem.price &&
                    oldItem.discountPrice == newItem.discountPrice &&
                    oldItem.availableQuantity == newItem.availableQuantity &&
                    oldItem.images == newItem.images
        }
    }
}