package com.maxrave.domain.repository

import com.maxrave.domain.data.entities.GoogleAccountEntity
import com.maxrave.domain.data.model.account.AccountInfo
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccountInfo(cookie: String): Flow<List<AccountInfo>>

    fun getYouTubeCookie(): String?

    fun insertGoogleAccount(googleAccountEntity: GoogleAccountEntity): Flow<Long>

    fun getGoogleAccounts(): Flow<List<GoogleAccountEntity>?>

    fun getUsedGoogleAccount(): Flow<GoogleAccountEntity?>

    suspend fun deleteGoogleAccount(email: String)

    fun updateGoogleAccountUsed(
        email: String,
        isUsed: Boolean,
    ): Flow<Int>
}