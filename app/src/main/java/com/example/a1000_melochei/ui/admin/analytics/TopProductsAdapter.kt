package com.example.a1000_melochei.ui.admin.analytics.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.a1000_melochei.R
import com.example.a1000_melochei.databinding.ItemTopProductBinding
import com.example.a1000_melochei.ui.admin.analytics.viewmodel.AnalyticsViewModel
import com.example.a1000_melochei.util.CurrencyFormatter

/**
 * Адаптер для отображения списка популярных товаров в аналитике
 */
class TopProductsAdapter : ListAdapter<AnalyticsViewModel.TopProductData, TopProductsAdapter.TopProductViewHolder>(TopProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopProductViewHolder {
        val binding = ItemTopProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TopProductViewHolder(private val binding: ItemTopProductBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AnalyticsViewModel.TopProductData) {
            binding.apply {
                // Заполняем данные о товаре
                tvProductName.text = item.product.name
                tvProductSales.text = itemView.context.getString(
                    R.string.sales_count,
                    item.salesCount.toString()
                )
                tvSalesAmount.text = CurrencyFormatter.format(item.salesAmount)

                // Загружаем изображение товара
                if (item.product.images.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(item.product.images[0])
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(ivProductImage)
                } else {
                    ivProductImage.setImageResource(R.drawable.placeholder_image)
                }

                // Устанавливаем позицию в рейтинге
                val position = adapterPosition + 1
                tvPosition.text = position.toString()
            }
        }
    }

    class TopProductDiffCallback : DiffUtil.ItemCallback<AnalyticsViewModel.TopProductData>() {
        override fun areItemsTheSame(
            oldItem: AnalyticsViewModel.TopProductData,
            newItem: AnalyticsViewModel.TopProductData
        ): Boolean {
            return oldItem.product.id == newItem.product.id
        }

        override fun areContentsTheSame(
            oldItem: AnalyticsViewModel.TopProductData,
            newItem: AnalyticsViewModel.TopProductData
        ): Boolean {
            return oldItem.salesCount == newItem.salesCount &&
                    oldItem.salesAmount == newItem.salesAmount
        }
    }
}