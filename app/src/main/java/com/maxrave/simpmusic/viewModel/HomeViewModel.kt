package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.data.model.explore.mood.Genre
import com.maxrave.simpmusic.data.model.explore.mood.Mood
import com.maxrave.simpmusic.data.model.home.homeItem
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val mainRepository: MainRepository, application: Application, private var dataStoreManager: DataStoreManager) : AndroidViewModel(application) {
    private val _homeItemList: MutableLiveData<Resource<ArrayList<homeItem>>> = MutableLiveData()
    val homeItemList: LiveData<Resource<ArrayList<homeItem>>> = _homeItemList
    private val _exploreMoodItem: MutableLiveData<Resource<Mood>> = MutableLiveData()
    val exploreMoodItem: LiveData<Resource<Mood>> = _exploreMoodItem

    private val _chart: MutableLiveData<Resource<Chart>> = MutableLiveData()
    val chart: LiveData<Resource<Chart>> = _chart
    var regionCodeChart: MutableLiveData<String> = MutableLiveData()

    var loading = MutableLiveData<Boolean>()
    var loadingChart = MutableLiveData<Boolean>()
    var errorMessage = MutableLiveData<String>()
    private var regionCode: String? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onError("Exception handled: ${throwable.localizedMessage}")
    }
    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
    }


    fun getHomeItemList() {
        loading.value = true
        viewModelScope.launch {
            val job1 = viewModelScope.launch {
                mainRepository.getHome(regionCode!!).collect {values->
                    _homeItemList.value = values
                    Log.d("HomeViewModel", "getHomeItemList: ${homeItemList.value?.data}")
                }
            }
            val job2 = viewModelScope.launch {
                mainRepository.exploreMood(regionCode!!).collect{values ->
                    _exploreMoodItem.value = values
                    Log.d("HomeViewModel", "getHomeItemList: ${exploreMoodItem.value?.data}")
                    loading.value = false
                }
            }
            val job3 = viewModelScope.launch {
                mainRepository.exploreChart("ZZ").collect{values ->
                    regionCodeChart.value = "ZZ"
                    _chart.value = values
                    Log.d("HomeViewModel", "getHomeItemList: ${chart.value?.data}")
                    loading.value = false
                }
            }
            job1.join()
            job2.join()
            job3.join()
            withContext(Dispatchers.Main) {
                loading.value = false
            }
        }
    }
    fun exploreChart(region: String){
        loadingChart.value = true
        viewModelScope.launch {
            mainRepository.exploreChart(region).collect{values ->
                regionCodeChart.value = region
                _chart.value = values
                Log.d("HomeViewModel", "getHomeItemList: ${chart.value?.data}")
                loadingChart.value = false
            }
            withContext(Dispatchers.Main) {
                loadingChart.value = false
            }
        }
    }

    private fun onError(message: String) {
        errorMessage.value = message
        loading.value = false
    }

    fun getLocation() {
        viewModelScope.launch {
            dataStoreManager.location.collect { location ->
                regionCode = location
            }
        }
    }
}