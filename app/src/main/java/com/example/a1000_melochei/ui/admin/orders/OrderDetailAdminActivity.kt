package com.example.a1000_melochei.ui.admin.orders

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Order
import com.example.a1000_melochei.data.model.OrderStatus
import com.example.a1000_melochei.data.model.User
import com.example.a1000_melochei.databinding.ActivityOrderDetailAdminBinding
import com.example.a1000_melochei.ui.admin.orders.adapter.OrderItemAdminAdapter
import com.example.a1000_melochei.ui.admin.orders.viewmodel.AdminOrderViewModel
import com.example.a1000_melochei.util.CurrencyFormatter
import com.example.a1000_melochei.util.DateUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Активность для детального просмотра заказа администратором.
 * Позволяет просматривать информацию о заказе, изменять его статус,
 * связываться с клиентом и другие административные функции.
 */
class OrderDetailAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailAdminBinding
    private val viewModel: AdminOrderViewModel by viewModel()

    private lateinit var orderItemsAdapter: OrderItemAdminAdapter
    private var orderId: String = ""
    private var currentOrder: Order? = null
    private var customer: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailAdminBinding.inflate(layoutInflater)
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
        setupStatusSpinner()
        setupObservers()
        setupListeners()

        // Загружаем информацию о заказе и клиенте
        viewModel.loadOrderDetails(orderId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.order_detail_title_admin)
    }

    private fun setupRecyclerView() {
        orderItemsAdapter = OrderItemAdminAdapter()
        binding.rvOrderItems.apply {
            layoutManager = LinearLayoutManager(this@OrderDetailAdminActivity)
            adapter = orderItemsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupStatusSpinner() {
        val statusOptions = arrayOf(
            getString(R.string.order_status_pending),
            getString(R.string.order_status_processing),
            getString(R.string.order_status_shipping),
            getString(R.string.order_status_delivered),
            getString(R.string.order_status_completed),
            getString(R.string.order_status_cancelled)
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            statusOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerStatus.adapter = adapter
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

                        // Загружаем информацию о клиенте
                        viewModel.loadCustomerDetails(order.userId)
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(true, result.message)
                }
            }
        })

        // Наблюдение за данными клиента
        viewModel.customerDetails.observe(this, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    result.data?.let { user ->
                        customer = user
                        updateCustomerUI(user)
                    }
                }
                is Resource.Error -> {
                    binding.customerErrorMessage.isVisible = true
                    binding.customerErrorMessage.text = result.message ?: getString(R.string.error_loading_customer)
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })

        // Наблюдение за результатом обновления статуса заказа
        viewModel.updateOrderStatusResult.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.statusProgressBar.isVisible = true
                    binding.spinnerStatus.isEnabled = false
                }
                is Resource.Success -> {
                    binding.statusProgressBar.isVisible = false
                    binding.spinnerStatus.isEnabled = true
                    Toast.makeText(
                        this,
                        getString(R.string.order_status_updated),
                        Toast.LENGTH_SHORT
                    ).show()

                    // Перезагружаем детали заказа
                    viewModel.loadOrderDetails(orderId)
                }
                is Resource.Error -> {
                    binding.statusProgressBar.isVisible = false
                    binding.spinnerStatus.isEnabled = true
                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_updating_order_status),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        // Наблюдение за результатом отмены заказа
        viewModel.cancelOrderResult.observe(this, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    Toast.makeText(this, R.string.order_cancelled_success, Toast.LENGTH_SHORT).show()
                    // Перезагружаем детали заказа
                    viewModel.loadOrderDetails(orderId)
                }
                is Resource.Error -> {
                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_cancelling_order),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })

        // Наблюдение за результатом экспорта заказа
        viewModel.exportOrderResult.observe(this, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    result.data?.let { file ->
                        shareOrderFile(file)
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_exporting_order),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })
    }

    private fun setupListeners() {
        // Spinner изменения статуса заказа
        binding.spinnerStatus.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentOrder?.let { order ->
                    val newStatus = when (position) {
                        0 -> "pending"
                        1 -> "processing"
                        2 -> "shipping"
                        3 -> "delivered"
                        4 -> "completed"
                        5 -> "cancelled"
                        else -> "pending"
                    }

                    // Обновляем статус только если он изменился
                    if (newStatus != order.status.name) {
                        viewModel.updateOrderStatus(order.id, newStatus)
                    }
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Ничего не делаем
            }
        }

        // Кнопка связи с клиентом
        binding.btnContactCustomer.setOnClickListener {
            showContactOptions()
        }

        // Кнопка отмены заказа
        binding.btnCancelOrder.setOnClickListener {
            showCancelOrderConfirmation()
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

        // Выбираем текущий статус в спиннере
        val statusPosition = when (order.status) {
            OrderStatus.PENDING -> 0
            OrderStatus.PROCESSING -> 1
            OrderStatus.SHIPPING -> 2
            OrderStatus.DELIVERED -> 3
            OrderStatus.COMPLETED -> 4
            OrderStatus.CANCELLED -> 5
        }
        binding.spinnerStatus.setSelection(statusPosition)

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
        orderItemsAdapter.submitList(order.items)

        // Итоговые суммы
        binding.tvSubtotal.text = CurrencyFormatter.format(order.subtotal)
        binding.tvDelivery.text = if (order.deliveryFee > 0) {
            CurrencyFormatter.format(order.deliveryFee)
        } else {
            getString(R.string.free)
        }
        binding.tvTotal.text = CurrencyFormatter.format(order.total)

        // Отображение/скрытие кнопки отмены заказа
        val canCancel = order.status == OrderStatus.PENDING || order.status == OrderStatus.PROCESSING
        binding.btnCancelOrder.isVisible = canCancel
    }

    private fun updateCustomerUI(user: User) {
        binding.tvCustomerName.text = user.name
        binding.tvCustomerEmail.text = user.email
        binding.tvCustomerPhone.text = user.phone

        // Показываем кнопку связи с клиентом
        binding.btnContactCustomer.isVisible = true
    }

    private fun showContactOptions() {
        customer?.let { user ->
            val contactOptions = arrayOf(
                getString(R.string.contact_by_phone),
                getString(R.string.contact_by_email),
                getString(R.string.contact_by_sms)
            )

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.contact_customer))
                .setItems(contactOptions) { _, which ->
                    when (which) {
                        0 -> { // Звонок
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${user.phone}")
                            }
                            startActivity(intent)
                        }
                        1 -> { // Email
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${user.email}")
                                putExtra(Intent.EXTRA_SUBJECT,
                                    getString(R.string.order_email_subject, currentOrder?.number ?: ""))
                            }
                            startActivity(intent)
                        }
                        2 -> { // SMS
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:${user.phone}")
                                putExtra("sms_body",
                                    getString(R.string.order_sms_body, currentOrder?.number ?: ""))
                            }
                            startActivity(intent)
                        }
                    }
                }
                .show()
        } ?: run {
            Toast.makeText(this, R.string.customer_info_not_available, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCancelOrderConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.cancel_order_title))
            .setMessage(getString(R.string.cancel_order_admin_message))
            .setPositiveButton(R.string.cancel_order) { _, _ ->
                currentOrder?.let { order ->
                    viewModel.cancelOrder(order.id)
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.scrollView.isVisible = !isLoading && !binding.errorLayout.isVisible
    }

    private fun showError(isError: Boolean, message: String? = null) {
        binding.errorLayout.isVisible = isError
        binding.errorMessage.text = message ?: getString(R.string.error_loading_order_details)
        binding.scrollView.isVisible = !isError && !binding.progressBar.isVisible
    }

    private fun shareOrderFile(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_order)))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.admin_order_detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_print -> {
                printOrder()
                true
            }
            R.id.action_export -> {
                exportOrder()
                true
            }
            R.id.action_share -> {
                currentOrder?.let { order ->
                    viewModel.generateOrderPdf(this, order)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun printOrder() {
        // В реальном приложении здесь была бы логика для печати заказа
        Toast.makeText(this, R.string.printing_not_implemented, Toast.LENGTH_SHORT).show()
    }

    private fun exportOrder() {
        val exportOptions = arrayOf(
            getString(R.string.export_pdf),
            getString(R.string.export_csv)
        )

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.export_order))
            .setItems(exportOptions) { _, which ->
                currentOrder?.let { order ->
                    when (which) {
                        0 -> viewModel.generateOrderPdf(this, order)
                        1 -> viewModel.exportOrderToCsv(this, order)
                    }
                }
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}