package com.example.bankloginsystem

import android.net.Uri
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

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
     * It sets up a continuous listener, so it will automatically update the UI on any data change.
     *
     * @param userId The ID of the user.
     * @param onComplete A callback that provides a list of `Book` objects.
     */
    fun getBooks(userId: String, onComplete: (List<Book>) -> Unit) {
        database.child("books").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val books = mutableListOf<Book>()
                for (bookSnapshot in snapshot.children) {
                    val book = bookSnapshot.getValue(Book::class.java)
                    // Manually add the Firebase key to each book object for future reference.
                    book?.firebaseKey = bookSnapshot.key
                    book?.let { books.add(it) }
                }
                onComplete(books)
            }

            override fun onCancelled(error: DatabaseError) {
                // In a production app, you should log this error or show a message to the user.
            }
        })
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
 */
data class User(val fullName: String, val email: String, var balance: Double = 0.0)

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
