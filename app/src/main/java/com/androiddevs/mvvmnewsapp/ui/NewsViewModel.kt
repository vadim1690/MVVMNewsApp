package com.androiddevs.mvvmnewsapp.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.*
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.androiddevs.mvvmnewsapp.App
import com.androiddevs.mvvmnewsapp.models.Article
import com.androiddevs.mvvmnewsapp.models.NewsResponse
import com.androiddevs.mvvmnewsapp.repository.NewsRepository
import com.androiddevs.mvvmnewsapp.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class NewsViewModel(
    app: Application,
    val newsRepository: NewsRepository
) : AndroidViewModel(app) {
    var breakingNewsPage = 1
    var breakingNewsResponse: NewsResponse? = null

    var searchNewsPage = 1
    var searchNewsResponse: NewsResponse? = null
    private val _breakingNewsFlow = MutableStateFlow<Resource<NewsResponse>>(Resource.Empty())
    val breakingNewsFlow = _breakingNewsFlow
    private val _searchNewsFlow = MutableStateFlow<Resource<NewsResponse>>(Resource.Empty())
    val searchNewsFlow = _searchNewsFlow

    init {
        getBreakingNews("us")
    }

    fun getBreakingNews(countryCode: String) = viewModelScope.launch {
        safeBreakingNewsCallWithFlow(countryCode)
    }

    fun getSearchNews(queryString: String) = viewModelScope.launch {
        safeSearchNewsCall(queryString)
    }

    fun saveArticle(article: Article) = viewModelScope.launch {
        newsRepository.upsert(article)
    }

    fun getSavedNews() = newsRepository.getSavedNews()

    fun deleteArticle(article: Article) = viewModelScope.launch {
        newsRepository.deleteArticle(article)
    }






    private suspend fun safeSearchNewsCall(searchQuery: String) {
        _searchNewsFlow.value = Resource.Loading()
        try {
            if (hasInternetConnection()) {
                val response = newsRepository.searchNews(searchQuery, searchNewsPage)
                _searchNewsFlow.value = handleSearchNewsResponse(response)
            } else {
                _searchNewsFlow.value = Resource.Error("No Internet connection")
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> _searchNewsFlow.value = Resource.Error("Network failure")
                else -> _searchNewsFlow.value = Resource.Error("Conversion Error")
            }
        }
    }

    private suspend fun safeBreakingNewsCallWithFlow(countryCode: String) {
        _breakingNewsFlow.value = Resource.Loading()
        try {
            if (hasInternetConnection()) {
                val response = newsRepository.getBreakingNews(countryCode, breakingNewsPage)
                _breakingNewsFlow.value = handleBreakingNewsResponse(response)
            } else {
                _breakingNewsFlow.value = Resource.Error("No Internet connection")
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> _breakingNewsFlow.value = Resource.Error("Network Failure")
                else -> _breakingNewsFlow.value = Resource.Error("Conversion Error")
            }
        }
    }

    private fun handleBreakingNewsResponse(response: Response<NewsResponse>): Resource<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let { newsResponse ->
                breakingNewsPage++
                if (breakingNewsResponse == null) {
                    breakingNewsResponse = newsResponse
                } else {
                    val oldArticles = breakingNewsResponse?.articles
                    val newArticles = newsResponse.articles
                    oldArticles?.addAll(newArticles)
                }
                return Resource.Success(breakingNewsResponse ?: newsResponse)
            }
        }
        return Resource.Error(response.message())
    }

    private fun handleSearchNewsResponse(response: Response<NewsResponse>): Resource<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let { newsResponse ->
                searchNewsPage++
                if (searchNewsResponse == null) {
                    searchNewsResponse = newsResponse
                } else {
                    val oldArticles = searchNewsResponse?.articles
                    val newArticles = newsResponse.articles
                    oldArticles?.addAll(newArticles)
                }
                return Resource.Success(searchNewsResponse ?: newsResponse)
            }
        }
        return Resource.Error(response.message())
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = getApplication<App>().getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }

        } else {
            connectivityManager.activeNetworkInfo?.run {
                return when (type) {
                    TYPE_WIFI -> true
                    TYPE_MOBILE -> true
                    TYPE_ETHERNET -> true
                    else -> false
                }
            }
        }
        return false
    }

}