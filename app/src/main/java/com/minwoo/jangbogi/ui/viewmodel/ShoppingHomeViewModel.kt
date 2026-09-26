package com.minwoo.jangbogi.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minwoo.jangbogi.data.JangbogiRepository
import com.minwoo.jangbogi.domain.ListWithProgress
import com.minwoo.jangbogi.domain.PurchaseIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ShoppingHomeViewModel(
    private val repo: JangbogiRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val _activeListId = MutableStateFlow<Long?>(null)
    val activeListId: StateFlow<Long?> = _activeListId.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _draft = MutableStateFlow(false)
    val draft: StateFlow<Boolean> = _draft.asStateFlow()
    private val _draftQuery = MutableStateFlow(savedState["draft_query"] ?: "")
    val draftQuery: StateFlow<String> = _draftQuery.asStateFlow()
    private val _draftIntent = MutableStateFlow(PurchaseIntent.BUY)
    val draftIntent: StateFlow<PurchaseIntent> = _draftIntent.asStateFlow()
    private val _draftName = MutableStateFlow("내 장보기")
    val draftName: StateFlow<String> = _draftName.asStateFlow()
    private var adding = false

    val lists: StateFlow<List<ListWithProgress>> = repo.observeListsWithProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _activeListId.value = repo.resolveActiveListId()
            _loading.value = false
            repo.observeListsWithProgress().collect { entries ->
                val active = _activeListId.value
                if (!_draft.value && active != null && entries.none { it.list.id == active }) {
                    _activeListId.value = repo.resolveActiveListId()
                }
            }
        }
    }

    fun selectList(listId: Long) {
        viewModelScope.launch {
            if (repo.selectList(listId)) {
                _draft.value = false
                _activeListId.value = listId
            }
        }
    }

    fun startNewDraft() {
        val date = LocalDate.now()
        _draftName.value = "${date.monthValue}월 ${date.dayOfMonth}일 장보기"
        _draftQuery.value = ""
        savedState["draft_query"] = ""
        _draftIntent.value = PurchaseIntent.BUY
        _draft.value = true
    }

    fun cancelNewDraft() { _draft.value = false }

    fun setDraftQuery(value: String) {
        _draftQuery.value = value
        savedState["draft_query"] = value
    }

    fun setDraftIntent(value: PurchaseIntent) { _draftIntent.value = value }

    fun savedQuery(listId: Long): String = savedState["query_$listId"] ?: ""

    fun saveQuery(listId: Long, value: String) { savedState["query_$listId"] = value }

    fun addToDraft(input: String, onError: (String) -> Unit) {
        if (adding || input.isBlank()) return
        adding = true
        viewModelScope.launch {
            try {
                val id = repo.addToNewList(input, _draftIntent.value, _draftName.value)
                if (id == null) onError("물건 이름을 적어주세요")
                else {
                    _draftQuery.value = ""
                    savedState["draft_query"] = ""
                    _draft.value = false
                    _activeListId.value = id
                }
            } catch (_: Exception) {
                onError("저장하지 못했어요. 다시 시도해 주세요")
            } finally {
                adding = false
            }
        }
    }
}
