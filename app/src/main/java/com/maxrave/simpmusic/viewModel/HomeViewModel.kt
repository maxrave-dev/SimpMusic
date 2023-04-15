package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.data.model.explore.mood.Genre
import com.maxrave.simpmusic.data.model.explore.mood.Mood
import com.maxrave.simpmusic.data.model.home.homeItem
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val mainRepository: MainRepository, application: Application) : AndroidViewModel(application) {
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

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onError("Exception handled: ${throwable.localizedMessage}")
    }


    fun getHomeItemList() {
        loading.value = true
        val parentJob = viewModelScope.launch {
            val job1 = viewModelScope.launch {
                mainRepository.getHome().collect {values->
                    _homeItemList.value = values
                    Log.d("HomeViewModel", "getHomeItemList: ${homeItemList.value?.data}")
                }
            }
            val job2 = viewModelScope.launch {
                mainRepository.exploreMood().collect{values ->
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
    fun exploreChart(regionCode: String){
        loadingChart.value = true
        val job = viewModelScope.launch {
            mainRepository.exploreChart(regionCode).collect{values ->
                regionCodeChart.value = regionCode
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
}