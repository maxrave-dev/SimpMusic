//package com.maxrave.simpmusic.viewModel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import com.maxrave.simpmusic.data.repository.MainRepository
//
//class ViewModelFactory(private val repository: MainRepository): ViewModelProvider.Factory {
//
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        return if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
//            SearchViewModel(this.repository) as T
//        } else {
//            throw IllegalArgumentException("ViewModel Not Found")
//        }
//    }
//}