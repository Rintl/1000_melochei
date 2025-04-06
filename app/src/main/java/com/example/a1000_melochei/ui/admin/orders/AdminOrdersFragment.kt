package com.yourstore.app.ui.admin.orders

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Order
import com.yourstore.app.databinding.FragmentAdminOrdersBinding
import com.yourstore.app.ui.admin.AdminActivity
import com.yourstore.app.ui.admin.orders.adapter.AdminOrderAdapter
import com.yourstore.app.ui.admin.orders.viewmodel.AdminOrderViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar
import java.util.Date

/**
 * Фрагмент для управления заказами в административной панели.
 * Отображает список заказов с возможностью фильтрации, сортировки и поиска.
 * Позволяет просматривать детали заказа и изменять его статус.
 */
class AdminOrdersFragment : Fragment(), AdminActivity.Refreshable {

    private var _binding: FragmentAdminOrdersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminOrderViewModel by viewModel()
    private lateinit var orderAdapter: AdminOrderAdapter

    // Фильтры для заказов
    private var currentStatusFilter: String? = null
    private var currentDateFilter: String? = null
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupStatusFilter()
        setupDateFilter()
        setupSortOptions()
        setupObservers()
        setupListeners()

        // Загружаем заказы
        viewModel.loadOrders()
    }

    private fun setupRecyclerView() {
        orderAdapter = AdminOrderAdapter(
            onOrderClick = { order ->
                navigateToOrderDetails(order)
            },
            onStatusUpdateClick = { order ->
                showStatusUpdateDialog(order)
            }
        )

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter
        }
    }

    private fun setupStatusFilter() {
        val statusOptions = arrayOf(
            getString(R.string.all_orders),
            getString(R.string.order_status_pending),
            getString(R.string.order_status_processing),
            getString(R.string.order_status_shipping),
            getString(R.string.order_status_delivered),
            getString(R.string.order_status_completed),
            getString(R.string.order_status_cancelled)
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            statusOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerStatus.adapter = adapter
        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentStatusFilter = when (position) {
                    0 -> null // Все заказы
                    1 -> "pending"
                    2 -> "processing"
                    3 -> "shipping"
                    4 -> "delivered"
                    5 -> "completed"
                    6 -> "cancelled"
                    else -> null
                }
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentStatusFilter = null
                applyFilters()
            }
        }
    }

    private fun setupDateFilter() {
        val dateOptions = arrayOf(
            getString(R.string.all_time),
            getString(R.string.today),
            getString(R.string.yesterday),
            getString(R.string.this_week),
            getString(R.string.last_week),
            getString(R.string.this_month),
            getString(R.string.last_month)
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            dateOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerDate.adapter = adapter
        binding.spinnerDate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentDateFilter = when (position) {
                    0 -> null // Все время
                    1 -> "today"
                    2 -> "yesterday"
                    3 -> "this_week"
                    4 -> "last_week"
                    5 -> "this_month"
                    6 -> "last_month"
                    else -> null
                }
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentDateFilter = null
                applyFilters()
            }
        }
    }

    private fun setupSortOptions() {
        val sortOptions = arrayOf(
            getString(R.string.sort_by_date_newest),
            getString(R.string.sort_by_date_oldest),
            getString(R.string.sort_by_price_highest),
            getString(R.string.sort_by_price_lowest)
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sortOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerSort.adapter = adapter
        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> viewModel.sortOrders("date_desc")
                    1 -> viewModel.sortOrders("date_asc")
                    2 -> viewModel.sortOrders("price_desc")
                    3 -> viewModel.sortOrders("price_asc")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // По умолчанию сортируем по дате (новые сначала)
                viewModel.sortOrders("date_desc")
            }
        }
    }

    private fun setupObservers() {
        // Наблюдение за списком заказов
        viewModel.orders.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                    showError(false)
                }
                is Resource.Success -> {
                    showLoading(false)
                    showError(false)

                    result.data?.let { orders ->
                        if (orders.isEmpty()) {
                            showEmptyState(true)
                        } else {
                            showEmptyState(false)
                            orderAdapter.submitList(orders)

                            // Обновляем счетчик заказов
                            binding.tvOrderCount.text = getString(R.string.orders_count, orders.size)

                            // Обновляем бейдж с количеством новых заказов
                            val newOrdersCount = orders.count { it.status.name == "pending" }
                            updateNewOrdersBadge(newOrdersCount)
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

        // Наблюдение за результатом обновления статуса заказа
        viewModel.updateOrderStatusResult.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.order_status_updated),
                        Toast.LENGTH_SHORT
                    ).show()

                    // Обновляем список заказов
                    viewModel.loadOrders()
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        result.message ?: getString(R.string.error_updating_order_status),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })
    }

    private fun setupListeners() {
        // Обновление через свайп вниз
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadOrders()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Кнопка повторной попытки загрузки при ошибке
        binding.btnRetry.setOnClickListener {
            viewModel.loadOrders()
        }

        // Поиск
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query ?: ""
                applyFilters()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText ?: ""
                applyFilters()
                return true
            }
        })

        // Кнопка для показа/скрытия фильтров
        binding.btnToggleFilters.setOnClickListener {
            toggleFiltersVisibility()
        }
    }

    private fun applyFilters() {
        viewModel.filterOrders(currentStatusFilter, currentDateFilter, searchQuery)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.rvOrders.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
        binding.filtersContainer.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showError(isError: Boolean, message: String? = null) {
        binding.errorLayout.isVisible = isError
        binding.errorMessage.text = message ?: getString(R.string.error_loading_orders)
        binding.rvOrders.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible
        binding.filtersContainer.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyLayout.isVisible = isEmpty
        binding.emptyMessage.text = if (currentStatusFilter != null || currentDateFilter != null || searchQuery.isNotEmpty()) {
            getString(R.string.no_orders_found_for_filters)
        } else {
            getString(R.string.no_orders_yet)
        }
        binding.rvOrders.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible
        binding.filtersContainer.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible
    }

    private fun toggleFiltersVisibility() {
        val isVisible = binding.advancedFiltersContainer.isVisible
        binding.advancedFiltersContainer.isVisible = !isVisible
        binding.btnToggleFilters.setIconResource(
            if (isVisible) R.drawable.ic_filter_expand else R.drawable.ic_filter_collapse
        )
    }

    private fun showStatusUpdateDialog(order: Order) {
        val statusOptions = arrayOf(
            getString(R.string.order_status_pending),
            getString(R.string.order_status_processing),
            getString(R.string.order_status_shipping),
            getString(R.string.order_status_delivered),
            getString(R.string.order_status_completed),
            getString(R.string.order_status_cancelled)
        )

        // Находим индекс текущего статуса
        val currentStatusIndex = when (order.status.name) {
            "pending" -> 0
            "processing" -> 1
            "shipping" -> 2
            "delivered" -> 3
            "completed" -> 4
            "cancelled" -> 5
            else -> 0
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.update_order_status))
            .setSingleChoiceItems(statusOptions, currentStatusIndex) { dialog, which ->
                val newStatus = when (which) {
                    0 -> "pending"
                    1 -> "processing"
                    2 -> "shipping"
                    3 -> "delivered"
                    4 -> "completed"
                    5 -> "cancelled"
                    else -> "pending"
                }
                dialog.dismiss()
                viewModel.updateOrderStatus(order.id, newStatus)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun navigateToOrderDetails(order: Order) {
        val intent = Intent(requireContext(), OrderDetailAdminActivity::class.java).apply {
            putExtra("ORDER_ID", order.id)
        }
        startActivity(intent)
    }

    private fun updateNewOrdersBadge(count: Int) {
        // Обновляем бейдж в соответствующем меню, если оно доступно
        (activity as? AdminActivity)?.updateOrdersBadge(count)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.admin_order_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                toggleFiltersVisibility()
                true
            }
            R.id.action_export -> {
                exportOrders()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportOrders() {
        val exportOptions = arrayOf(
            getString(R.string.export_csv),
            getString(R.string.export_excel)
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.export_orders))
            .setItems(exportOptions) { _, which ->
                when (which) {
                    0 -> viewModel.exportOrdersToCSV(requireContext())
                    1 -> viewModel.exportOrdersToExcel(requireContext())
                }
            }
            .show()
    }

    override fun refresh() {
        viewModel.loadOrders()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем заказы при возвращении на экран
        viewModel.loadOrders()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}