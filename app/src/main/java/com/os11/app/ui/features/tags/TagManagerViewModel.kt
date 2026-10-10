package com.os11.app.ui.features.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.tag.TagItem
import com.os11.app.service.tag.TagService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class TagManagerViewModel @Inject constructor(private val tagService: TagService) : ViewModel() {

    private val dialogsState = MutableStateFlow(
        Dialogs(
            renameTag = null,
            recolorTag = null,
            mergeTag = null,
            showCreate = false,
        ),
    )

    private data class Dialogs(
        val renameTag: TagItem?,
        val recolorTag: TagItem?,
        val mergeTag: TagItem?,
        val showCreate: Boolean,
    )

    val uiState: StateFlow<TagManagerUiState> = combine(
        tagService.observeTagItems(),
        dialogsState,
    ) { tags, dialogs ->
        TagManagerUiState(
            tags = tags,
            isLoading = false,
            selectedTagForRename = dialogs.renameTag,
            selectedTagForRecolor = dialogs.recolorTag,
            selectedTagForMerge = dialogs.mergeTag,
            showCreateTagDialog = dialogs.showCreate,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TagManagerUiState(isLoading = true),
    )

    fun openCreateTagDialog() {
        dialogsState.value = dialogsState.value.copy(showCreate = true)
    }

    fun closeCreateTagDialog() {
        dialogsState.value = dialogsState.value.copy(showCreate = false)
    }

    fun openRenameDialog(tag: TagItem) {
        dialogsState.value = dialogsState.value.copy(renameTag = tag)
    }

    fun closeRenameDialog() {
        dialogsState.value = dialogsState.value.copy(renameTag = null)
    }

    fun openRecolorDialog(tag: TagItem) {
        dialogsState.value = dialogsState.value.copy(recolorTag = tag)
    }

    fun closeRecolorDialog() {
        dialogsState.value = dialogsState.value.copy(recolorTag = null)
    }

    fun openMergeDialog(tag: TagItem) {
        dialogsState.value = dialogsState.value.copy(mergeTag = tag)
    }

    fun closeMergeDialog() {
        dialogsState.value = dialogsState.value.copy(mergeTag = null)
    }

    fun createTag(name: String, color: String? = null, parentTagId: String? = null) {
        viewModelScope.launch {
            tagService.createTag(name = name, color = color, parentTagId = parentTagId)
            closeCreateTagDialog()
        }
    }

    fun renameTag(tagId: String, newName: String) {
        viewModelScope.launch {
            tagService.renameTag(tagId, newName)
            closeRenameDialog()
        }
    }

    fun recolorTag(tagId: String, newColor: String?) {
        viewModelScope.launch {
            tagService.recolorTag(tagId, newColor)
            closeRecolorDialog()
        }
    }

    fun mergeTags(sourceTagId: String, targetTagId: String) {
        viewModelScope.launch {
            tagService.mergeTags(sourceTagId, targetTagId)
            closeMergeDialog()
        }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            tagService.deleteTag(tagId)
        }
    }
}
