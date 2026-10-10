package com.os11.app.ui.features.tags

import com.os11.app.service.tag.TagItem

data class TagManagerUiState(
    val tags: List<TagItem> = emptyList(),
    val isLoading: Boolean = false,
    val selectedTagForRename: TagItem? = null,
    val selectedTagForRecolor: TagItem? = null,
    val selectedTagForMerge: TagItem? = null,
    val showCreateTagDialog: Boolean = false,
)
