package com.example.bankloginsystem

import android.net.Uri
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import com.google.firebase.database.Query
import kotlinx.coroutines.tasks.await

/**
 * A singleton-style manager for handling all interactions with Firebase services.
 * This class abstracts the complexities of Firebase Realtime Database and Cloud Storage,
 * providing simple, asynchronous methods for the rest of the application.
 *
 * It manages:
 * - User data persistence in Realtime Database.
 * - Book data CRUD operations for each user.
 * - Uploading and retrieving cover images from Cloud Storage.
 */
class FirebaseManager {
    // Get a reference to the root of the Firebase Realtime Database.
    private val database = FirebaseDatabase.getInstance().reference
    // Get a reference to the root of the Firebase Cloud Storage.
    private val storage = FirebaseStorage.getInstance().reference

    /**
     * Saves basic user information to the Realtime Database under the `/users` path.
     * This is typically called once during user registration.
     *
     * @param userId The unique ID of the user (usually from Firebase Auth).
     * @param fullName The user's full name.
     * @param email The user's email address.
     */
    fun saveUser(userId: String, fullName: String, email: String) {
        val user = User(fullName, email)
        database.child("users").child(userId).setValue(user)
    }

    /**
     * Retrieves a user's profile from the Firebase Realtime Database.
     *
     * @param userId The ID of the user to fetch.
     * @param onComplete A callback that provides the `User` object or `null` if not found.
     */
    fun getUser(userId: String, onComplete: (User?) -> Unit) {
        database.child("users").child(userId).get()
            .addOnSuccessListener {
                onComplete(it.getValue(User::class.java))
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    /**
     * Uploads an image to Firebase Cloud Storage in the `/images` directory.
     *
     * @param imageUri The local URI of the image to upload.
     * @param onComplete A callback that provides the public download URL of the uploaded image
     *                   or `null` if the upload failed.
     */
    fun uploadImage(imageUri: Uri, onComplete: (String?) -> Unit) {
        // Generate a unique file name to prevent overwrites.
        val fileName = UUID.randomUUID().toString()
        val imageRef = storage.child("images/$fileName")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                // If the upload is successful, get the public download URL.
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    onComplete(uri.toString())
                }
            }
            .addOnFailureListener {
                // If the upload fails, return null.
                onComplete(null)
            }
    }

    /**
     * Saves a new book to a user's library in the Realtime Database.
     * It creates a unique entry under `/books/{userId}`.
     *
     * @param userId The ID of the user.
     * @param book The `Book` object to save.
     * @param onComplete A callback that provides the unique key of the newly created book entry
     *                   or `null` if the save operation failed.
     */
    fun saveBook(userId: String, book: Book, onComplete: (String?) -> Unit) {
        // `push()` creates a unique, chronological key for the new entry.
        val bookRef = database.child("books").child(userId).push()
        bookRef.setValue(book)
            .addOnSuccessListener { onComplete(bookRef.key) } // Return the key on success.
            .addOnFailureListener { onComplete(null) }
    }

    /**
     * Retrieves all books for a specific user from the Realtime Database.
     * This method attaches a `ValueEventListener` which provides a real-time subscription to the data.
     * The `onDataChange` callback will be triggered once with the initial state of the data, and then
     * again every time the data at this location changes in the database.
     *
     * @param userId The ID of the user whose books are to be fetched.
     * @param onComplete A callback function that will be invoked with the complete list of `Book` objects.
     *                   This callback will be called every time the data updates.
     */
    fun getBooks(userId: String, onComplete: (List<Book>) -> Unit) {
        database.child("books").child(userId).addValueEventListener(object : ValueEventListener {
            /**
             * This method is called once with the initial value and again
             * whenever data at this location is updated.
             *
             * @param snapshot A `DataSnapshot` instance containing the data at the specified
             *                 database reference. You can extract the data from this snapshot.
             *                 The snapshot represents the complete data at `/books/{userId}`.
             */
            override fun onDataChange(snapshot: DataSnapshot) {
                val books = mutableListOf<Book>()
                // The `snapshot.children` property returns an iterable of all the direct
                // children at this location. In this case, each child is a book.
                for (bookSnapshot in snapshot.children) {
                    // `getValue(Book::class.java)` automatically deserializes the child snapshot
                    // into a `Book` data class object. Firebase matches the keys in the snapshot
                    // to the property names in your data class.
                    val book = bookSnapshot.getValue(Book::class.java)

                    // The `bookSnapshot.key` gives you the unique key for the book in Firebase
                    // (e.g., "-Nq..."). We manually add this to our `Book` object so we can
                    // reference it later for updates or deletions.
                    book?.firebaseKey = bookSnapshot.key
                    book?.let { books.add(it) }
                }
                // Finally, we pass the fully parsed list of books to the `onComplete` callback.
                // Since this is a real-time listener, this code will execute every time a book is
                // added, updated, or removed for this user in Firebase.
                onComplete(books)
            }

            /**
             * This method will be called in the event that the client does not have permission
             * to read the data at the specified location.
             *
             * @param error A `DatabaseError` object containing details about the failure.
             */
            override fun onCancelled(error: DatabaseError) {
                // In a production app, you should implement proper error handling,
                // such as logging the error or displaying a message to the user.
                onComplete(emptyList()) // You might want to return an empty list or handle the error differently.
            }
        })
    }
    
    /**
     * Fetches a single page of books for a user from the Realtime Database for pagination.
     *
     * @param userId The ID of the user whose books are to be fetched.
     * @param pageSize The number of books to retrieve for the page.
     * @param startKey The Firebase key of the item to start after. For the first page, this is null.
     * @return A Pair containing the list of books for the page and the key for the next page.
     */
    suspend fun getBooksPage(userId: String, pageSize: Int, startKey: String?): Pair<List<Book>, String?> {
        val query: Query = database.child("books").child(userId).orderByKey()

        val finalQuery = if (startKey == null) {
            query.limitToFirst(pageSize + 1)
        } else {
            // The +1 is to fetch one extra item, which tells us if there's a next page.
            query.startAt(startKey).limitToFirst(pageSize + 1)
        }

        val snapshot = finalQuery.get().await()

        val books = snapshot.children.mapNotNull { dataSnapshot ->
            dataSnapshot.getValue(Book::class.java)?.apply {
                firebaseKey = dataSnapshot.key
            }
        }

        // The query with startAt is inclusive. If it's not the first page, we need to drop the first item,
        // which is the last item from the previous page.
        val pageData = if (startKey != null && books.isNotEmpty()) {
            books.drop(1)
        } else {
            books
        }

        val nextKey = if (pageData.size > pageSize) {
            // We fetched one extra item. The key of this extra item is the starting point for the next page.
            pageData.last().firebaseKey
        } else {
            null
        }

        // Return the list without the extra item used for determining the next key.
        return Pair(pageData.take(pageSize), nextKey)
    }


    /**
     * Updates an existing book in a user's library.
     *
     * @param userId The ID of the user.
     * @param book The `Book` object with updated information. The `firebaseKey` must not be null.
     * @param onComplete A callback indicating whether the update was successful.
     */
    fun updateBook(userId: String, book: Book, onComplete: (Boolean) -> Unit) {
        if (book.firebaseKey == null) {
            onComplete(false)
            return
        }
        database.child("books").child(userId).child(book.firebaseKey!!).setValue(book)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    /**
     * Deletes a book from a user's library in the Realtime Database.
     *
     * @param userId The ID of the user.
     * @param firebaseKey The unique key of the book entry to delete.
     * @param onComplete A callback indicating whether the deletion was successful.
     */
    fun deleteBook(userId: String, firebaseKey: String, onComplete: (Boolean) -> Unit) {
        database.child("books").child(userId).child(firebaseKey).removeValue()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    /**
     * Updates the user's balance in the Realtime Database.
     *
     * @param userId The ID of the user.
     * @param newBalance The new balance to set.
     * @param onComplete A callback indicating whether the update was successful.
     */
    fun updateBalance(userId: String, newBalance: Double, onComplete: (Boolean) -> Unit) {
        database.child("users").child(userId).child("balance").setValue(newBalance)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    /**
     * Retrieves the user's balance from the Realtime Database.
     *
     * @param userId The ID of the user.
     * @param onComplete A callback that provides the user's balance or `null` if it cannot be retrieved.
     */
    fun getUserBalance(userId: String, onComplete: (Double?) -> Unit) {
        database.child("users").child(userId).child("balance").get()
            .addOnSuccessListener { onComplete(it.getValue(Double::class.java)) }
            .addOnFailureListener { onComplete(null) }
    }
}

/**
 * A simple data class to represent a user in Firebase.
 * All fields have default values to allow for safe deserialization from Firebase.
 */
data class User(val fullName: String = "", val email: String = "", var balance: Double = 0.0)

/**
 * Represents a book in the Firebase database.
 * All fields are declared with default values to allow Firebase to deserialize the data
 * even if some fields are missing from the database snapshot.
 */
data class Book(
    var name: String = "",
    var author: String = "",
    var category: String = "",
    var genre: String = "",
    var coverUri: String? = null,
    var status: String? = null,
    var firebaseKey: String? = null // This will be null when creating, but populated when read.
)
