package com.androiddevs.mvvmnewsapp.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.androiddevs.mvvmnewsapp.models.Source
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "articles"
)
data class Article(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val author: String?,
    val content: String?,
    val description: String?,
    val publishedAt: String?,
    var source: Source?,
    val title: String?,
    val url: String?,
    val urlToImage: String?
) : Parcelable



