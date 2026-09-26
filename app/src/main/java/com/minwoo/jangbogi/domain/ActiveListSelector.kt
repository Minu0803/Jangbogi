package com.minwoo.jangbogi.domain

object ActiveListSelector {
    fun choose(storedId: Long?, lists: List<ListWithProgress>): Long? {
        if (storedId != null && lists.any { it.list.id == storedId }) return storedId
        val order = compareBy<ListWithProgress>({ it.list.createdAt }, { it.list.id })
        return (lists.filter { it.totalCount > it.checkedCount }.maxWithOrNull(order)
            ?: lists.maxWithOrNull(order))?.list?.id
    }
}
