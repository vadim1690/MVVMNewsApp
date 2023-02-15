package com.androiddevs.mvvmnewsapp.util


// sealed class is a class the will specify its extenders as below Success,Error,Loading
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T> : Resource<T>()
    class Empty<T> : Resource<T>()
}