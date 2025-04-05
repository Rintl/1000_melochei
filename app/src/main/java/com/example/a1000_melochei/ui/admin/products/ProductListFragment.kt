package com.yourstore.app.ui.admin.products

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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Product
import com.yourstore.app.databinding.FragmentProductListBinding
import com.yourstore.app.ui.admin.AdminActivity
import com.yourstore.app.ui.admin.products.adapter.AdminProductAdapter
import com.yourstore.app.ui.admin.products.viewmodel.AdminProductViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Фрагмент для отображения списка товаров в административной панели.
 * Позволяет просматривать, фильтровать, сортировать, добавлять, редактировать и удалять товары.
 */
class ProductListFragment : Fragment(), AdminActivity.Refreshable {

    private var _binding: FragmentProductListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminProductViewModel by viewModel()
    private lateinit var productAdapter: AdminProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilterOptions()
        setupSortSpinner()
        setupObservers()
        setupListeners()

        // Загружаем список товаров
        viewModel.loadProducts()
    }

    private fun setupRecyclerView() {
        productAdapter = AdminProductAdapter(
            onItemClick = { product ->
                navigateToEditProduct(product)
            },
            onDeleteClick = { product ->
                showDeleteConfirmation(product)
            },
            onStatusToggleClick = { product, newStatus ->
                viewModel.toggleProductStatus(product.id, newStatus)
            }
        )

        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
        }
    }

    private fun setupFilterOptions() {
        // Настройка фильтрации по наличию
        binding.chipInStock.setOnCheckedChangeListener { _, isChecked ->
            viewModel.filterProductsByAvailability(isChecked)
        }

        // Настройка фильтрации по акциям
        binding.chipOnSale.setOnCheckedChangeListener { _, isChecked ->
            viewModel.filterProductsByPromotion(isChecked)
        }

        // Настройка фильтрации по низкому остатку
        binding.chipLowStock.setOnCheckedChangeListener { _, isChecked ->
            viewModel.filterProductsByLowStock(isChecked)
        }
    }

    private fun setupSortSpinner() {
        val sortOptions = arrayOf(
            getString(R.string.sort_by_name_asc),
            getString(R.string.sort_by_name_desc),
            getString(R.string.sort_by_price_asc),
            getString(R.string.sort_by_price_desc),
            getString(R.string.sort_by_stock_asc),
            getString(R.string.sort_by_stock_desc),
            getString(R.string.sort_by_date_added_newest),
            getString(R.string.sort_by_date_added_oldest)
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
                    0 -> viewModel.sortProductsByName(true)
                    1 -> viewModel.sortProductsByName(false)
                    2 -> viewModel.sortProductsByPrice(true)
                    3 -> viewModel.sortProductsByPrice(false)
                    4 -> viewModel.sortProductsByStock(true)
                    5 -> viewModel.sortProductsByStock(false)
                    6 -> viewModel.sortProductsByDateAdded(false)
                    7 -> viewModel.sortProductsByDateAdded(true)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Ничего не делаем
            }
        }
    }

    private fun setupObservers() {
        // Наблюдение за загрузкой категорий
        viewModel.categories.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    result.data?.let { categories ->
                        // Заполняем выпадающий список категорий
                        val categoryNames = categories.map { it.name }
                        val categoryAdapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            categoryNames
                        ).apply {
                            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        }

                        binding.spinnerCategory.adapter = categoryAdapter
                        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                val selectedCategory = if (position > 0) categories[position - 1] else null
                                viewModel.filterProductsByCategory(selectedCategory?.id)
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {
                                viewModel.filterProductsByCategory(null)
                            }
                        }

                        // Добавляем опцию "Все категории" в начало списка
                        categoryAdapter.insert(getString(R.string.all_categories), 0)
                    }
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })

        // Наблюдение за списком товаров
        viewModel.products.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                    showError(false)
                }
                is Resource.Success -> {
                    showLoading(false)
                    showError(false)

                    result.data?.let { products ->
                        if (products.isEmpty()) {
                            showEmptyState(true)
                        } else {
                            showEmptyState(false)
                            productAdapter.submitList(products)

                            // Обновляем счетчик товаров
                            binding.tvProductCount.text = getString(R.string.products_count, products.size)
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

        // Наблюдение за результатом обновления статуса товара
        viewModel.updateProductStatusResult.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        result.message ?: getString(R.string.error_updating_product_status),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })

        // Наблюдение за результатом удаления товара
        viewModel.deleteProductResult.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.product_deleted),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    // Обновляем список товаров
                    viewModel.loadProducts()
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        result.message ?: getString(R.string.error_deleting_product),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })
    }

    private fun setupListeners() {
        // Кнопка добавления нового товара
        binding.fabAddProduct.setOnClickListener {
            navigateToAddProduct()
        }

        // Кнопка импорта товаров
        binding.btnImportProducts.setOnClickListener {
            navigateToImportProducts()
        }

        // Обновление через свайп вниз
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadProducts()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Кнопка повторной попытки загрузки при ошибке
        binding.btnRetry.setOnClickListener {
            viewModel.loadProducts()
        }

        // Поиск в локальном списке
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.searchProducts(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchProducts(newText ?: "")
                return true
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.rvProducts.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
        binding.filterSortLayout.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showError(isError: Boolean, message: String? = null) {
        binding.errorLayout.isVisible = isError
        binding.errorMessage.text = message ?: getString(R.string.error_loading_products)
        binding.rvProducts.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible
        binding.filterSortLayout.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyLayout.isVisible = isEmpty
        binding.emptyMessage.text = if (binding.searchView.query.isNotEmpty()) {
            getString(R.string.no_products_found)
        } else {
            getString(R.string.no_products_yet)
        }
        binding.rvProducts.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible
        binding.filterSortLayout.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible
    }

    private fun showDeleteConfirmation(product: Product) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_product_title))
            .setMessage(getString(R.string.delete_product_message, product.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteProduct(product.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun navigateToAddProduct() {
        startActivity(Intent(requireContext(), AddProductActivity::class.java))
    }

    private fun navigateToEditProduct(product: Product) {
        val intent = Intent(requireContext(), EditProductActivity::class.java).apply {
            putExtra("PRODUCT_ID", product.id)
        }
        startActivity(intent)
    }

    private fun navigateToImportProducts() {
        startActivity(Intent(requireContext(), ImportProductsActivity::class.java))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.admin_product_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                toggleFilterVisibility()
                true
            }
            R.id.action_export -> {
                showExportDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleFilterVisibility() {
        binding.filterContainer.isVisible = !binding.filterContainer.isVisible
    }

    private fun showExportDialog() {
        val options = arrayOf(
            getString(R.string.export_csv),
            getString(R.string.export_excel)
        )

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.export_products)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.exportProductsToCSV(requireContext())
                    1 -> viewModel.exportProductsToExcel(requireContext())
                }
            }
            .show()
    }

    override fun refresh() {
        viewModel.loadProducts()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем данные при возвращении на экран
        viewModel.loadProducts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}