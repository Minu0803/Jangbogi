package com.minwoo.jangbogi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minwoo.jangbogi.data.JangbogiRepository
import com.minwoo.jangbogi.domain.Category
import com.minwoo.jangbogi.domain.ItemOrganizer
import com.minwoo.jangbogi.domain.ListUiState
import com.minwoo.jangbogi.domain.ShareTextBuilder
import com.minwoo.jangbogi.domain.Suggestion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ListViewModel(
    private val repo: JangbogiRepository,
    private val listId: Long
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val uiState: StateFlow<ListUiState> =
        combine(repo.observeList(listId), repo.observeItems(listId)) { list, items ->
            ListUiState(
                listName = list?.name.orEmpty(),
                sections = ItemOrganizer.sections(items),
                completedItems = ItemOrganizer.completed(items),
                totalCount = items.size,
                checkedCount = items.count { it.isChecked },
                isLoading = false
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListUiState())

    val suggestions: StateFlow<List<Suggestion>> =
        _query.flatMapLatest { q -> repo.observeSuggestions(listId, q.trim()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun addItem(rawInput: String) {
        viewModelScope.launch {
            if (repo.addItem(listId, rawInput)) _query.value = ""
        }
    }

    fun toggleItem(itemId: Long) {
        viewModelScope.launch { repo.toggleItem(itemId) }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch { repo.deleteItem(itemId) }
    }

    fun undoDeleteItem() {
        viewModelScope.launch { repo.undoDeleteItem() }
    }

    fun updateItem(itemId: Long, name: String, quantity: Int, category: Category) {
        viewModelScope.launch { repo.updateItem(itemId, name, quantity, category) }
    }

    fun renameList(newName: String) {
        viewModelScope.launch { repo.renameList(listId, newName) }
    }

    fun clearCompleted() {
        viewModelScope.launch { repo.clearCompleted(listId) }
    }

    fun undoClearCompleted() {
        viewModelScope.launch { repo.undoClearCompleted() }
    }

    fun buildShareText(): String {
        val state = uiState.value
        val items = state.sections.flatMap { it.items } + state.completedItems
        return ShareTextBuilder.build(state.listName, items)
    }
}
