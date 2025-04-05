package com.yourstore.app.ui.customer.orders

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Order
import com.yourstore.app.data.model.OrderStatus
import com.yourstore.app.databinding.ActivityOrderDetailBinding
import com.yourstore.app.ui.customer.OrderItemAdapter
import com.yourstore.app.ui.customer.orders.viewmodel.OrderViewModel
import com.yourstore.app.util.CurrencyFormatter
import com.yourstore.app.util.DateUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Активность для отображения подробной информации о заказе.
 * Позволяет просматривать статус, содержимое, адрес доставки и другие детали заказа.
 */
class OrderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailBinding
    private val viewModel: OrderViewModel by viewModel()

    private lateinit var orderItemAdapter: OrderItemAdapter
    private var orderId: String = ""
    private var currentOrder: Order? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получаем ID заказа из переданных данных
        orderId = intent.getStringExtra("ORDER_ID") ?: ""
        if (orderId.isEmpty()) {
            Toast.makeText(this, R.string.error_invalid_order, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Загружаем информацию о заказе
        viewModel.loadOrderDetails(orderId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.order_detail_title)
    }

    private fun setupRecyclerView() {
        orderItemAdapter = OrderItemAdapter()
        binding.rvOrderItems.apply {
            layoutManager = LinearLayoutManager(this@OrderDetailActivity)
            adapter = orderItemAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupObservers() {
        // Наблюдение за данными заказа
        viewModel.orderDetails.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                    showError(false)
                }
                is Resource.Success -> {
                    showLoading(false)
                    showError(false)

                    result.data?.let { order ->
                        currentOrder = order
                        updateOrderUI(order)
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(true, result.message)
                }
            }
        })

        // Наблюдение за результатом отмены заказа
        viewModel.cancelOrderResult.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.btnCancelOrder.isEnabled = false
                    binding.cancelOrderProgressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.btnCancelOrder.isEnabled = true
                    binding.cancelOrderProgressBar.visibility = View.GONE

                    Toast.makeText(this, R.string.order_cancelled_success, Toast.LENGTH_SHORT).show()

                    // Перезагружаем детали заказа
                    viewModel.loadOrderDetails(orderId)
                }
                is Resource.Error -> {
                    binding.btnCancelOrder.isEnabled = true
                    binding.cancelOrderProgressBar.visibility = View.GONE

                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_cancelling_order),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun setupListeners() {
        // Кнопка отмены заказа
        binding.btnCancelOrder.setOnClickListener {
            showCancelOrderConfirmation()
        }

        // Кнопка связи с магазином
        binding.btnContactSupport.setOnClickListener {
            showContactOptions()
        }

        // Кнопка повторения заказа
        binding.btnReorder.setOnClickListener {
            currentOrder?.let { order ->
                viewModel.reorderItems(order)
                Toast.makeText(this, R.string.items_added_to_cart, Toast.LENGTH_SHORT).show()
                navigateToCart()
            }
        }

        // Кнопка повторной попытки загрузки при ошибке
        binding.btnRetry.setOnClickListener {
            viewModel.loadOrderDetails(orderId)
        }
    }

    private fun updateOrderUI(order: Order) {
        // Номер и дата заказа
        binding.tvOrderNumber.text = getString(R.string.order_number_value, order.number)
        binding.tvOrderDate.text = DateUtils.formatDateTime(order.createdAt)

        // Статус заказа
        updateOrderStatus(order.status)

        // Детали доставки
        binding.tvDeliveryMethod.text = when (order.deliveryMethod) {
            "delivery" -> getString(R.string.delivery)
            "pickup" -> getString(R.string.pickup)
            else -> order.deliveryMethod
        }

        // Адрес доставки или пункт самовывоза
        if (order.deliveryMethod == "delivery" && order.address != null) {
            binding.addressLayout.visibility = View.VISIBLE
            binding.tvAddress.text = order.address.address
            binding.pickupPointLayout.visibility = View.GONE
        } else if (order.deliveryMethod == "pickup" && order.pickupPoint != null) {
            binding.addressLayout.visibility = View.GONE
            binding.pickupPointLayout.visibility = View.VISIBLE
            binding.tvPickupPoint.text = when (order.pickupPoint) {
                0 -> getString(R.string.pickup_point_1)
                1 -> getString(R.string.pickup_point_2)
                else -> getString(R.string.pickup_point_unknown)
            }
        } else {
            binding.addressLayout.visibility = View.GONE
            binding.pickupPointLayout.visibility = View.GONE
        }

        // Дата и время доставки
        if (order.deliveryDate != null) {
            binding.tvDeliveryDate.text = DateUtils.formatDate(order.deliveryDate)
            binding.tvDeliveryTime.text = DateUtils.formatTime(order.deliveryDate)
            binding.deliveryDateTimeLayout.visibility = View.VISIBLE
        } else {
            binding.deliveryDateTimeLayout.visibility = View.GONE
        }

        // Способ оплаты
        binding.tvPaymentMethod.text = when (order.paymentMethod) {
            "cash" -> getString(R.string.payment_cash)
            "card" -> getString(R.string.payment_card)
            "kaspi" -> getString(R.string.payment_kaspi)
            else -> order.paymentMethod
        }

        // Комментарий к заказу
        if (order.comment.isNotEmpty()) {
            binding.tvComment.text = order.comment
            binding.commentLayout.visibility = View.VISIBLE
        } else {
            binding.commentLayout.visibility = View.GONE
        }

        // Товары заказа
        orderItemAdapter.submitList(order.items)

        // Итоговые суммы
        binding.tvSubtotal.text = CurrencyFormatter.format(order.subtotal)
        binding.tvDelivery.text = if (order.deliveryFee > 0) {
            CurrencyFormatter.format(order.deliveryFee)
        } else {
            getString(R.string.free)
        }
        binding.tvTotal.text = CurrencyFormatter.format(order.total)

        // Управление видимостью кнопок в зависимости от статуса заказа
        val canCancel = order.status == OrderStatus.PENDING || order.status == OrderStatus.PROCESSING
        binding.btnCancelOrder.isVisible = canCancel
    }

    private fun updateOrderStatus(status: OrderStatus) {
        // Текст статуса
        binding.tvOrderStatus.text = when (status) {
            OrderStatus.PENDING -> getString(R.string.order_status_pending)
            OrderStatus.PROCESSING -> getString(R.string.order_status_processing)
            OrderStatus.SHIPPING -> getString(R.string.order_status_shipping)
            OrderStatus.DELIVERED -> getString(R.string.order_status_delivered)
            OrderStatus.COMPLETED -> getString(R.string.order_status_completed)
            OrderStatus.CANCELLED -> getString(R.string.order_status_cancelled)
        }

        // Цвет статуса
        val statusColor = when (status) {
            OrderStatus.PENDING -> R.color.status_pending
            OrderStatus.PROCESSING -> R.color.status_processing
            OrderStatus.SHIPPING -> R.color.status_shipping
            OrderStatus.DELIVERED, OrderStatus.COMPLETED -> R.color.status_completed
            OrderStatus.CANCELLED -> R.color.status_cancelled
        }
        binding.tvOrderStatus.setTextColor(getColor(statusColor))

        // Прогресс заказа
        binding.orderProgressLayout.isVisible = status != OrderStatus.CANCELLED
        when (status) {
            OrderStatus.PENDING -> {
                binding.progressStep1.setImageResource(R.drawable.ic_step_active)
                binding.progressStep2.setImageResource(R.drawable.ic_step_inactive)
                binding.progressStep3.setImageResource(R.drawable.ic_step_inactive)
                binding.progressStep4.setImageResource(R.drawable.ic_step_inactive)
                binding.progressLine1.setBackgroundResource(R.color.progress_inactive)
                binding.progressLine2.setBackgroundResource(R.color.progress_inactive)
                binding.progressLine3.setBackgroundResource(R.color.progress_inactive)
            }
            OrderStatus.PROCESSING -> {
                binding.progressStep1.setImageResource(R.drawable.ic_step_completed)
                binding.progressStep2.setImageResource(R.drawable.ic_step_active)
                binding.progressStep3.setImageResource(R.drawable.ic_step_inactive)
                binding.progressStep4.setImageResource(R.drawable.ic_step_inactive)
                binding.progressLine1.setBackgroundResource(R.color.progress_active)
                binding.progressLine2.setBackgroundResource(R.color.progress_inactive)
                binding.progressLine3.setBackgroundResource(R.color.progress_inactive)
            }
            OrderStatus.SHIPPING -> {
                binding.progressStep1.setImageResource(R.drawable.ic_step_completed)
                binding.progressStep2.setImageResource(R.drawable.ic_step_completed)
                binding.progressStep3.setImageResource(R.drawable.ic_step_active)
                binding.progressStep4.setImageResource(R.drawable.ic_step_inactive)
                binding.progressLine1.setBackgroundResource(R.color.progress_active)
                binding.progressLine2.setBackgroundResource(R.color.progress_active)
                binding.progressLine3.setBackgroundResource(R.color.progress_inactive)
            }
            OrderStatus.DELIVERED, OrderStatus.COMPLETED -> {
                binding.progressStep1.setImageResource(R.drawable.ic_step_completed)
                binding.progressStep2.setImageResource(R.drawable.ic_step_completed)
                binding.progressStep3.setImageResource(R.drawable.ic_step_completed)
                binding.progressStep4.setImageResource(R.drawable.ic_step_completed)
                binding.progressLine1.setBackgroundResource(R.color.progress_active)
                binding.progressLine2.setBackgroundResource(R.color.progress_active)
                binding.progressLine3.setBackgroundResource(R.color.progress_active)
            }
            else -> {
                // Для отмененных заказов прогресс не показываем
            }
        }
    }

    private fun showCancelOrderConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.cancel_order_title)
            .setMessage(R.string.cancel_order_message)
            .setPositiveButton(R.string.cancel_order) { _, _ ->
                viewModel.cancelOrder(orderId)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showContactOptions() {
        val options = arrayOf(
            getString(R.string.contact_by_phone),
            getString(R.string.contact_by_email)
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.contact_support)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Звонок в магазин
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:+77771234567") // Номер магазина
                        }
                        startActivity(intent)
                    }
                    1 -> {
                        // Отправка email
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@1000melochei.kz") // Email магазина
                            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.order_inquiry_subject, orderId))
                        }
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    private fun navigateToCart() {
        val intent = Intent().apply {
            putExtra("NAVIGATE_TO_CART", true)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.scrollView.isVisible = !isLoading && !binding.errorLayout.isVisible
    }

    private fun showError(isError: Boolean, message: String? = null) {
        binding.errorLayout.isVisible = isError
        binding.errorMessage.text = message ?: getString(R.string.error_loading_order_details)
        binding.scrollView.isVisible = !isError && !binding.progressBar.isVisible

        binding.btnRetry.setOnClickListener {
            viewModel.loadOrderDetails(orderId)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}