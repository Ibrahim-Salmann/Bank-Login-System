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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
 * It ensures that the user is logged in before showing any content.
 */
class BookShelfPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Redirect to login if the user is not authenticated.
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
 * It handles fetching books from both local and remote databases, searching, and user interactions.
 */
@Composable
fun BookShelfScreen(modifier: Modifier = Modifier) {
    var searchQuery by remember { mutableStateOf("") }
    var allBooks by remember { mutableStateOf<List<UserBook>>(emptyList()) }
    val firebaseManager = remember { FirebaseManager() }
    val firebaseAuth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val dbHelper = remember { UserBooksDatabaseHelper(context) }
    val userSessionManager = remember { UserSessionManager(context) }

    // This effect fetches books from both the local SQLite database and Firebase when the screen loads.
    LaunchedEffect(key1 = Unit) {
        // First, load books from the local database for a fast initial display.
        val localUserId = userSessionManager.getUserDetails()[UserSessionManager.PREF_USER_ID]?.toIntOrNull()
        if (localUserId != null) {
            allBooks = dbHelper.getBooksByUser(localUserId)
        }

        // Then, fetch the latest data from Firebase to ensure the bookshelf is up-to-date.
        val firebaseUserId = firebaseAuth.currentUser?.uid
        if (firebaseUserId != null) {
            firebaseManager.getBooks(firebaseUserId) { firebaseBooks ->
                // A simple sync strategy: merge Firebase data with local data.
                val userBooks = firebaseBooks.map { book ->
                    val localBook = allBooks.find { it.firebaseKey == book.firebaseKey }
                    UserBook(
                        id = localBook?.id ?: 0, // Keep local DB ID
                        userId = localUserId ?: 0,
                        bookId = localBook?.bookId ?: 0,
                        name = book.name,
                        author = book.author,
                        category = book.category,
                        genre = book.genre,
                        coverUri = book.coverUri,
                        status = book.status,
                        firebaseKey = book.firebaseKey
                    )
                }
                allBooks = userBooks
            }
        }
    }

    // Filter the books based on the user's search query.
    val filteredBooks = allBooks.filter {
        it.name.contains(searchQuery, true) ||
                it.author.contains(searchQuery, true) ||
                it.category.contains(searchQuery, true) ||
                it.genre.contains(searchQuery, true)
    }

    Box(modifier = modifier.fillMaxSize()) {
        ScanLines()
        Column(modifier = Modifier.fillMaxSize()) {
            TopToolbar(
                onAddClick = { context.startActivity(Intent(context, AddBookPage::class.java)) },
                onReturnClick = {
                    context.startActivity(Intent(context, WelcomePage::class.java))
                    (context as? Activity)?.finish()
                }
            )

            SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })

            LazyColumn {
                items(filteredBooks, key = { it.id }) { book ->
                    BookItem(
                        book = book,
                        onUpdateStatus = { newStatus ->
                            val updatedBook = book.copy(status = newStatus)
                            val firebaseUserId = firebaseAuth.currentUser?.uid
                            if (firebaseUserId != null && book.firebaseKey != null) {
                                // Update Firebase first
                                firebaseManager.updateBook(firebaseUserId, updatedBook.toBook()) { success ->
                                    if (success) {
                                        // Then update local database
                                        dbHelper.updateBookStatus(book.id, newStatus)
                                        allBooks = allBooks.map { if (it.id == book.id) updatedBook else it }
                                        Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to update status in Firebase", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onDelete = {
                            val firebaseUserId = firebaseAuth.currentUser?.uid
                            if (firebaseUserId != null && book.firebaseKey != null) {
                                // Delete from Firebase first
                                firebaseManager.deleteBook(firebaseUserId, book.firebaseKey) { success ->
                                    if (success) {
                                        // Then delete from local database
                                        dbHelper.deleteBookFromLibrary(book.id)
                                        allBooks = allBooks.filter { it.id != book.id }
                                        Toast.makeText(context, "Book deleted", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to delete book from Firebase", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onUpdateCover = { newCoverUri ->
                            val firebaseUserId = firebaseAuth.currentUser?.uid
                            if (firebaseUserId != null && book.firebaseKey != null) {
                                // 1. Upload new image to Firebase Storage
                                firebaseManager.uploadImage(newCoverUri) { downloadUrl ->
                                    if (downloadUrl != null) {
                                        val updatedBook = book.copy(coverUri = downloadUrl)
                                        // 2. Update the book record in Firebase Realtime Database
                                        firebaseManager.updateBook(firebaseUserId, updatedBook.toBook()) { success ->
                                            if (success) {
                                                // 3. Update the book record in the local SQLite database
                                                dbHelper.updateBookCover(book.bookId, downloadUrl)
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
 * Extension function to convert a `UserBook` (local model) to a `Book` (Firebase model).
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

@Composable
fun TopToolbar(onAddClick: () -> Unit, onReturnClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onAddClick) { Text("Add Book") }
        Button(onClick = onReturnClick) { Text("Return") }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(value = query, onValueChange = onQueryChange, label = { Text("Search Books") }, modifier = Modifier.fillMaxWidth().padding(8.dp))
}

/**
 * Displays a single book item in the list.
 * It shows the book's details, cover image, and provides options for interaction.
 */
@Composable
fun BookItem(
    book: UserBook,
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

    // This effect loads the book's cover image from a URL when the URI changes.
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
                    bitmap = null
                }
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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

        Column(modifier = Modifier.weight(1f)) {
            Text(text = book.name, fontWeight = FontWeight.Bold)
            Text(text = "by ${book.author}")
            Text(text = "Category: ${book.category}")
            Text(text = "Genre: ${book.genre}")
            book.status?.let { Text(text = "Status: $it") }
        }

        // --- ACTION BUTTONS ---
        Column {
            // Button to change the book's reading status.
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
            // Button to delete the book from the user's library.
            Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookShelfScreenPreview() {
    BankLoginSystemTheme {
        BookShelfScreen()
    }
}
