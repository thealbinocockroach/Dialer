package com.example.repository

import com.example.data.local.QuickResponseDao
import com.example.data.model.QuickResponseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class QuickResponsesRepository(
    private val quickResponseDao: QuickResponseDao
) {
    val allResponses: Flow<List<QuickResponseEntity>> = quickResponseDao.getAll()

    suspend fun addResponse(text: String) = withContext(Dispatchers.IO) {
        quickResponseDao.insert(QuickResponseEntity(text = text.trim()))
    }

    suspend fun deleteResponse(response: QuickResponseEntity) = withContext(Dispatchers.IO) {
        quickResponseDao.delete(response)
    }

    suspend fun seedDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        if (quickResponseDao.getCount() == 0) {
            val defaults = listOf(
                QuickResponseEntity(text = "Can't talk right now. What's up?", isDefault = true),
                QuickResponseEntity(text = "In a meeting, will call you later.", isDefault = true),
                QuickResponseEntity(text = "On my way. Will call when I arrive.", isDefault = true),
                QuickResponseEntity(text = "Please text me instead.", isDefault = true)
            )
            quickResponseDao.insertAll(defaults)
        }
    }
}
