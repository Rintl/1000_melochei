package com.example.a1000_melochei.ui.customer.cart.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.model.CartItem
import com.example.a1000_melochei.databinding.ItemCartBinding
import com.example.a1000_melochei.util.CurrencyFormatter

/**
 * Адаптер для отображения элементов корзины в RecyclerView.
 */
class CartItemAdapter(
    private val listener: CartItemListener
) : ListAdapter<CartItem, CartItemAdapter.CartItemViewHolder>(CartItemDiffCallback()) {

    interface CartItemListener {
        fun onItemClick(cartItem: CartItem)
        fun onQuantityChanged(cartItem: CartItem, newQuantity: Int)
        fun onRemoveClick(cartItem: CartItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartItemViewHolder, position: Int) {
        val cartItem = getItem(position)
        holder.bind(cartItem)
    }

    inner class CartItemViewHolder(
        private val binding: ItemCartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getItem(position))
                }
            }

            binding.btnRemove.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRemoveClick(getItem(position))
                }
            }

            binding.btnDecrease.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val cartItem = getItem(position)
                    val newQuantity = cartItem.quantity - 1
                    if (newQuantity >= 1) {
                        listener.onQuantityChanged(cartItem, newQuantity)
                    }
                }
            }

            binding.btnIncrease.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val cartItem = getItem(position)
                    val newQuantity = cartItem.quantity + 1
                    if (newQuantity <= cartItem.availableQuantity) {
                        listener.onQuantityChanged(cartItem, newQuantity)
                    }
                }
            }
        }

        fun bind(cartItem: CartItem) {
            binding.apply {
                // Название товара
                tvProductName.text = cartItem.name

                // Информация о количестве
                tvQuantity.text = cartItem.quantity.toString()

                // Доступность кнопок изменения количества
                btnDecrease.isEnabled = cartItem.quantity > 1
                btnIncrease.isEnabled = cartItem.quantity < cartItem.availableQuantity

                // Обработка цен
                if (cartItem.discountPrice != null && cartItem.discountPrice < cartItem.price) {
                    // Есть скидка, показываем обе цены
                    val itemDiscountPrice = cartItem.discountPrice * cartItem.quantity
                    tvPrice.text = CurrencyFormatter.format(itemDiscountPrice)

                    tvOriginalPrice.isVisible = true
                    val itemOriginalPrice = cartItem.price * cartItem.quantity
                    tvOriginalPrice.text = CurrencyFormatter.format(itemOriginalPrice)
                    tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

                    // Показываем лейбл скидки
                    tvDiscount.isVisible = true
                    val discountPercent = ((cartItem.price - cartItem.discountPrice) / cartItem.price * 100).toInt()
                    tvDiscount.text = itemView.context.getString(R.string.discount_percent, discountPercent)
                } else {
                    // Нет скидки, показываем только основную цену
                    val itemPrice = cartItem.price * cartItem.quantity
                    tvPrice.text = CurrencyFormatter.format(itemPrice)
                    tvOriginalPrice.isVisible = false
                    tvDiscount.isVisible = false
                }

                // Загрузка изображения
                if (cartItem.imageUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(cartItem.imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(ivProductImage)
                } else {
                    ivProductImage.setImageResource(R.drawable.placeholder_image)
                }

                // Показываем предупреждение, если количество близко к максимально доступному
                if (cartItem.quantity == cartItem.availableQuantity) {
                    tvMaxQuantityWarning.isVisible = true
                    tvMaxQuantityWarning.text = itemView.context.getString(R.string.max_quantity_reached)
                } else {
                    tvMaxQuantityWarning.isVisible = false
                }
            }
        }
    }

    /**
     * DiffUtil.Callback для оптимизации обновлений списка элементов корзины
     */
    class CartItemDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.productId == newItem.productId &&
                    oldItem.quantity == newItem.quantity &&
                    oldItem.price == newItem.price &&
                    oldItem.discountPrice == newItem.discountPrice
        }
    }
}