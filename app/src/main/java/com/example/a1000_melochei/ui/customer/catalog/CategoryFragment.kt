package com.yourstore.app.ui.customer.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Product
import com.yourstore.app.databinding.FragmentCategoryBinding
import com.yourstore.app.ui.customer.catalog.adapter.ProductAdapter
import com.yourstore.app.ui.customer.catalog.viewmodel.CatalogViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Фрагмент для отображения товаров в выбранной категории.
 * Получает ID категории и отображает соответствующие товары.
 */
class CategoryFragment : Fragment() {

    private val args: CategoryFragmentArgs by navArgs()
    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CatalogViewModel by viewModel()
    private lateinit var productAdapter: ProductAdapter

    private var categoryId: String = ""
    private var categoryName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Получаем ID категории из аргументов
        categoryId = args.categoryId
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()
        setupSortSpinner()
        setupFilterOptions()

        // Загружаем информацию о категории и её товары
        viewModel.loadCategoryWithProducts(categoryId)
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            navigateToProductDetail(product)
        }

        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = productAdapter
        }
    }

    private fun setupObservers() {
        // Наблюдение за данными категории
        viewModel.category.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    result.data?.let { category ->
                        categoryName = category.name
                        updateTitle(category.name)
                    }
                }
                else -> {}
            }
        })

        // Наблюдение за товарами категории
        viewModel.categoryProducts.observe(viewLifecycleOwner, Observer { result ->
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
                            binding.tvProductCount.text = resources.getQuantityString(
                                R.plurals.product_count,
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

    private fun setupListeners() {
        // Обработка обновления через свайп вниз
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadCategoryWithProducts(categoryId)
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Обработка повторной попытки загрузки при ошибке
        binding.btnRetry.setOnClickListener {
            viewModel.loadCategoryWithProducts(categoryId)
        }

        // Обработка изменения вида отображения (сетка/список)
        binding.btnViewToggle.setOnClickListener {
            toggleViewMode()
        }
    }

    private fun setupSortSpinner() {
        val sortOptions = arrayOf(
            getString(R.string.sort_by_popularity),
            getString(R.string.sort_by_price_asc),
            getString(R.string.sort_by_price_desc),
            getString(R.string.sort_by_name_asc),
            getString(R.string.sort_by_name_desc)
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
                    0 -> viewModel.sortProductsByPopularity()
                    1 -> viewModel.sortProductsByPrice(true)
                    2 -> viewModel.sortProductsByPrice(false)
                    3 -> viewModel.sortProductsByName(true)
                    4 -> viewModel.sortProductsByName(false)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Ничего не делаем
            }
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
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.rvProducts.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
        binding.sortFilterLayout.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showError(isError: Boolean, message: String? = null) {
        binding.errorLayout.isVisible = isError
        binding.errorMessage.text = message ?: getString(R.string.error_loading_products)
        binding.rvProducts.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible
        binding.sortFilterLayout.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyLayout.isVisible = isEmpty
        binding.emptyMessage.text = getString(R.string.no_products_in_category)
        binding.rvProducts.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible
        binding.sortFilterLayout.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible
    }

    private fun updateTitle(title: String) {
        (requireActivity() as AppCompatActivity).supportActionBar?.title = title
    }

    private fun toggleViewMode() {
        val layoutManager = binding.rvProducts.layoutManager as GridLayoutManager
        if (layoutManager.spanCount == 2) {
            // Переключаемся на список
            layoutManager.spanCount = 1
            binding.btnViewToggle.setImageResource(R.drawable.ic_grid_view)
        } else {
            // Переключаемся на сетку
            layoutManager.spanCount = 2
            binding.btnViewToggle.setImageResource(R.drawable.ic_list_view)
        }
        productAdapter.notifyItemRangeChanged(0, productAdapter.itemCount)
    }

    private fun navigateToProductDetail(product: Product) {
        val action = CategoryFragmentDirections.actionNavigationCategoryToProductDetail(product.id)
        findNavController().navigate(action)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.category_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                // Переход на экран поиска с предварительно выбранной категорией
                val intent = android.content.Intent(requireContext(), SearchActivity::class.java).apply {
                    putExtra("CATEGORY_ID", categoryId)
                    putExtra("CATEGORY_NAME", categoryName)
                }
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}