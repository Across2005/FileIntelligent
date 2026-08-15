package com.crossk

import com.crossk.data.RepoResult

import android.app.Application
import com.crossk.data.FileRepository
import com.crossk.data.SoundManager
import com.crossk.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CrossKApp : Application() {
    val fileRepository = FileRepository()
    lateinit var soundManager: SoundManager
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        soundManager = SoundManager(this)
        fileRepository.soundManager = soundManager

        // Wire Room database
        val db = AppDatabase.getInstance(this)
        fileRepository.database = db

        // Load persisted data from Room (overrides mock data)
        appScope.launch {
            fileRepository.loadFromRoom()
            when (val r = fileRepository.loadAllFromRoom()) {
                is RepoResult.Err -> { /* log only — first run is allowed to fail */ }
                is RepoResult.Ok -> { /* nothing extra */ }
            }
        }
    }

    /** Save repository state to Room — called by ViewModel and QuickCapture */
    fun saveState() {
        appScope.launch {
            fileRepository.saveAll()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        if (::soundManager.isInitialized) {
            soundManager.release()
        }
    }
}
