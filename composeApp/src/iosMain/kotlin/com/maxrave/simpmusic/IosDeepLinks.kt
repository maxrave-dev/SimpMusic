package com.maxrave.simpmusic

import com.eygraber.uri.Uri
import com.maxrave.domain.data.model.intent.GenericIntent
import com.maxrave.simpmusic.viewModel.SharedViewModel
import org.koin.mp.KoinPlatform.getKoin

fun handleIosUrl(url: String) {
    getKoin().get<SharedViewModel>().setIntent(GenericIntent(data = Uri.parse(url)))
}
