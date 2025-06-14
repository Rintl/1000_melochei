package com.example.a1000_melochei.ui.customer.catalog

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Category
import com.example.a1000_melochei.data.model.Product
import com.example.a1000_melochei.databinding.ActivitySearchBinding
import com.example.a1000_melochei.ui.customer.catalog.adapter.ProductAdapter
import com.example.a1000_melochei.ui.customer.catalog.viewmodel.CatalogViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


/**
 * Активность для поиска товаров.
 * Позволяет искать товары по названию, фильтровать по категориям,
 * сортировать по различным параметрам.
 */
class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: CatalogViewModel by viewModel()

    private lateinit var productAdapter: ProductAdapter
    private var searchQuery: String = ""
    private var selectedCategoryId: String? = null
    private var minPrice: Double? = null
    private var maxPrice: Double? = null
    private var sortType: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получаем параметры из Intent, если они есть
        searchQuery = intent.getStringExtra("SEARCH_QUERY") ?: ""
        selectedCategoryId = intent.getStringExtra("CATEGORY_ID")

        setupToolbar()
        setupRecyclerView()
        setupSearchView()
        setupSortSpinner()
        setupCategoryChips()
        setupPriceFilter()
        setupObservers()

        // Если есть запрос или категория, выполняем поиск
        if (searchQuery.isNotEmpty() || selectedCategoryId != null) {
            performSearch()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.search)
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            navigateToProductDetail(product)
        }

        binding.rvSearchResults.apply {
            layoutManager = GridLayoutManager(this@SearchActivity, 2)
            adapter = productAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query ?: ""
                performSearch()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Можно реализовать поиск по мере ввода текста
                // для автоматического обновления результатов
                return false
            }
        })

        // Устанавливаем запрос, если он был передан
        if (searchQuery.isNotEmpty()) {
            binding.searchView.setQuery(searchQuery, false)
        }
    }

    private fun setupSortSpinner() {
        val sortOptions = arrayOf(
            getString(R.string.sort_by_relevance),
            getString(R.string.sort_by_price_asc),
            getString(R.string.sort_by_price_desc),
            getString(R.string.sort_by_name_asc),
            getString(R.string.sort_by_name_desc)
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            sortOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerSort.adapter = adapter
        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (sortType != position) {
                    sortType = position
                    if (binding.rvSearchResults.isVisible) {
                        applySorting()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Ничего не делаем
            }
        }
    }

    private fun setupCategoryChips() {
        // Загружаем и отображаем все доступные категории как chips
        viewModel.loadCategories()
        viewModel.categories.observe(this, Observer { result ->
            if (result is Resource.Success) {
                result.data?.let { categories ->
                    updateCategoryChips(categories)
                }
            }
        })
    }

    private fun updateCategoryChips(categories: List<Category>) {
        binding.chipGroupCategories.removeAllViews()

        // Добавляем чип "Все категории"
        val allCategoriesChip = layoutInflater.inflate(
            R.layout.item_chip_choice, binding.chipGroupCategories, false
        ) as Chip
        allCategoriesChip.text = getString(R.string.all_categories)
        allCategoriesChip.id = View.generateViewId()
        allCategoriesChip.isChecked = selectedCategoryId == null
        binding.chipGroupCategories.addView(allCategoriesChip)

        // Добавляем чипы для всех категорий
        categories.forEach { category ->
            val chip = layoutInflater.inflate(
                R.layout.item_chip_choice, binding.chipGroupCategories, false
            ) as Chip
            chip.text = category.name
            chip.id = View.generateViewId()
            chip.tag = category.id
            chip.isChecked = category.id == selectedCategoryId
            binding.chipGroupCategories.addView(chip)
        }

        // Обработчик выбора категории
        binding.chipGroupCategories.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == View.NO_ID) {
                // Ничего не выбрано, выбираем "Все категории"
                allCategoriesChip.isChecked = true
                selectedCategoryId = null
            } else {
                val selectedChip = group.findViewById<Chip>(checkedId)
                selectedCategoryId = if (selectedChip == allCategoriesChip) null else selectedChip.tag as String
            }
            // Перезапускаем поиск с новым фильтром категории
            performSearch()
        }
    }

    private fun setupPriceFilter() {
        // Настройка фильтрации по цене
        binding.btnApplyPrice.setOnClickListener {
            try {
                val min = binding.etMinPrice.text.toString()
                val max = binding.etMaxPrice.text.toString()

                minPrice = if (min.isNotEmpty()) min.toDouble() else null
                maxPrice = if (max.isNotEmpty()) max.toDouble() else null

                performSearch()
            } catch (e: NumberFormatException) {
                binding.etMinPrice.error = getString(R.string.invalid_price)
                binding.etMaxPrice.error = getString(R.string.invalid_price)
            }
        }

        // Сброс фильтра по цене
        binding.btnResetPrice.setOnClickListener {
            binding.etMinPrice.setText("")
            binding.etMaxPrice.setText("")
            minPrice = null
            maxPrice = null
            performSearch()
        }

        // Настройка чипов фильтрации
        binding.chipInStock.setOnCheckedChangeListener { _, _ ->
            performSearch()
        }

        binding.chipOnSale.setOnCheckedChangeListener { _, _ ->
            performSearch()
        }
    }

    private fun setupObservers() {
        // Наблюдение за результатами поиска
        viewModel.searchResults.observe(this, Observer { result ->
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

                            // Обновляем счетчик результатов
                            binding.tvResultCount.text = resources.getQuantityString(
                                R.plurals.search_results_count,
                                products.size,
                                products.size
                            )
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
    }

    private fun performSearch() {
        val inStockOnly = binding.chipInStock.isChecked
        val onSaleOnly = binding.chipOnSale.isChecked

        viewModel.searchProducts(
            query = searchQuery,
            categoryId = selectedCategoryId,
            minPrice = minPrice,
            maxPrice = maxPrice,
            inStockOnly = inStockOnly,
            onSaleOnly = onSaleOnly
        )

        applySorting()
    }

    private fun applySorting() {
        when (sortType) {
            0 -> viewModel.sortSearchResultsByRelevance() // По релевантности
            1 -> viewModel.sortSearchResultsByPrice(true) // По цене (возрастание)
            2 -> viewModel.sortSearchResultsByPrice(false) // По цене (убывание)
            3 -> viewModel.sortSearchResultsByName(true) // По названию (A-Z)
            4 -> viewModel.sortSearchResultsByName(false) // По названию (Z-A)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.rvSearchResults.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
        binding.tvResultCount.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showError(isError: Boolean, message: String? = null) {
        binding.errorLayout.isVisible = isError
        binding.errorMessage.text = message ?: getString(R.string.error_search)
        binding.rvSearchResults.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible
        binding.tvResultCount.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyLayout.isVisible = isEmpty

        // Создаем сообщение в зависимости от параметров поиска
        val message = if (searchQuery.isNotEmpty()) {
            getString(R.string.no_search_results_for, searchQuery)
        } else if (selectedCategoryId != null) {
            getString(R.string.no_products_in_category_with_filters)
        } else {
            getString(R.string.no_products_with_filters)
        }

        binding.emptyMessage.text = message

        binding.rvSearchResults.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible
        binding.tvResultCount.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible

        if (isEmpty) {
            binding.tvResultCount.text = getString(R.string.no_results)
        }
    }

    private fun navigateToProductDetail(product: Product) {
        val intent = Intent(this, ProductDetailActivity::class.java).apply {
            putExtra("PRODUCT_ID", product.id)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}