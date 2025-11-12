package com.example.bankloginsystem

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch

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
        val userDetails = userSessionManager.getUserDetails()
        val userId = userDetails[UserSessionManager.PREF_USER_ID] ?: ""
        setContent {
            BankLoginSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    BookShelfScreen(
                        modifier = Modifier.padding(padding),
                        userId = userId
                    )
                }
            }
        }
    }
}

@Composable
fun BookShelfScreen(modifier: Modifier = Modifier, userId: String) {
    var searchQuery by remember { mutableStateOf("") }
    var deleteMode by remember { mutableStateOf(false) }
    var selectedBooks by remember { mutableStateOf(setOf<Int>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    // --- IMPLEMENTATION: State to hold the book being updated ---
    var bookToUpdate by remember { mutableStateOf<UserBook?>(null) }

    val context = LocalContext.current
    val dbHelper = remember { UserBooksDatabaseHelper(context) }
    var allBooks by remember { mutableStateOf<List<UserBook>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    fun refreshBookList() {
        coroutineScope.launch {
            val id = userId.toIntOrNull()
            if (id != null) {
                allBooks = dbHelper.getBooksByUser(id)
            }
        }
    }

    // --- IMPLEMENTATION: Launcher for selecting a new cover image ---
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { newCoverUri ->
            bookToUpdate?.let { book ->
                coroutineScope.launch {
                    val success = dbHelper.updateBookCover(book.bookId, newCoverUri.toString())
                    if (success) {
                        Toast.makeText(context, "Cover image updated!", Toast.LENGTH_SHORT).show()
                        refreshBookList()
                    } else {
                        Toast.makeText(context, "Failed to update cover.", Toast.LENGTH_SHORT).show()
                    }
                }
                bookToUpdate = null // Close dialog
            }
        }
    }

    LaunchedEffect(key1 = userId) {
        refreshBookList()
    }

    val filteredBooks = allBooks.filter {
        it.name.contains(searchQuery, true) ||
                it.author.contains(searchQuery, true) ||
                it.category.contains(searchQuery, true) ||
                it.genre.contains(searchQuery, true)
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${selectedBooks.size} book(s)? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            selectedBooks.forEach { bookId ->
                                dbHelper.deleteBookFromLibrary(bookId)
                            }
                            refreshBookList()
                            selectedBooks = setOf()
                            deleteMode = false
                            showDeleteConfirmation = false
                            Toast.makeText(context, "Book(s) deleted.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    // --- IMPLEMENTATION: Show the Update Book Dialog ---
    bookToUpdate?.let { book ->
        UpdateBookDialog(
            book = book,
            onDismiss = { bookToUpdate = null },
            onStatusChange = { newStatus ->
                coroutineScope.launch {
                    dbHelper.updateBookStatus(book.id, newStatus)
                    refreshBookList()
                    Toast.makeText(context, "Status updated!", Toast.LENGTH_SHORT).show()
                }
                bookToUpdate = null // Close dialog
            },
            onCoverChangeClick = {
                galleryLauncher.launch("image/*")
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopToolbar(
            deleteMode = deleteMode,
            hasBooks = allBooks.isNotEmpty(),
            booksSelected = selectedBooks.isNotEmpty(),
            onAddClick = {
                val intent = Intent(context, AddBookPage::class.java)
                context.startActivity(intent)
            },
            onDeleteToggle = {
                deleteMode = !deleteMode
                if (!deleteMode) {
                    selectedBooks = setOf() 
                }
            },
            onConfirmDelete = { showDeleteConfirmation = true },
            onReturnClick = {
                val intent = Intent(context, WelcomePage::class.java)
                context.startActivity(intent)
                (context as? Activity)?.finish()
            }
        )

        SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })

        LazyColumn {
            items(filteredBooks, key = { it.id }) { book ->
                BookRow(
                    book = book,
                    deleteMode = deleteMode,
                    isSelected = book.id in selectedBooks,
                    onBookClick = {
                        if (deleteMode) {
                            selectedBooks = if (book.id in selectedBooks) {
                                selectedBooks - book.id
                            } else {
                                selectedBooks + book.id
                            }
                        } else {
                            // --- IMPLEMENTATION: Set the book to be updated, which triggers the dialog ---
                            bookToUpdate = book
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TopToolbar(
    deleteMode: Boolean, hasBooks: Boolean, booksSelected: Boolean,
    onAddClick: () -> Unit, onDeleteToggle: () -> Unit, onConfirmDelete: () -> Unit, onReturnClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF063041)).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if (deleteMode) {
            Button(onClick = onDeleteToggle) { Text("Cancel") }
            Button(onClick = onConfirmDelete, enabled = booksSelected) { Text("Delete Selected") }
        } else {
            Button(onClick = onAddClick) { Text("Add Book") }
            Button(onClick = onDeleteToggle, enabled = hasBooks) { Text("Delete") }
            Button(onClick = onReturnClick) { Text("Return") }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth().padding(12.dp),
        leadingIcon = { Text("🔍") }, placeholder = { Text("Search books...") },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color(0xFFEEEEEE), focusedContainerColor = Color(0xFFFFFFFF)
        )
    )
}

@Composable
fun BookRow(book: UserBook, deleteMode: Boolean, isSelected: Boolean, onBookClick: () -> Unit) {
    val backgroundColor = if (isSelected) Color(0xFFD32F2F) else Color(0xFF0B3954)
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp).background(backgroundColor)
            .clickable(onClick = onBookClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(book.name, color = Color.White, fontWeight = FontWeight.Bold)
            Text("By ${book.author}", color = Color.LightGray)
            Text("${book.category} • ${book.genre}", color = Color.Gray)
            // --- IMPLEMENTATION: Display the book's status ---
            book.status?.let {
                Text("Status: $it", color = Color(0xFF00BCD4), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (deleteMode) {
            Checkbox(checked = isSelected, onCheckedChange = { onBookClick() })
        }
        Text("📘", fontSize = MaterialTheme.typography.headlineSmall.fontSize)
    }
}

// --- IMPLEMENTATION: A dialog for updating book status and cover ---
@Composable
fun UpdateBookDialog(
    book: UserBook,
    onDismiss: () -> Unit,
    onStatusChange: (String?) -> Unit,
    onCoverChangeClick: () -> Unit
) {
    val statuses = listOf("Reading", "Completed", "On Hold", "Dropped", "Plan to Read")
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(book.name) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Current status: ${book.status ?: "None"}")
                Spacer(modifier = Modifier.height(16.dp))

                Box {
                    Button(onClick = { expanded = true }) { Text("Change Status") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        statuses.forEach { status ->
                            DropdownMenuItem(text = { Text(status) }, onClick = { onStatusChange(status); expanded = false })
                        }
                        DropdownMenuItem(text = { Text("Clear Status") }, onClick = { onStatusChange(null); expanded = false })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onCoverChangeClick) { Text("Change Cover Image") }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun BookShelfScreenPreview() {
    BankLoginSystemTheme {
        BookShelfScreen(userId = "1")
    }
}
