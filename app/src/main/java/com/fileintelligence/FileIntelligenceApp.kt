package com.fileintelligence

import android.app.Application
import com.fileintelligence.data.FileRepository

class FileIntelligenceApp : Application() {
    val fileRepository = FileRepository()
}
