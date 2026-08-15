package com.crossk.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crossk.CrossKApp
import kotlinx.coroutines.launch

/**
 * Main ViewModel providing auto-persistence for FileRepository.
 * Repository lives in CrossKApp (shared with Widget/QuickCapture).
 * This ViewModel adds automatic save-on-mutation behavior via Room.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CrossKApp
    val repository get() = app.fileRepository

    /** Save current state to Room */
    fun save() {
        viewModelScope.launch {
            repository.saveAll()
        }
    }

    fun addFileAndSave(name: String, content: String, extension: String, sizeBytes: Long) {
        repository.addFile(name, content, extension, sizeBytes)
        save()
    }

    fun deleteFileAndSave(id: String) {
        repository.deleteFile(id)
        save()
    }

    fun completeQuestAndSave(questId: String) {
        repository.gameEngine.completeQuest(questId)
        save()
    }

    fun addXpAndSave(amount: Int) {
        repository.gameEngine.addXpRaw(amount)
        save()
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            repository.saveAll()
        }
    }
}
