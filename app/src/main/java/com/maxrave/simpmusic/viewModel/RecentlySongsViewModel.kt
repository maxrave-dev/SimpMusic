package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.maxrave.simpmusic.pagination.RecentPagingSource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel

@UnstableApi
class RecentlySongsViewModel(
    application: Application,
) : BaseViewModel(application) {
    val recentlySongs =
        Pager(
            PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20,
            ),
        ) {
            RecentPagingSource(mainRepository)
        }.flow.cachedIn(viewModelScope)
}