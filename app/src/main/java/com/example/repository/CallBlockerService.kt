package com.example.repository

import com.example.data.local.BlockedNumberDao
import com.example.data.local.SettingsPreferences
import com.example.data.model.BlockedNumberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class BlockCheckResult(
    val isBlocked: Boolean,
    val matchedRule: BlockedNumberEntity? = null,
    val reason: String = ""
)

class CallBlockerService(
    private val blockedDao: BlockedNumberDao,
    private val preferences: SettingsPreferences
) {
    val allBlockedRules: Flow<List<BlockedNumberEntity>> = blockedDao.getAllBlocked()

    suspend fun checkIsBlocked(phoneNumber: String): BlockCheckResult = withContext(Dispatchers.IO) {
        val cleanNumber = phoneNumber.filter { it.isDigit() }

        // 1. Check Unknown / Private numbers
        val blockUnknown = preferences.blockUnknownNumbers.value
        if (blockUnknown && (cleanNumber.isEmpty() || phoneNumber.equals("Unknown", ignoreCase = true) || phoneNumber.equals("Private", ignoreCase = true))) {
            return@withContext BlockCheckResult(
                isBlocked = true,
                matchedRule = null,
                reason = "Blocked by Private/Unknown numbers policy"
            )
        }

        // 2. Check rules in database
        val rules = blockedDao.getAllBlockedList()
        for (rule in rules) {
            val cleanPattern = rule.pattern.filter { it.isDigit() }
            if (rule.isPrefixPattern) {
                if (cleanNumber.startsWith(cleanPattern) || phoneNumber.startsWith(rule.pattern)) {
                    blockedDao.incrementBlockedCount(rule.id)
                    return@withContext BlockCheckResult(
                        isBlocked = true,
                        matchedRule = rule,
                        reason = "Matched prefix rule: ${rule.pattern}"
                    )
                }
            } else {
                if (cleanNumber == cleanPattern || phoneNumber.trim() == rule.pattern.trim()) {
                    blockedDao.incrementBlockedCount(rule.id)
                    return@withContext BlockCheckResult(
                        isBlocked = true,
                        matchedRule = rule,
                        reason = "Matched blocked number: ${rule.pattern}"
                    )
                }
            }
        }

        BlockCheckResult(isBlocked = false)
    }

    suspend fun addRule(pattern: String, isPrefix: Boolean, note: String = ""): Long = withContext(Dispatchers.IO) {
        val entity = BlockedNumberEntity(
            pattern = pattern.trim(),
            isPrefixPattern = isPrefix,
            note = note.trim()
        )
        blockedDao.insert(entity)
    }

    suspend fun deleteRule(rule: BlockedNumberEntity) = withContext(Dispatchers.IO) {
        blockedDao.delete(rule)
    }

    suspend fun seedInitialBlockedIfEmpty() = withContext(Dispatchers.IO) {
        val current = blockedDao.getAllBlockedList()
        if (current.isEmpty()) {
            blockedDao.insert(
                BlockedNumberEntity(
                    pattern = "800",
                    isPrefixPattern = true,
                    note = "Toll-free telemarketers prefix",
                    blockedCount = 14
                )
            )
            blockedDao.insert(
                BlockedNumberEntity(
                    pattern = "900",
                    isPrefixPattern = true,
                    note = "Premium rate numbers",
                    blockedCount = 3
                )
            )
            blockedDao.insert(
                BlockedNumberEntity(
                    pattern = "+1 (888) 555-0199",
                    isPrefixPattern = false,
                    note = "Known robocall scammer",
                    blockedCount = 5
                )
            )
        }
    }
}
