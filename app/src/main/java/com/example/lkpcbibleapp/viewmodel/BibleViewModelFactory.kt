package com.example.lkpcbibleapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BibleViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BibleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BibleViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
