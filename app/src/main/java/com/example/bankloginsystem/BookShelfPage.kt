package com.example.bankloginsystem

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme
import com.example.bankloginsystem.ui.theme.ScanLines
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * The BookShelfPage activity displays the user's personal collection of books.
 * It ensures that the user is logged in before showing any content by checking the [UserSessionManager].
 * If the user is not logged in, it redirects them to the [LoginPage].
 */
class BookShelfPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userSessionManager = UserSessionManager(this)
        if (!userSessionManager.isLoggedIn()) {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContent {
            BankLoginSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    BookShelfScreen(modifier = Modifier.padding(padding))
                }
            }
        }
    }
}

/**
 * The main screen for displaying the user's book collection.
 * It handles fetching books from both the local SQLite database and Firebase Realtime Database,
 * searching and filtering the book list, and handling user interactions like updating book status,
 * deleting books, and updating cover images.
 *
 * @param modifier Modifier for this composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookShelfScreen(modifier: Modifier = Modifier) {
    val isInPreview = LocalInspectionMode.current
    var searchQuery by remember { mutableStateOf("") }
    var allBooks by remember { mutableStateOf<List<UserBook>>(emptyList()) }
    val firebaseManager = remember { if(!isInPreview) FirebaseManager() else null }
    val firebaseAuth = remember { if(!isInPreview) FirebaseAuth.getInstance() else null }
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val userSessionManager = remember { UserSessionManager(context) }
    var isDeleteMode by remember { mutableStateOf(false) }

    // This effect fetches books from both the local SQLite database and Firebase when the screen loads.
    if (!isInPreview) {
        LaunchedEffect(key1 = Unit) {
            // First, load books from the local database for a fast initial display.
            val localUserId = userSessionManager.getUserDetails()[UserSessionManager.PREF_USER_ID]?.toIntOrNull()
            if (localUserId != null) {
                allBooks = dbHelper.getBooksByUser(localUserId)
            }

            // Then, fetch the latest data from Firebase to ensure the bookshelf is up-to-date.
            val firebaseUserId = firebaseAuth?.currentUser?.uid
            if (firebaseUserId != null) {
                firebaseManager?.getBooks(firebaseUserId) { firebaseBooks ->
                    // A simple sync strategy: merge Firebase data with local data.
                    // This ensures that books from both sources are displayed.
                    // TODO: Implement a more robust data synchronization strategy to handle conflicts.
                    val localBooks = allBooks
                    val mergedBooks = (localBooks + firebaseBooks.map { book ->
                        UserBook(
                            id = 0, // This will be updated later if the book is not in the local database
                            userId = localUserId ?: 0,
                            bookId = 0, // This will be updated later if the book is not in the local database
                            name = book.name,
                            author = book.author,
                            category = book.category,
                            genre = book.genre,
                            coverUri = book.coverUri,
                            status = book.status,
                            firebaseKey = book.firebaseKey
                        )
                    }).distinctBy { it.firebaseKey }
                    allBooks = mergedBooks
                }
            }
        }
    }

    // Filter the books based on the user's search query in real-time.
    val filteredBooks = allBooks.filter {
        it.name.contains(searchQuery, true) ||
                it.author.contains(searchQuery, true) ||
                it.category.contains(searchQuery, true) ||
                it.genre.contains(searchQuery, true)
    }

    Box(modifier = modifier.fillMaxSize()) {
        ScanLines()
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar with actions for deleting books, adding a new book, and returning to the welcome screen.
            TopAppBar(
                title = { Text("My Bookshelf") },
                actions = {
                    IconButton(onClick = { isDeleteMode = !isDeleteMode }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Books", tint = if (isDeleteMode) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                    Button(onClick = { context.startActivity(Intent(context, AddBookPage::class.java)) }) { Text("Add Book") }
                    Button(onClick = { context.startActivity(Intent(context, WelcomePage::class.java)); (context as? Activity)?.finish() }) { Text("Return") }
                }
            )

            // Search bar to filter the list of books.
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Books") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )

            // Display the list of books in a LazyColumn for performance.
            LazyColumn {
                items(filteredBooks, key = { it.id }) { book ->
                    BookItem(
                        book = book,
                        isDeleteMode = isDeleteMode,
                        onUpdateStatus = { newStatus ->
                            val updatedBook = book.copy(status = newStatus)
                            val firebaseUserId = firebaseAuth?.currentUser?.uid
                            if (firebaseUserId != null && book.firebaseKey != null) {
                                // First, update the book in Firebase.
                                firebaseManager?.updateBook(firebaseUserId, updatedBook.toBook()) { success ->
                                    if (success) {
                                        // If the Firebase update is successful, update the local SQLite database.
                                        dbHelper.updateBookStatus(book.id, newStatus)
                                        // Update the local list to reflect the change immediately.
                                        allBooks = allBooks.map { if (it.id == book.id) updatedBook else it }
                                        Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to update status in Firebase", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onDelete = {
                            val firebaseUserId = firebaseAuth?.currentUser?.uid
                            if (firebaseUserId != null && book.firebaseKey != null) {
                                // First, delete the book from Firebase.
                                firebaseManager?.deleteBook(firebaseUserId, book.firebaseKey) { success ->
                                    if (success) {
                                        // If the Firebase deletion is successful, delete the book from the local SQLite database.
                                        dbHelper.deleteBookFromLibrary(book.id)
                                        // Update the local list to remove the book immediately.
                                        allBooks = allBooks.filter { it.id != book.id }
                                        Toast.makeText(context, "Book deleted", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to delete book from Firebase", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onUpdateCover = { newCoverUri ->
                            val firebaseUserId = firebaseAuth?.currentUser?.uid
                            if (firebaseUserId != null && book.firebaseKey != null) {
                                // 1. Upload new image to Firebase Storage.
                                firebaseManager?.uploadImage(newCoverUri) { downloadUrl ->
                                    if (downloadUrl != null) {
                                        val updatedBook = book.copy(coverUri = downloadUrl)
                                        // 2. Update the book record in Firebase Realtime Database.
                                        firebaseManager.updateBook(firebaseUserId, updatedBook.toBook()) { success ->
                                            if (success) {
                                                // 3. Update the book record in the local SQLite database.
                                                dbHelper.updateBookCover(book.bookId, downloadUrl)
                                                // Update the local list to reflect the change immediately.
                                                allBooks = allBooks.map { if (it.id == book.id) updatedBook else it }
                                                Toast.makeText(context, "Cover updated", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to update cover in Firebase", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to upload new cover", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Extension function to convert a `UserBook` (local data model) to a `Book` (Firebase data model).
 * This is useful for keeping the data models separate and converting between them when communicating with Firebase.
 * @return A [Book] object.
 */
fun UserBook.toBook(): Book {
    return Book(
        name = this.name,
        author = this.author,
        category = this.category,
        genre = this.genre,
        coverUri = this.coverUri,
        status = this.status,
        firebaseKey = this.firebaseKey
    )
}

/**
 * Displays a single book item in the list.
 * It shows the book's details, cover image, and provides options for interaction like
 * updating the reading status, deleting the book, or updating the cover image.
 *
 * @param book The [UserBook] to display.
 * @param isDeleteMode A flag to indicate if the delete mode is active.
 * @param onUpdateStatus A callback function to be invoked when the user updates the book's status.
 * @param onDelete A callback function to be invoked when the user deletes the book.
 * @param onUpdateCover A callback function to be invoked when the user updates the book's cover.
 */
@Composable
fun BookItem(
    book: UserBook,
    isDeleteMode: Boolean,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit,
    onUpdateCover: (Uri) -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var statusExpanded by remember { mutableStateOf(false) }
    val statuses = listOf("Currently Reading", "Completed", "Paused", "Dropped", "Plan to Read")

    // Launcher for the gallery to pick a new cover image.
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onUpdateCover(uri)
        }
    }

    // This effect loads the book's cover image from a URL when the coverUri changes.
    // It runs in a background thread to avoid blocking the UI.
    LaunchedEffect(book.coverUri) {
        if (!book.coverUri.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val url = URL(book.coverUri)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val input: InputStream = connection.inputStream
                    bitmap = BitmapFactory.decodeStream(input)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If image loading fails, bitmap is set to null, and "No Image" will be shown.
                    bitmap = null
                }
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        // The book cover image. It's clickable to allow the user to update the cover.
        Box(modifier = Modifier.clickable { galleryLauncher.launch("image/*") }) {
            if (bitmap != null) {
                Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = book.name, modifier = Modifier.size(60.dp, 90.dp))
            } else {
                Box(modifier = Modifier.size(60.dp, 90.dp).background(Color.DarkGray), contentAlignment = Alignment.Center) {
                    Text("No Image", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Column for book details: title, author, category, genre, and status.
        Column(modifier = Modifier.weight(1f)) {
            Text(text = book.name, fontWeight = FontWeight.Bold)
            Text(text = "by ${book.author}")
            Text(text = "Category: ${book.category}")
            Text(text = "Genre: ${book.genre}")
            book.status?.let { Text(text = "Status: $it") }
        }

        // --- ACTION BUTTONS ---
        // If delete mode is active, show a delete button.
        if (isDeleteMode) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Book", tint = MaterialTheme.colorScheme.error)
            }
        } else {
            // Otherwise, show a button to update the book's status.
            Column {
                Box {
                    Button(onClick = { statusExpanded = true }) { Text("Status") }
                    DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        statuses.forEach { status ->
                            DropdownMenuItem(text = { Text(status) }, onClick = {
                                onUpdateStatus(status)
                                statusExpanded = false
                            })
                        }
                    }
                }
            }
        }
    }
}

/**
 * A preview function to render the `BookShelfScreen` in Android Studio's design view.
 * It's wrapped in the app's theme to ensure consistent styling.
 */
@Preview(showBackground = true)
@Composable
fun BookShelfScreenPreview() {
    BankLoginSystemTheme {
        BookShelfScreen()
    }
}
