package com.yourstore.app.ui.customer.orders

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Order
import com.yourstore.app.databinding.FragmentOrdersBinding
import com.yourstore.app.ui.customer.orders.adapter.OrderAdapter
import com.yourstore.app.ui.customer.orders.viewmodel.OrderViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Фрагмент для отображения списка заказов пользователя.
 * Позволяет просматривать историю заказов и их статусы.
 */
class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderViewModel by viewModel()
    private lateinit var orderAdapter: OrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabLayout()
        setupObservers()
        setupListeners()

        // Загружаем заказы
        viewModel.loadOrders()
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter { order ->
            navigateToOrderDetail(order)
        }

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter
        }
    }

    private fun setupTabLayout() {
        // Настраиваем табы для фильтрации заказов по статусу
        binding.tabLayout.apply {
            addTab(newTab().setText(R.string.all_orders))
            addTab(newTab().setText(R.string.active_orders))
            addTab(newTab().setText(R.string.completed_orders))
            addTab(newTab().setText(R.string.cancelled_orders))

            // Обработчик переключения табов
            addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                    when (tab?.position) {
                        0 -> viewModel.filterOrders(null) // Все заказы
                        1 -> viewModel.filterOrders(listOf("pending", "processing", "shipping")) // Активные
                        2 -> viewModel.filterOrders(listOf("completed", "delivered")) // Завершенные
                        3 -> viewModel.filterOrders(listOf("cancelled")) // Отмененные
                    }
                }

                override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                    // Ничего не делаем
                }

                override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                    // Ничего не делаем
                }
            })
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

        // Наблюдение за результатом отмены заказа
        viewModel.cancelOrderResult.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    Toast.makeText(requireContext(), R.string.order_cancelled_success, Toast.LENGTH_SHORT).show()
                    // Перезагружаем заказы
                    viewModel.loadOrders()
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        result.message ?: getString(R.string.error_cancelling_order),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })
    }

    private fun setupListeners() {
        // Обработка обновления через свайп вниз
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadOrders()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Обработка повторной попытки загрузки при ошибке
        binding.btnRetry.setOnClickListener {
            viewModel.loadOrders()
        }

        // Кнопка создания нового заказа при пустом списке
        binding.btnGoShopping.setOnClickListener {
            navigateToCatalog()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.rvOrders.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
        binding.tabLayout.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showError(isError: Boolean, message: String? = null) {
        binding.errorLayout.isVisible = isError
        binding.errorMessage.text = message ?: getString(R.string.error_loading_orders)
        binding.rvOrders.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible
        binding.tabLayout.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyLayout.isVisible = isEmpty
        binding.rvOrders.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible
        binding.tabLayout.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible

        // Меняем текст в зависимости от выбранного таба
        if (isEmpty) {
            val selectedTabPosition = binding.tabLayout.selectedTabPosition
            binding.emptyMessage.text = when (selectedTabPosition) {
                0 -> getString(R.string.no_orders)
                1 -> getString(R.string.no_active_orders)
                2 -> getString(R.string.no_completed_orders)
                3 -> getString(R.string.no_cancelled_orders)
                else -> getString(R.string.no_orders)
            }
        }
    }

    private fun navigateToOrderDetail(order: Order) {
        val intent = Intent(requireContext(), OrderDetailActivity::class.java).apply {
            putExtra("ORDER_ID", order.id)
        }
        startActivity(intent)
    }

    private fun navigateToCatalog() {
        // Переключаемся на таб каталога
        requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            R.id.bottom_nav_view
        )?.selectedItemId = R.id.navigation_catalog
    }

    override fun onResume() {
        super.onResume()
        // Обновляем данные при возвращении на экран
        viewModel.loadOrders()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}