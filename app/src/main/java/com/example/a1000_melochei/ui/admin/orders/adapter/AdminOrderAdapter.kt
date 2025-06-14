package com.example.a1000_melochei.ui.admin.orders.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.model.Order
import com.example.a1000_melochei.data.model.OrderStatus
import com.example.a1000_melochei.databinding.ItemAdminOrderBinding
import com.example.a1000_melochei.util.CurrencyFormatter
import com.example.a1000_melochei.util.DateUtils

/**
 * Адаптер для отображения списка заказов в панели администратора.
 * Позволяет кликать на заказ для перехода к деталям,
 * а также изменять статус заказа прямо из списка.
 */
class AdminOrderAdapter(
    private val onOrderClick: (Order) -> Unit,
    private val onOrderStatusChange: (Order, OrderStatus) -> Unit
) : ListAdapter<Order, AdminOrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemAdminOrderBinding.inflate(
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
        private val binding: ItemAdminOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onOrderClick(getItem(position))
                }
            }

            binding.btnChangeStatus.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showOrderStatusOptions(getItem(position))
                }
            }
        }

        fun bind(order: Order) {
            val context = binding.root.context

            binding.apply {
                // Номер и дата заказа
                tvOrderNumber.text = context.getString(R.string.order_number_value, order.number)
                tvOrderDate.text = DateUtils.formatDateTime(order.createdAt)

                // Данные клиента
                tvCustomerName.text = order.userName
                tvCustomerPhone.text = order.userPhone

                // Статус заказа
                updateOrderStatusUI(order.status, context)

                // Метод доставки
                tvDeliveryMethod.text = when (order.deliveryMethod) {
                    "delivery" -> context.getString(R.string.delivery)
                    "pickup" -> context.getString(R.string.pickup)
                    else -> order.deliveryMethod
                }

                // Адрес доставки или пункт самовывоза
                if (order.deliveryMethod == "delivery" && order.address != null) {
                    tvDeliveryAddress.text = order.address.address
                    tvDeliveryAddress.visibility = View.VISIBLE
                } else if (order.deliveryMethod == "pickup" && order.pickupPoint != null) {
                    val pickupText = when (order.pickupPoint) {
                        0 -> context.getString(R.string.pickup_point_1)
                        1 -> context.getString(R.string.pickup_point_2)
                        else -> context.getString(R.string.pickup_point_unknown)
                    }
                    tvDeliveryAddress.text = pickupText
                    tvDeliveryAddress.visibility = View.VISIBLE
                } else {
                    tvDeliveryAddress.visibility = View.GONE
                }

                // Количество товаров и общая сумма
                val itemsCount = order.items.size
                tvItemsCount.text = context.resources.getQuantityString(
                    R.plurals.items_count, itemsCount, itemsCount
                )
                tvOrderTotal.text = CurrencyFormatter.format(order.total)

                // Дата доставки
                if (order.deliveryDate != null) {
                    tvDeliveryDate.text = DateUtils.formatDate(order.deliveryDate)
                    tvDeliveryDate.visibility = View.VISIBLE
                } else {
                    tvDeliveryDate.visibility = View.GONE
                }

                // Управление видимостью кнопки изменения статуса
                btnChangeStatus.visibility = if (order.status == OrderStatus.CANCELLED) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }
        }

        private fun updateOrderStatusUI(status: OrderStatus, context: Context) {
            // Текст статуса
            binding.tvOrderStatus.text = when (status) {
                OrderStatus.PENDING -> context.getString(R.string.order_status_pending)
                OrderStatus.PROCESSING -> context.getString(R.string.order_status_processing)
                OrderStatus.SHIPPING -> context.getString(R.string.order_status_shipping)
                OrderStatus.DELIVERED -> context.getString(R.string.order_status_delivered)
                OrderStatus.COMPLETED -> context.getString(R.string.order_status_completed)
                OrderStatus.CANCELLED -> context.getString(R.string.order_status_cancelled)
            }

            // Цвет статуса
            val statusColor = when (status) {
                OrderStatus.PENDING -> R.color.status_pending
                OrderStatus.PROCESSING -> R.color.status_processing
                OrderStatus.SHIPPING -> R.color.status_shipping
                OrderStatus.DELIVERED, OrderStatus.COMPLETED -> R.color.status_completed
                OrderStatus.CANCELLED -> R.color.status_cancelled
            }
            binding.tvOrderStatus.setTextColor(ContextCompat.getColor(context, statusColor))

            // Фон статуса
            val statusBgColor = when (status) {
                OrderStatus.PENDING -> R.color.status_pending_bg
                OrderStatus.PROCESSING -> R.color.status_processing_bg
                OrderStatus.SHIPPING -> R.color.status_shipping_bg
                OrderStatus.DELIVERED, OrderStatus.COMPLETED -> R.color.status_completed_bg
                OrderStatus.CANCELLED -> R.color.status_cancelled_bg
            }
            binding.orderStatusBg.setBackgroundColor(ContextCompat.getColor(context, statusBgColor))
        }

        private fun showOrderStatusOptions(order: Order) {
            val context = binding.root.context
            val statusOptions = getAvailableStatusOptions(order.status)

            // Создаем список названий статусов для отображения
            val statusNames = statusOptions.map { status ->
                when (status) {
                    OrderStatus.PENDING -> context.getString(R.string.order_status_pending)
                    OrderStatus.PROCESSING -> context.getString(R.string.order_status_processing)
                    OrderStatus.SHIPPING -> context.getString(R.string.order_status_shipping)
                    OrderStatus.DELIVERED -> context.getString(R.string.order_status_delivered)
                    OrderStatus.COMPLETED -> context.getString(R.string.order_status_completed)
                    OrderStatus.CANCELLED -> context.getString(R.string.order_status_cancelled)
                }
            }.toTypedArray()

            // Показываем диалог выбора статуса
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(R.string.change_order_status)
                .setItems(statusNames) { _, which ->
                    val newStatus = statusOptions[which]
                    if (newStatus != order.status) {
                        onOrderStatusChange(order, newStatus)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        /**
         * Возвращает список доступных статусов для перехода из текущего статуса
         */
        private fun getAvailableStatusOptions(currentStatus: OrderStatus): List<OrderStatus> {
            return when (currentStatus) {
                OrderStatus.PENDING -> listOf(
                    OrderStatus.PROCESSING,
                    OrderStatus.CANCELLED
                )
                OrderStatus.PROCESSING -> listOf(
                    OrderStatus.SHIPPING,
                    OrderStatus.CANCELLED
                )
                OrderStatus.SHIPPING -> listOf(
                    OrderStatus.DELIVERED,
                    OrderStatus.CANCELLED
                )
                OrderStatus.DELIVERED -> listOf(
                    OrderStatus.COMPLETED
                )
                OrderStatus.COMPLETED -> emptyList() // Финальный статус
                OrderStatus.CANCELLED -> emptyList() // Финальный статус
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
                    oldItem.updatedAt == newItem.updatedAt
        }
    }
}