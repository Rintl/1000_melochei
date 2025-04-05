package com.yourstore.app.ui.customer.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.yourstore.app.R
import com.yourstore.app.data.common.Resource
import com.yourstore.app.data.model.Category
import com.yourstore.app.data.model.Product
import com.yourstore.app.data.model.Promotion
import com.yourstore.app.databinding.FragmentHomeBinding
import com.yourstore.app.ui.customer.catalog.adapter.CategoryAdapter
import com.yourstore.app.ui.customer.catalog.adapter.ProductAdapter
import com.yourstore.app.ui.customer.home.adapter.PromotionAdapter
import com.yourstore.app.ui.customer.home.viewmodel.HomeViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Фрагмент главной страницы клиентского интерфейса.
 * Отображает баннеры, промо-акции и популярные товары.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModel()

    private lateinit var promotionAdapter: PromotionAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var popularProductsAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupObservers()
        setupListeners()

        // Загрузка данных
        viewModel.loadData()
    }

    private fun setupRecyclerViews() {
        // Настройка адаптера для промо-акций
        promotionAdapter = PromotionAdapter { promotion ->
            handlePromotionClick(promotion)
        }
        binding.rvPromotions.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = promotionAdapter
        }

        // Настройка адаптера для категорий
        categoryAdapter = CategoryAdapter { category ->
            navigateToCategoryFragment(category)
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        // Настройка адаптера для популярных товаров
        popularProductsAdapter = ProductAdapter { product ->
            navigateToProductDetail(product)
        }
        binding.rvPopularProducts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = popularProductsAdapter
        }
    }

    private fun setupObservers() {
        // Наблюдение за промо-акциями
        viewModel.promotions.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.promotionsProgressBar.isVisible = true
                    binding.rvPromotions.isVisible = false
                }
                is Resource.Success -> {
                    binding.promotionsProgressBar.isVisible = false
                    binding.promotionsErrorMessage.isVisible = false
                    binding.rvPromotions.isVisible = true

                    result.data?.let { promotions ->
                        binding.promotionsEmptyMessage.isVisible = promotions.isEmpty()
                        promotionAdapter.submitList(promotions)
                    }
                }
                is Resource.Error -> {
                    binding.promotionsProgressBar.isVisible = false
                    binding.rvPromotions.isVisible = false
                    binding.promotionsErrorMessage.isVisible = true
                    binding.promotionsErrorMessage.text = result.message ?: getString(R.string.error_loading_promotions)
                }
            }
        })

        // Наблюдение за категориями
        viewModel.categories.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.categoriesProgressBar.isVisible = true
                    binding.rvCategories.isVisible = false
                }
                is Resource.Success -> {
                    binding.categoriesProgressBar.isVisible = false
                    binding.categoriesErrorMessage.isVisible = false
                    binding.rvCategories.isVisible = true

                    result.data?.let { categories ->
                        binding.categoriesEmptyMessage.isVisible = categories.isEmpty()
                        categoryAdapter.submitList(categories)
                    }
                }
                is Resource.Error -> {
                    binding.categoriesProgressBar.isVisible = false
                    binding.rvCategories.isVisible = false
                    binding.categoriesErrorMessage.isVisible = true
                    binding.categoriesErrorMessage.text = result.message ?: getString(R.string.error_loading_categories)
                }
            }
        })

        // Наблюдение за популярными товарами
        viewModel.popularProducts.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.popularProductsProgressBar.isVisible = true
                    binding.rvPopularProducts.isVisible = false
                }
                is Resource.Success -> {
                    binding.popularProductsProgressBar.isVisible = false
                    binding.popularProductsErrorMessage.isVisible = false
                    binding.rvPopularProducts.isVisible = true

                    result.data?.let { products ->
                        binding.popularProductsEmptyMessage.isVisible = products.isEmpty()
                        popularProductsAdapter.submitList(products)
                    }
                }
                is Resource.Error -> {
                    binding.popularProductsProgressBar.isVisible = false
                    binding.rvPopularProducts.isVisible = false
                    binding.popularProductsErrorMessage.isVisible = true
                    binding.popularProductsErrorMessage.text = result.message ?: getString(R.string.error_loading_popular_products)
                }
            }
        })
    }

    private fun setupListeners() {
        // Обработка нажатия на кнопку "Смотреть весь каталог"
        binding.btnViewAllCategories.setOnClickListener {
            findNavController().navigate(R.id.navigation_catalog)
        }

        // Обработка нажатия на кнопку поиска
        binding.searchBar.setOnClickListener {
            navigateToSearch()
        }

        // Обработка кнопки обновления
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadData()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun handlePromotionClick(promotion: Promotion) {
        // Обработка нажатия на промо-акцию
        when (promotion.type) {
            "category" -> {
                promotion.categoryId?.let { categoryId ->
                    val action = HomeFragmentDirections.actionNavigationHomeToNavigationCategory(categoryId)
                    findNavController().navigate(action)
                }
            }
            "product" -> {
                promotion.productId?.let { productId ->
                    val action = HomeFragmentDirections.actionNavigationHomeToProductDetail(productId)
                    findNavController().navigate(action)
                }
            }
            else -> {
                Toast.makeText(requireContext(), promotion.title, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToCategoryFragment(category: Category) {
        val action = HomeFragmentDirections.actionNavigationHomeToNavigationCategory(category.id)
        findNavController().navigate(action)
    }

    private fun navigateToProductDetail(product: Product) {
        val action = HomeFragmentDirections.actionNavigationHomeToProductDetail(product.id)
        findNavController().navigate(action)
    }

    private fun navigateToSearch() {
        // Переход на экран поиска через интент
        val intent = android.content.Intent(requireContext(), com.yourstore.app.ui.customer.catalog.SearchActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}