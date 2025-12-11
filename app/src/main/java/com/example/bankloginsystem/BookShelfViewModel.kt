package com.example.bankloginsystem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow

class BookShelfViewModel(
    private val firebaseManager: FirebaseManager = FirebaseManager()
) : ViewModel() {

    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    // Define a constant for the page size.
    private val pageSize = 10

    val bookPagingFlow: Flow<PagingData<Book>> =
        if (userId != null) {
            Pager(
                config = PagingConfig(pageSize = pageSize),
                pagingSourceFactory = { BookPagingSource(firebaseManager, userId, pageSize) }
            ).flow.cachedIn(viewModelScope)
        } else {
            // Return an empty flow if the user is not logged in.
            kotlinx.coroutines.flow.emptyFlow()
        }
}