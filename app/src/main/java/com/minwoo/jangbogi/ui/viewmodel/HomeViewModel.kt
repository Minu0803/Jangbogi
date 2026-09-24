package com.minwoo.jangbogi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minwoo.jangbogi.data.JangbogiRepository
import com.minwoo.jangbogi.domain.ListWithProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: JangbogiRepository) : ViewModel() {

    val lists: StateFlow<List<ListWithProgress>> =
        repo.observeListsWithProgress()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createList(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(repo.createList(name))
        }
    }

    fun renameList(listId: Long, newName: String) {
        viewModelScope.launch { repo.renameList(listId, newName) }
    }

    fun deleteList(listId: Long) {
        viewModelScope.launch { repo.deleteList(listId) }
    }

    fun undoDeleteList() {
        viewModelScope.launch { repo.undoDeleteList() }
    }
}
