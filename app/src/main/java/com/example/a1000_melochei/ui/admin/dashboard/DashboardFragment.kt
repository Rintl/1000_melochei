package com.example.a1000_melochei.ui.admin.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.databinding.FragmentDashboardBinding
import com.example.a1000_melochei.ui.admin.AdminActivity
import com.example.a1000_melochei.ui.admin.dashboard.viewmodel.DashboardViewModel
import com.example.a1000_melochei.ui.admin.orders.OrderDetailAdminActivity
import com.example.a1000_melochei.ui.admin.products.AddProductActivity
import com.example.a1000_melochei.ui.admin.products.ImportProductsActivity
import com.example.a1000_melochei.util.CurrencyFormatter
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Фрагмент панели управления администратора.
 * Отображает ключевые метрики, последние заказы, популярные товары и предоставляет
 * быстрый доступ к основным функциям.
 */
class DashboardFragment : Fragment(), AdminActivity.Refreshable {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupListeners()

        // Загружаем данные для дашборда
        loadDashboardData()
    }

    private fun setupObservers() {
        // Наблюдение за статистикой заказов
        viewModel.ordersStats.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.ordersStatsProgressBar.isVisible = true
                    binding.ordersStatsLayout.isVisible = false
                }
                is Resource.Success -> {
                    binding.ordersStatsProgressBar.isVisible = false
                    binding.ordersStatsLayout.isVisible = true

                    result.data?.let { stats ->
                        binding.tvTotalOrdersValue.text = stats.totalOrders.toString()
                        binding.tvPendingOrdersValue.text = stats.pendingOrders.toString()
                        binding.tvCompletedOrdersValue.text = stats.completedOrders.toString()
                        binding.tvCancelledOrdersValue.text = stats.cancelledOrders.toString()
                    }
                }
                is Resource.Error -> {
                    binding.ordersStatsProgressBar.isVisible = false
                    binding.ordersStatsLayout.isVisible = true
                    binding.tvOrdersStatsError.isVisible = true
                    binding.tvOrdersStatsError.text = result.message ?: getString(R.string.error_loading_orders_stats)
                }
            }
        })

        // Наблюдение за статистикой продаж
        viewModel.salesStats.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.salesStatsProgressBar.isVisible = true
                    binding.salesStatsLayout.isVisible = false
                }
                is Resource.Success -> {
                    binding.salesStatsProgressBar.isVisible = false
                    binding.salesStatsLayout.isVisible = true

                    result.data?.let { stats ->
                        binding.tvTotalSalesValue.text = CurrencyFormatter.format(stats.totalSales)
                        binding.tvAverageOrderValue.text = CurrencyFormatter.format(stats.averageOrderValue)
                        binding.tvTodaySalesValue.text = CurrencyFormatter.format(stats.todaySales)
                        binding.tvWeekSalesValue.text = CurrencyFormatter.format(stats.weekSales)
                    }
                }
                is Resource.Error -> {
                    binding.salesStatsProgressBar.isVisible = false
                    binding.salesStatsLayout.isVisible = true
                    binding.tvSalesStatsError.isVisible = true
                    binding.tvSalesStatsError.text = result.message ?: getString(R.string.error_loading_sales_stats)
                }
            }
        })

        // Наблюдение за статистикой товаров
        viewModel.productsStats.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.productsStatsProgressBar.isVisible = true
                    binding.productsStatsLayout.isVisible = false
                }
                is Resource.Success -> {
                    binding.productsStatsProgressBar.isVisible = false
                    binding.productsStatsLayout.isVisible = true

                    result.data?.let { stats ->
                        binding.tvTotalProductsValue.text = stats.totalProducts.toString()
                        binding.tvOutOfStockValue.text = stats.outOfStockProducts.toString()
                        binding.tvLowStockValue.text = stats.lowStockProducts.toString()
                        binding.tvProductCategoriesValue.text = stats.categoriesCount.toString()
                    }
                }
                is Resource.Error -> {
                    binding.productsStatsProgressBar.isVisible = false
                    binding.productsStatsLayout.isVisible = true
                    binding.tvProductsStatsError.isVisible = true
                    binding.tvProductsStatsError.text = result.message ?: getString(R.string.error_loading_products_stats)
                }
            }
        })

        // Наблюдение за последними заказами
        viewModel.latestOrders.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.latestOrdersProgressBar.isVisible = true
                    binding.latestOrdersLayout.isVisible = false
                }
                is Resource.Success -> {
                    binding.latestOrdersProgressBar.isVisible = false
                    binding.latestOrdersLayout.isVisible = true

                    result.data?.let { orders ->
                        if (orders.isEmpty()) {
                            binding.tvNoLatestOrders.isVisible = true
                            binding.latestOrdersContent.isVisible = false
                        } else {
                            binding.tvNoLatestOrders.isVisible = false
                            binding.latestOrdersContent.isVisible = true

                            // Отображаем последние 3 заказа
                            if (orders.size >= 1) {
                                val order1 = orders[0]
                                binding.tvOrderNumber1.text = getString(R.string.order_number_value, order1.number)
                                binding.tvOrderDate1.text = formatDate(order1.createdAt)
                                binding.tvOrderStatus1.text = getOrderStatusText(order1.status.name)
                                binding.tvOrderAmount1.text = CurrencyFormatter.format(order1.total)
                                binding.orderCard1.isVisible = true
                                binding.orderCard1.setOnClickListener {
                                    navigateToOrderDetail(order1.id)
                                }
                            } else {
                                binding.orderCard1.isVisible = false
                            }

                            if (orders.size >= 2) {
                                val order2 = orders[1]
                                binding.tvOrderNumber2.text = getString(R.string.order_number_value, order2.number)
                                binding.tvOrderDate2.text = formatDate(order2.createdAt)
                                binding.tvOrderStatus2.text = getOrderStatusText(order2.status.name)
                                binding.tvOrderAmount2.text = CurrencyFormatter.format(order2.total)
                                binding.orderCard2.isVisible = true
                                binding.orderCard2.setOnClickListener {
                                    navigateToOrderDetail(order2.id)
                                }
                            } else {
                                binding.orderCard2.isVisible = false
                            }

                            if (orders.size >= 3) {
                                val order3 = orders[2]
                                binding.tvOrderNumber3.text = getString(R.string.order_number_value, order3.number)
                                binding.tvOrderDate3.text = formatDate(order3.createdAt)
                                binding.tvOrderStatus3.text = getOrderStatusText(order3.status.name)
                                binding.tvOrderAmount3.text = CurrencyFormatter.format(order3.total)
                                binding.orderCard3.isVisible = true
                                binding.orderCard3.setOnClickListener {
                                    navigateToOrderDetail(order3.id)
                                }
                            } else {
                                binding.orderCard3.isVisible = false
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    binding.latestOrdersProgressBar.isVisible = false
                    binding.latestOrdersLayout.isVisible = true
                    binding.tvLatestOrdersError.isVisible = true
                    binding.tvLatestOrdersError.text = result.message ?: getString(R.string.error_loading_latest_orders)
                }
            }
        })

        // Наблюдение за популярными товарами
        viewModel.popularProducts.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.popularProductsProgressBar.isVisible = true
                    binding.popularProductsLayout.isVisible = false
                }
                is Resource.Success -> {
                    binding.popularProductsProgressBar.isVisible = false
                    binding.popularProductsLayout.isVisible = true

                    result.data?.let { products ->
                        if (products.isEmpty()) {
                            binding.tvNoPopularProducts.isVisible = true
                            binding.popularProductsContent.isVisible = false
                        } else {
                            binding.tvNoPopularProducts.isVisible = false
                            binding.popularProductsContent.isVisible = true

                            // Отображаем 3 популярных товара
                            if (products.size >= 1) {
                                val product1 = products[0]
                                binding.tvProductName1.text = product1.name
                                binding.tvProductPrice1.text = CurrencyFormatter.format(product1.price)
                                binding.tvProductSold1.text = getString(R.string.units_sold, product1.soldCount)
                                binding.productCard1.isVisible = true
                                binding.productCard1.setOnClickListener {
                                    navigateToProductEdit(product1.id)
                                }
                            } else {
                                binding.productCard1.isVisible = false
                            }

                            if (products.size >= 2) {
                                val product2 = products[1]
                                binding.tvProductName2.text = product2.name
                                binding.tvProductPrice2.text = CurrencyFormatter.format(product2.price)
                                binding.tvProductSold2.text = getString(R.string.units_sold, product2.soldCount)
                                binding.productCard2.isVisible = true
                                binding.productCard2.setOnClickListener {
                                    navigateToProductEdit(product2.id)
                                }
                            } else {
                                binding.productCard2.isVisible = false
                            }

                            if (products.size >= 3) {
                                val product3 = products[2]
                                binding.tvProductName3.text = product3.name
                                binding.tvProductPrice3.text = CurrencyFormatter.format(product3.price)
                                binding.tvProductSold3.text = getString(R.string.units_sold, product3.soldCount)
                                binding.productCard3.isVisible = true
                                binding.productCard3.setOnClickListener {
                                    navigateToProductEdit(product3.id)
                                }
                            } else {
                                binding.productCard3.isVisible = false
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    binding.popularProductsProgressBar.isVisible = false
                    binding.popularProductsLayout.isVisible = true
                    binding.tvPopularProductsError.isVisible = true
                    binding.tvPopularProductsError.text = result.message ?: getString(R.string.error_loading_popular_products)
                }
            }
        })
    }

    private fun setupListeners() {
        // Кнопки быстрых действий
        binding.btnAddProduct.setOnClickListener {
            navigateToAddProduct()
        }

        binding.btnImportProducts.setOnClickListener {
            navigateToImportProducts()
        }

        binding.btnViewAllProducts.setOnClickListener {
            navigateToProductsList()
        }

        binding.btnViewAllOrders.setOnClickListener {
            navigateToOrdersList()
        }

        binding.btnCategories.setOnClickListener {
            navigateToCategoriesList()
        }

        binding.btnAnalytics.setOnClickListener {
            navigateToAnalytics()
        }

        // Обновление данных через свайп вниз
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadDashboardData()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun loadDashboardData() {
        viewModel.loadDashboardData()
    }

    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }

    private fun getOrderStatusText(status: String): String {
        return when (status.uppercase()) {
            "PENDING" -> getString(R.string.order_status_pending)
            "PROCESSING" -> getString(R.string.order_status_processing)
            "SHIPPING" -> getString(R.string.order_status_shipping)
            "DELIVERED" -> getString(R.string.order_status_delivered)
            "COMPLETED" -> getString(R.string.order_status_completed)
            "CANCELLED" -> getString(R.string.order_status_cancelled)
            else -> status
        }
    }

    private fun navigateToOrderDetail(orderId: String) {
        val intent = Intent(requireContext(), OrderDetailAdminActivity::class.java).apply {
            putExtra("ORDER_ID", orderId)
        }
        startActivity(intent)
    }

    private fun navigateToProductEdit(productId: String) {
        val intent = Intent(requireContext(), com.example.a1000_melochei.ui.admin.products.EditProductActivity::class.java).apply {
            putExtra("PRODUCT_ID", productId)
        }
        startActivity(intent)
    }

    private fun navigateToAddProduct() {
        startActivity(Intent(requireContext(), AddProductActivity::class.java))
    }

    private fun navigateToImportProducts() {
        startActivity(Intent(requireContext(), ImportProductsActivity::class.java))
    }

    private fun navigateToProductsList() {
        findNavController().navigate(R.id.navigation_admin_products)
    }

    private fun navigateToOrdersList() {
        findNavController().navigate(R.id.navigation_admin_orders)
    }

    private fun navigateToCategoriesList() {
        findNavController().navigate(R.id.navigation_admin_categories)
    }

    private fun navigateToAnalytics() {
        findNavController().navigate(R.id.navigation_admin_analytics)
    }

    override fun refresh() {
        loadDashboardData()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем данные при возвращении на экран
        loadDashboardData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}