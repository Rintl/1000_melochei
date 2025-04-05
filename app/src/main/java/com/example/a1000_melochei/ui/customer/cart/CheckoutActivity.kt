package com.yourstore.app.ui.customer.cart

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Address
import com.yourstore.app.databinding.ActivityCheckoutBinding
import com.yourstore.app.ui.customer.CartItemSummaryAdapter
import com.yourstore.app.ui.customer.CustomerActivity
import com.yourstore.app.ui.customer.cart.viewmodel.CartViewModel
import com.yourstore.app.ui.customer.profile.AddressActivity
import com.yourstore.app.util.CurrencyFormatter
import com.yourstore.app.util.DateUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar
import java.util.Date

/**
 * Активность оформления заказа.
 * Позволяет пользователю выбрать адрес доставки, способ получения,
 * дату и время доставки, а также метод оплаты.
 */
class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private val viewModel: CartViewModel by viewModel()

    private lateinit var cartItemSummaryAdapter: CartItemSummaryAdapter
    private var selectedAddressId: String? = null
    private var selectedDeliveryMethod: String = ""
    private var selectedDeliveryDate: Date? = null
    private var selectedPaymentMethod: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupAddressSelection()
        setupDeliveryMethodSelection()
        setupDeliveryDateSelection()
        setupPaymentMethodSelection()
        setupObservers()
        setupListeners()

        // Загружаем данные для оформления заказа
        viewModel.loadCartForCheckout()
        viewModel.loadUserAddresses()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.checkout)
    }

    private fun setupRecyclerView() {
        cartItemSummaryAdapter = CartItemSummaryAdapter()

        binding.rvOrderItems.apply {
            layoutManager = LinearLayoutManager(this@CheckoutActivity)
            adapter = cartItemSummaryAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupAddressSelection() {
        binding.rgDeliveryMethod.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_delivery -> {
                    selectedDeliveryMethod = "delivery"
                    binding.addressSelectionLayout.visibility = View.VISIBLE
                    binding.pickupPointLayout.visibility = View.GONE
                    recalculateTotal()
                }
                R.id.rb_pickup -> {
                    selectedDeliveryMethod = "pickup"
                    binding.addressSelectionLayout.visibility = View.GONE
                    binding.pickupPointLayout.visibility = View.VISIBLE
                    recalculateTotal()
                }
            }
        }

        // Настройка кнопки добавления нового адреса
        binding.btnAddAddress.setOnClickListener {
            val intent = Intent(this, AddressActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupDeliveryMethodSelection() {
        // По умолчанию выбираем доставку
        binding.rbDelivery.isChecked = true
        selectedDeliveryMethod = "delivery"

        // Настраиваем выбор пункта самовывоза
        val pickupPoints = arrayOf(
            "Магазин 1000 мелочей (ул. Центральная, 10)",
            "Склад 1000 мелочей (ул. Промышленная, 5)"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, pickupPoints)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPickupPoint.adapter = adapter
    }

    private fun setupDeliveryDateSelection() {
        // Установка текущей даты + 1 день как минимальной даты доставки
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        selectedDeliveryDate = calendar.time

        // Отображаем выбранную дату и время
        updateDeliveryDateTimeUI()

        // Настройка выбора даты
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        // Настройка выбора времени
        binding.btnSelectTime.setOnClickListener {
            showTimePicker()
        }
    }

    private fun setupPaymentMethodSelection() {
        binding.rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_cash -> selectedPaymentMethod = "cash"
                R.id.rb_card -> selectedPaymentMethod = "card"
                R.id.rb_kaspi -> selectedPaymentMethod = "kaspi"
            }

            // Показываем QR-код только для Kaspi
            binding.kaspiQrLayout.isVisible = selectedPaymentMethod == "kaspi"
        }

        // По умолчанию выбираем наличными
        binding.rbCash.isChecked = true
        selectedPaymentMethod = "cash"
    }

    private fun setupObservers() {
        // Наблюдение за содержимым корзины
        viewModel.cartItems.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                    showError(false)
                }
                is Resource.Success -> {
                    showLoading(false)
                    showError(false)

                    result.data?.let { cartItems ->
                        if (cartItems.isEmpty()) {
                            showEmptyState(true)
                        } else {
                            showEmptyState(false)
                            cartItemSummaryAdapter.submitList(cartItems)
                        }
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    showEmptyState(false)
                    showError(true, result.message)
                }
            }
        })

        // Наблюдение за общей суммой корзины
        viewModel.cartTotal.observe(this, Observer { cartTotal ->
            binding.tvSubtotal.text = CurrencyFormatter.format(cartTotal.subtotal)
            binding.tvDelivery.text = if (cartTotal.deliveryFee > 0) {
                CurrencyFormatter.format(cartTotal.deliveryFee)
            } else {
                getString(R.string.free)
            }
            binding.tvTotal.text = CurrencyFormatter.format(cartTotal.total)
        })

        // Наблюдение за списком адресов пользователя
        viewModel.userAddresses.observe(this, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    result.data?.let { addresses ->
                        setupAddressSpinner(addresses)
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_loading_addresses),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })

        // Наблюдение за результатом оформления заказа
        viewModel.placeOrderResult.observe(this, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.btnPlaceOrder.isEnabled = false
                    binding.placeOrderProgressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.btnPlaceOrder.isEnabled = true
                    binding.placeOrderProgressBar.visibility = View.GONE

                    // Показываем сообщение об успешном оформлении заказа
                    showOrderSuccessDialog(result.data ?: "")
                }
                is Resource.Error -> {
                    binding.btnPlaceOrder.isEnabled = true
                    binding.placeOrderProgressBar.visibility = View.GONE

                    Toast.makeText(
                        this,
                        result.message ?: getString(R.string.error_placing_order),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }

    private fun setupListeners() {
        // Кнопка оформления заказа
        binding.btnPlaceOrder.setOnClickListener {
            if (validateOrder()) {
                placeOrder()
            }
        }
    }

    private fun setupAddressSpinner(addresses: List<Address>) {
        // Если адресов нет, показываем сообщение
        if (addresses.isEmpty()) {
            binding.tvNoAddresses.visibility = View.VISIBLE
            binding.spinnerAddress.visibility = View.GONE
            return
        }

        binding.tvNoAddresses.visibility = View.GONE
        binding.spinnerAddress.visibility = View.VISIBLE

        // Создаем адаптер для выпадающего списка
        val addressItems = addresses.map { it.title + ": " + it.address }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, addressItems)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAddress.adapter = adapter

        // Обработчик выбора адреса
        binding.spinnerAddress.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAddressId = addresses[position].id
                recalculateTotal()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedAddressId = null
            }
        }

        // Выбираем адрес по умолчанию, если есть
        val defaultAddressIndex = addresses.indexOfFirst { it.isDefault }
        if (defaultAddressIndex >= 0) {
            binding.spinnerAddress.setSelection(defaultAddressIndex)
            selectedAddressId = addresses[defaultAddressIndex].id
        } else {
            selectedAddressId = addresses.firstOrNull()?.id
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDeliveryDate?.let {
            calendar.time = it
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                selectedDeliveryDate = calendar.time
                updateDeliveryDateTimeUI()
            },
            year,
            month,
            day
        )

        // Установка минимальной даты (завтрашний день)
        val minDate = Calendar.getInstance()
        minDate.add(Calendar.DAY_OF_MONTH, 1)
        datePickerDialog.datePicker.minDate = minDate.timeInMillis

        // Установка максимальной даты (14 дней вперед)
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.DAY_OF_MONTH, 14)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        selectedDeliveryDate?.let {
            calendar.time = it
        }

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
                selectedDeliveryDate = calendar.time
                updateDeliveryDateTimeUI()
            },
            hour,
            minute,
            true
        )

        timePickerDialog.show()
    }

    private fun updateDeliveryDateTimeUI() {
        selectedDeliveryDate?.let { date ->
            binding.tvSelectedDate.text = DateUtils.formatDate(date)
            binding.tvSelectedTime.text = DateUtils.formatTime(date)
        }
    }

    private fun recalculateTotal() {
        // Расчет стоимости доставки в зависимости от выбранного адреса и метода доставки
        viewModel.calculateDeliveryFee(selectedAddressId, selectedDeliveryMethod)
    }

    private fun validateOrder(): Boolean {
        // Проверка выбора адреса для доставки
        if (selectedDeliveryMethod == "delivery" && selectedAddressId == null) {
            Toast.makeText(this, R.string.error_select_address, Toast.LENGTH_SHORT).show()
            return false
        }

        // Проверка выбора даты и времени доставки
        if (selectedDeliveryDate == null) {
            Toast.makeText(this, R.string.error_select_delivery_date, Toast.LENGTH_SHORT).show()
            return false
        }

        // Проверка выбора метода оплаты
        if (selectedPaymentMethod.isEmpty()) {
            Toast.makeText(this, R.string.error_select_payment_method, Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun placeOrder() {
        viewModel.placeOrder(
            addressId = if (selectedDeliveryMethod == "delivery") selectedAddressId else null,
            deliveryMethod = selectedDeliveryMethod,
            pickupPoint = if (selectedDeliveryMethod == "pickup")
                binding.spinnerPickupPoint.selectedItemPosition else null,
            deliveryDate = selectedDeliveryDate,
            paymentMethod = selectedPaymentMethod,
            comment = binding.etOrderComment.text.toString()
        )
    }

    private fun showOrderSuccessDialog(orderId: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.order_success_title)
            .setMessage(getString(R.string.order_success_message, orderId))
            .setPositiveButton(R.string.btn_view_orders) { _, _ ->
                navigateToOrders()
            }
            .setNegativeButton(R.string.btn_continue_shopping) { _, _ ->
                navigateToHome()
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateToOrders() {
        val intent = Intent(this, CustomerActivity::class.java).apply {
            putExtra("NAVIGATE_TO_ORDERS", true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToHome() {
        val intent = Intent(this, CustomerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.scrollView.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showError(isError: Boolean, message: String? = null) {
        binding.errorLayout.isVisible = isError
        binding.errorMessage.text = message ?: getString(R.string.error_loading_cart)
        binding.scrollView.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible

        binding.btnRetry.setOnClickListener {
            viewModel.loadCartForCheckout()
        }
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyLayout.isVisible = isEmpty
        binding.scrollView.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible

        binding.btnGoShopping.setOnClickListener {
            navigateToHome()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}