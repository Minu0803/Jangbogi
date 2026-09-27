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
    private val _selectionEpoch = MutableStateFlow(0L)
    val selectionEpoch: StateFlow<Long> = _selectionEpoch.asStateFlow()
    private val _entryIntent = MutableStateFlow(PurchaseIntent.BUY)
    val entryIntent: StateFlow<PurchaseIntent> = _entryIntent.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _draft = MutableStateFlow(savedState["draft"] ?: false)
    val draft: StateFlow<Boolean> = _draft.asStateFlow()
    private val _draftQuery = MutableStateFlow(savedState["draft_query"] ?: "")
    val draftQuery: StateFlow<String> = _draftQuery.asStateFlow()
    private val _draftIntent = MutableStateFlow(
        savedState.get<String>("draft_intent")?.let { runCatching { PurchaseIntent.valueOf(it) }.getOrNull() }
            ?: PurchaseIntent.BUY
    )
    val draftIntent: StateFlow<PurchaseIntent> = _draftIntent.asStateFlow()
    private val _draftName = MutableStateFlow(savedState["draft_name"] ?: "내 장보기")
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
                if (active != null && entries.none { it.list.id == active }) {
                    _activeListId.value = repo.resolveActiveListId()
                }
            }
        }
    }

    fun selectList(listId: Long, onSelected: () -> Unit = {}) {
        viewModelScope.launch {
            if (repo.selectList(listId)) {
                _entryIntent.value = PurchaseIntent.BUY
                _draft.value = false
                savedState["draft"] = false
                _activeListId.value = listId
                _selectionEpoch.value += 1
                onSelected()
            }
        }
    }

    fun startNewDraft() {
        val date = LocalDate.now()
        val baseName = "${date.monthValue}월 ${date.dayOfMonth}일 장보기"
        var name = baseName
        var suffix = 2
        while (lists.value.any { it.list.name == name }) {
            name = "$baseName ($suffix)"
            suffix++
        }
        _draftName.value = name
        savedState["draft_name"] = _draftName.value
        _draftQuery.value = ""
        savedState["draft_query"] = ""
        _draftIntent.value = PurchaseIntent.BUY
        savedState["draft_intent"] = PurchaseIntent.BUY.name
        _draft.value = true
        savedState["draft"] = true
    }

    fun cancelNewDraft() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _activeListId.value = repo.resolveActiveListId()
                _entryIntent.value = PurchaseIntent.BUY
                _draft.value = false
                savedState["draft"] = false
            } finally {
                _loading.value = false
            }
        }
    }

    fun setDraftQuery(value: String) {
        _draftQuery.value = value
        savedState["draft_query"] = value
    }

    fun setDraftIntent(value: PurchaseIntent) {
        _draftIntent.value = value
        savedState["draft_intent"] = value.name
    }

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
                    _entryIntent.value = _draftIntent.value
                    _draftQuery.value = ""
                    savedState["draft_query"] = ""
                    _draft.value = false
                    savedState["draft"] = false
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
