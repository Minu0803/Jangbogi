package com.minwoo.jangbogi.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minwoo.jangbogi.data.JangbogiRepository
import com.minwoo.jangbogi.domain.ListWithProgress
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** A shopping destination is always bound to the explicitly selected store. */
class ShoppingHomeViewModel(repo: JangbogiRepository, savedState: SavedStateHandle) : ViewModel() {
    val storeId: Long = requireNotNull(savedState["listId"])
    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()
    private val _exists = MutableStateFlow(true)
    val exists = _exists.asStateFlow()
    val lists: StateFlow<List<ListWithProgress>> = repo.observeListsWithProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    init {
        viewModelScope.launch {
            repo.observeList(storeId).collect { store ->
                _exists.value = store != null
                _loading.value = false
            }
        }
    }
}
