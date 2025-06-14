package com.example.a1000_melochei.ui.admin.categories

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.example.a1000_melochei.R
import com.example.a1000_melochei.data.common.Resource
import com.example.a1000_melochei.data.model.Category
import com.example.a1000_melochei.databinding.FragmentCategoryListBinding
import com.example.a1000_melochei.ui.admin.AdminActivity
import com.example.a1000_melochei.ui.admin.categories.adapter.AdminCategoryAdapter
import com.example.a1000_melochei.ui.admin.categories.viewmodel.CategoryViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Фрагмент для отображения списка категорий в административной панели.
 * Позволяет просматривать, добавлять, редактировать и удалять категории.
 */
class CategoryListFragment : Fragment(), AdminActivity.Refreshable {

    private var _binding: FragmentCategoryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by viewModel()
    private lateinit var categoryAdapter: AdminCategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Загружаем список категорий
        viewModel.loadCategories()
    }

    private fun setupRecyclerView() {
        categoryAdapter = AdminCategoryAdapter(
            onItemClick = { category ->
                navigateToEditCategory(category)
            },
            onDeleteClick = { category ->
                showDeleteConfirmation(category)
            },
            onVisibilityToggleClick = { category, isVisible ->
                viewModel.toggleCategoryVisibility(category.id, isVisible)
            }
        )

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }

    private fun setupObservers() {
        // Наблюдение за списком категорий
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

                            // Обновляем счетчик категорий
                            binding.tvCategoryCount.text = getString(R.string.categories_count, categories.size)
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

        // Наблюдение за результатом удаления категории
        viewModel.deleteCategoryResult.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Success -> {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.category_deleted),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    // Обновляем список категорий
                    viewModel.loadCategories()
                }
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        result.message ?: getString(R.string.error_deleting_category),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })

        // Наблюдение за результатом изменения видимости категории
        viewModel.updateCategoryVisibilityResult.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        result.message ?: getString(R.string.error_updating_category_visibility),
                        Toast.LENGTH_SHORT
                    ).show()
                    // Перезагружаем список в случае ошибки
                    viewModel.loadCategories()
                }
                else -> { /* Игнорируем другие состояния */ }
            }
        })

        // Наблюдение за отфильтрованными категориями
        viewModel.filteredCategories.observe(viewLifecycleOwner, Observer { categories ->
            if (categories.isEmpty()) {
                // Если нет результатов поиска
                if (viewModel.isSearchActive()) {
                    binding.emptyLayout.isVisible = true
                    binding.emptyMessage.text = getString(R.string.no_categories_found)
                    binding.rvCategories.isVisible = false
                } else {
                    binding.emptyLayout.isVisible = binding.progressBar.isVisible == false &&
                            binding.errorLayout.isVisible == false
                    binding.rvCategories.isVisible = false
                }
            } else {
                binding.emptyLayout.isVisible = false
                binding.rvCategories.isVisible = binding.progressBar.isVisible == false &&
                        binding.errorLayout.isVisible == false

                categoryAdapter.submitList(categories)
                binding.tvCategoryCount.text = getString(R.string.categories_count, categories.size)
            }
        })
    }

    private fun setupListeners() {
        // Кнопка добавления новой категории
        binding.fabAddCategory.setOnClickListener {
            navigateToAddCategory()
        }

        // Обновление через свайп вниз
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadCategories()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Кнопка повторной попытки загрузки при ошибке
        binding.btnRetry.setOnClickListener {
            viewModel.loadCategories()
        }

        // Кнопка перехода к добавлению категории при пустом списке
        binding.btnAddCategory.setOnClickListener {
            navigateToAddCategory()
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
        binding.emptyLayout.isVisible = isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible
        binding.rvCategories.isVisible = !isEmpty && !binding.progressBar.isVisible && !binding.errorLayout.isVisible
    }

    private fun showDeleteConfirmation(category: Category) {
        // Проверяем, есть ли товары в категории
        if (category.productCount > 0) {
            // Предупреждаем о наличии товаров
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.category_contains_products_title))
                .setMessage(getString(R.string.category_contains_products_message, category.productCount))
                .setPositiveButton(R.string.delete_anyway) { _, _ ->
                    confirmDeleteCategory(category)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            // Если товаров нет, просто подтверждаем удаление
            confirmDeleteCategory(category)
        }
    }

    private fun confirmDeleteCategory(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_category_title))
            .setMessage(getString(R.string.delete_category_message, category.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteCategory(category.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun navigateToAddCategory() {
        startActivity(Intent(requireContext(), AddCategoryActivity::class.java))
    }

    private fun navigateToEditCategory(category: Category) {
        val intent = Intent(requireContext(), AddCategoryActivity::class.java).apply {
            putExtra("CATEGORY_ID", category.id)
            putExtra("EDIT_MODE", true)
        }
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.category_list_menu, menu)

        // Настройка поиска
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchCategories(newText ?: "")
                return true
            }
        })

        // Сброс поиска при закрытии SearchView
        searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.searchCategories("")
                return true
            }
        })
    }

    override fun refresh() {
        viewModel.loadCategories()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем данные при возвращении на экран
        viewModel.loadCategories()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}