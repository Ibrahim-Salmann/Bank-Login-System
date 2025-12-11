package com.example.bankloginsystem

import androidx.paging.PagingSource
import androidx.paging.PagingState

class BookPagingSource(
    private val firebaseManager: FirebaseManager,
    private val userId: String,
    private val pageSize: Int
) : PagingSource<String, Book>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Book> {
        return try {
            val currentPageKey = params.key
            val (books, nextKey) = firebaseManager.getBooksPage(
                userId = userId,
                pageSize = pageSize,
                startKey = currentPageKey
            )

            LoadResult.Page(
                data = books,
                prevKey = null, // Only paging forward
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Book>): String? {
        // This is used to determine which page to load when the data is refreshed.
        // We can use the anchorPosition to find the closest page to the last accessed position.
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
                ?: state.closestPageToPosition(anchorPosition)?.nextKey
        }
    }
}