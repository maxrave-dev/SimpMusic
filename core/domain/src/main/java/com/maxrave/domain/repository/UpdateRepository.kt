package com.maxrave.domain.repository

import com.maxrave.domain.data.model.update.UpdateData
import com.maxrave.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface UpdateRepository {
    fun checkForGithubReleaseUpdate(): Flow<Resource<UpdateData>>
    fun checkForFdroidUpdate(): Flow<Resource<UpdateData>>
}