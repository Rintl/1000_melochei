package com.yourstore.app.ui.customer.orders.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yourstore.app.R
import com.yourstore.app.data.model.Order
import com.yourstore.app.data.model.OrderStatus
import com.yourstore.app.databinding.ItemOrderBinding
import com.yourstore.app.util.CurrencyFormatter
import com.yourstore.app.util.DateUtils

/**
 * Адаптер для отображения списка заказов в RecyclerView.
 */
class OrderAdapter(
    private val onOrderClick: (Order) -> Unit
) : ListAdapter<Order, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = getItem(position)
        holder.bind(order)
    }

    inner class OrderViewHolder(
        private val binding: ItemOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onOrderClick(getItem(position))
                }
            }
        }

        fun bind(order: Order) {
            binding.apply {
                // Номер и дата заказа
                tvOrderNumber.text = itemView.context.getString(R.string.order_number_value, order.number)
                tvOrderDate.text = DateUtils.formatDateTime(order.createdAt)

                // Статус заказа
                tvOrderStatus.text = when (order.status) {
                    OrderStatus.PENDING -> itemView.context.getString(R.string.order_status_pending)
                    OrderStatus.PROCESSING -> itemView.context.getString(R.string.order_status_processing)
                    OrderStatus.SHIPPING -> itemView.context.getString(R.string.order_status_shipping)
                    OrderStatus.DELIVERED -> itemView.context.getString(R.string.order_status_delivered)
                    OrderStatus.COMPLETED -> itemView.context.getString(R.string.order_status_completed)
                    OrderStatus.CANCELLED -> itemView.context.getString(R.string.order_status_cancelled)
                }

                // Цвет статуса
                val statusColor = when (order.status) {
                    OrderStatus.PENDING -> R.color.status_pending
                    OrderStatus.PROCESSING -> R.color.status_processing
                    OrderStatus.SHIPPING -> R.color.status_shipping
                    OrderStatus.DELIVERED, OrderStatus.COMPLETED -> R.color.status_completed
                    OrderStatus.CANCELLED -> R.color.status_cancelled
                }
                tvOrderStatus.setTextColor(ContextCompat.getColor(itemView.context, statusColor))

                // Сумма заказа
                tvOrderTotal.text = CurrencyFormatter.format(order.total)

                // Количество товаров
                val itemsCount = order.items.size
                tvItemsCount.text = itemView.context.resources.getQuantityString(
                    R.plurals.items_count, itemsCount, itemsCount
                )

                // Способ доставки
                tvDeliveryMethod.text = when (order.deliveryMethod) {
                    "delivery" -> itemView.context.getString(R.string.delivery)
                    "pickup" -> itemView.context.getString(R.string.pickup)
                    else -> order.deliveryMethod
                }

                // Дата доставки, если есть
                if (order.deliveryDate != null) {
                    tvDeliveryDate.text = DateUtils.formatDate(order.deliveryDate)
                } else {
                    tvDeliveryDate.text = itemView.context.getString(R.string.not_specified)
                }
            }
        }
    }

    /**
     * DiffUtil.Callback для оптимизации обновлений списка заказов
     */
    class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.status == newItem.status &&
                    oldItem.total == newItem.total &&
                    oldItem.deliveryDate == newItem.deliveryDate
        }
    }
}