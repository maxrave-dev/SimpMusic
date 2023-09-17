package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.model.explore.mood.Mood
import com.maxrave.simpmusic.data.model.home.HomeItem
import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mainRepository: MainRepository,
    application: Application,
    private var dataStoreManager: DataStoreManager
) : AndroidViewModel(application) {
    private val _homeItemList: MutableLiveData<Resource<ArrayList<HomeItem>>> = MutableLiveData()
    val homeItemList: LiveData<Resource<ArrayList<HomeItem>>> = _homeItemList
    private val _exploreMoodItem: MutableLiveData<Resource<Mood>> = MutableLiveData()
    val exploreMoodItem: LiveData<Resource<Mood>> = _exploreMoodItem
    private val _accountInfo: MutableLiveData<Pair<String?, String?>?> = MutableLiveData()
    val accountInfo: LiveData<Pair<String?, String?>?> = _accountInfo

    val showSnackBarErrorState = MutableSharedFlow<String>()

    private val _chart: MutableLiveData<Resource<Chart>> = MutableLiveData()
    val chart: LiveData<Resource<Chart>> = _chart
    var regionCodeChart: MutableLiveData<String> = MutableLiveData()

    val loading = MutableLiveData<Boolean>()
    val loadingChart = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()
    private var regionCode: String = ""
    private var language: String = ""

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onError("Exception handled: ${throwable.localizedMessage}")
    }

    init {
        viewModelScope.launch {
            language = dataStoreManager.getString(SELECTED_LANGUAGE).first()
                ?: SUPPORTED_LANGUAGE.codes.first()
            //  refresh when region change
            dataStoreManager.location.distinctUntilChanged().collect {
                regionCode = it
                getHomeItemList()
            }
            dataStoreManager.language.distinctUntilChanged().collect {
                language = it
                getHomeItemList()
            }
        }

    }

    fun getHomeItemList() {
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() ?: SUPPORTED_LANGUAGE.codes.first() }
        regionCode = runBlocking { dataStoreManager.location.first() }
        loading.value = true
        viewModelScope.launch {
            combine(
//                mainRepository.getHome(
//                    regionCode,
//                    SUPPORTED_LANGUAGE.serverCodes[SUPPORTED_LANGUAGE.codes.indexOf(language)]
//                ),
                mainRepository.getHomeData(),
                mainRepository.getMoodAndMomentsData(),
                mainRepository.getChartData("ZZ")
            ) { home, exploreMood, exploreChart ->
                Triple(home, exploreMood, exploreChart)
            }.collect { result ->
                val home = result.first
                Log.d("home size", "${home.data?.size}")
                val exploreMoodItem = result.second
                val chart = result.third
                _homeItemList.value = home
                _exploreMoodItem.value = exploreMoodItem
                regionCodeChart.value = "ZZ"
                _chart.value = chart
                Log.d("HomeViewModel", "getHomeItemList: $result")
                loading.value = false
                dataStoreManager.cookie.first().let {
                    if (it != "") {
                        _accountInfo.postValue(Pair(dataStoreManager.getString("AccountName").first(), dataStoreManager.getString("AccountThumbUrl").first()))
                    }
                }
                when {
                    home is Resource.Error -> home.message
                    exploreMoodItem is Resource.Error -> exploreMoodItem.message
                    chart is Resource.Error -> chart.message
                    else -> null
                }?.let {
                    showSnackBarErrorState.emit(it)
                    Log.w("Error", "getHomeItemList: ${home.message}")
                    Log.w("Error", "getHomeItemList: ${exploreMoodItem.message}")
                    Log.w("Error", "getHomeItemList: ${chart.message}")
                }
            }
        }
    }

    fun exploreChart(region: String) {
        viewModelScope.launch {
            loadingChart.value = true
            mainRepository.getChartData(
                region).collect { values ->
                regionCodeChart.value = region
                _chart.value = values
                Log.d("HomeViewModel", "getHomeItemList: ${chart.value?.data}")
                loadingChart.value = false
            }
        }
    }

    private fun onError(message: String) {
        errorMessage.value = message
        loading.value = false
    }

}