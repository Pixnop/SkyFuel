package leonfvt.skyfuel_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.domain.model.Category
import leonfvt.skyfuel_app.domain.model.CategoryColors
import leonfvt.skyfuel_app.domain.usecase.category.CreateCategoryUseCase
import leonfvt.skyfuel_app.domain.usecase.category.DeleteCategoryUseCase
import leonfvt.skyfuel_app.domain.usecase.category.GetAllCategoriesUseCase
import leonfvt.skyfuel_app.domain.usecase.category.UpdateCategoryUseCase
import javax.inject.Inject

/**
 * État de l'écran de gestion des catégories
 */
data class CategoryState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingCategory: Category? = null,
    val showDeleteConfirmation: Boolean = false,
    val categoryToDelete: Category? = null
)

/**
 * Événements de l'écran de catégories
 */
sealed class CategoryEvent {
    data object ShowCreateDialog : CategoryEvent()
    data object HideCreateDialog : CategoryEvent()
    data class ShowEditDialog(val category: Category) : CategoryEvent()
    data object HideEditDialog : CategoryEvent()
    data class CreateCategory(val name: String, val color: Long, val icon: String, val description: String) : CategoryEvent()
    data class UpdateCategory(val category: Category) : CategoryEvent()
    data class ShowDeleteConfirmation(val category: Category) : CategoryEvent()
    data object HideDeleteConfirmation : CategoryEvent()
    data class DeleteCategory(val category: Category) : CategoryEvent()
    data object ClearError : CategoryEvent()
}

/**
 * ViewModel pour la gestion des catégories
 */
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val getAllCategoriesUseCase: GetAllCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryState(isLoading = true))
    val state: StateFlow<CategoryState> = _state.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getAllCategoriesUseCase()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
                )
                .collect { categories ->
                    _state.value = _state.value.copy(
                        categories = categories,
                        isLoading = false
                    )
                }
        }
    }

    fun onEvent(event: CategoryEvent) {
        when (event) {
            is CategoryEvent.ShowCreateDialog -> {
                _state.value = _state.value.copy(showCreateDialog = true)
            }
            is CategoryEvent.HideCreateDialog -> {
                _state.value = _state.value.copy(showCreateDialog = false)
            }
            is CategoryEvent.ShowEditDialog -> {
                _state.value = _state.value.copy(
                    showEditDialog = true,
                    editingCategory = event.category
                )
            }
            is CategoryEvent.HideEditDialog -> {
                _state.value = _state.value.copy(
                    showEditDialog = false,
                    editingCategory = null
                )
            }
            is CategoryEvent.CreateCategory -> {
                createCategory(event.name, event.color, event.icon, event.description)
            }
            is CategoryEvent.UpdateCategory -> {
                updateCategory(event.category)
            }
            is CategoryEvent.ShowDeleteConfirmation -> {
                _state.value = _state.value.copy(
                    showDeleteConfirmation = true,
                    categoryToDelete = event.category
                )
            }
            is CategoryEvent.HideDeleteConfirmation -> {
                _state.value = _state.value.copy(
                    showDeleteConfirmation = false,
                    categoryToDelete = null
                )
            }
            is CategoryEvent.DeleteCategory -> {
                deleteCategory(event.category)
            }
            is CategoryEvent.ClearError -> {
                _state.value = _state.value.copy(error = null)
            }
        }
    }

    private fun createCategory(name: String, color: Long, icon: String, description: String) {
        viewModelScope.launch {
            try {
                val category = Category(
                    name = name,
                    color = color,
                    icon = icon,
                    description = description
                )
                createCategoryUseCase(category)
                _state.value = _state.value.copy(showCreateDialog = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Erreur lors de la création: ${e.message}"
                )
            }
        }
    }

    private fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                updateCategoryUseCase(category)
                _state.value = _state.value.copy(
                    showEditDialog = false,
                    editingCategory = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Erreur lors de la mise à jour: ${e.message}"
                )
            }
        }
    }

    private fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                deleteCategoryUseCase(category)
                _state.value = _state.value.copy(
                    showDeleteConfirmation = false,
                    categoryToDelete = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Erreur lors de la suppression: ${e.message}"
                )
            }
        }
    }
}
