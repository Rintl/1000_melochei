package com.example.a1000_melochei.ui.customer.catalog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Category
import com.example.a1000_melochei.databinding.FragmentCatalogBinding
import com.example.a1000_melochei.ui.customer.catalog.adapter.CategoryAdapter
import com.example.a1000_melochei.ui.customer.catalog.viewmodel.CatalogViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Фрагмент каталога товаров.
 * Отображает список категорий товаров в виде сетки.
 */
class CatalogFragment : Fragment() {

    private var _binding: FragmentCatalogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CatalogViewModel by viewModel()
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Загружаем категории
        viewModel.loadCategories()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter { category ->
            navigateToCategoryFragment(category)
        }

        binding.rvCategories.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = categoryAdapter
        }
    }

    private fun setupObservers() {
        // Наблюдение за категориями
        viewModel.categories.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Loading -> {
                    showLoading(true)
                    showError(false)
                }
                is Resource.Success -> {
                    showLoading(false)
                    showError(false)

                    result.data?.let { categories ->
                        if (categories.isEmpty()) {
                            showEmptyState(true)
                        } else {
                            showEmptyState(false)
                            categoryAdapter.submitList(categories)
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
            viewModel.loadCategories()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Обработка повторной попытки загрузки при ошибке
        binding.btnRetry.setOnClickListener {
            viewModel.loadCategories()
        }

        // Обработка поиска
        binding.searchBar.setOnClickListener {
            navigateToSearch()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.rvCategories.isVisible = !isLoading && !binding.errorLayout.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showError(isError: Boolean, message: String? = null) {
        binding.errorLayout.isVisible = isError
        binding.errorMessage.text = message ?: getString(R.string.error_loading_categories)
        binding.rvCategories.isVisible = !isError && !binding.progressBar.isVisible && !binding.emptyLayout.isVisible
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyLayout.isVisible = isEmpty
        binding.rvCategories.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible
    }

    private fun navigateToCategoryFragment(category: Category) {
        val action = CatalogFragmentDirections.actionNavigationCatalogToNavigationCategory(category.id)
        findNavController().navigate(action)
    }

    private fun navigateToSearch() {
        startActivity(Intent(requireContext(), SearchActivity::class.java))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.catalog_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    val intent = Intent(requireContext(), SearchActivity::class.java).apply {
                        putExtra("SEARCH_QUERY", query)
                    }
                    startActivity(intent)
                    return true
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Можно реализовать поиск по мере ввода текста
                return false
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                navigateToSearch()
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