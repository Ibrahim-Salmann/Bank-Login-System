package com.example.bankloginsystem

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * The BookShelfPage activity displays the user's personal collection of books.
 * It is the main hub for viewing, adding, updating, and deleting books.
 * It ensures that only a logged-in user can access this screen.
 */
class BookShelfPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Session validation: Ensures a user is logged in before showing the page.
        val userSessionManager = UserSessionManager(this)
        if (!userSessionManager.isLoggedIn()) {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Retrieve the logged-in user's ID to fetch their specific data.
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

/**
 * Creates a temporary, uniquely named image file in the app's private storage.
 * This is a required helper function for the modern `TakePicture` camera contract.
 * @param context The application context, used to access the file system.
 * @return A secure content URI for the newly created file, ready to be used by the camera app.
 */
private fun createImageFile(context: Context): Uri {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
    // The authority must match the one defined in the `provider` tag in AndroidManifest.xml.
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
}

/**
 * The main composable that builds the entire Book Shelf user interface.
 * It manages all the state for the screen, including the book list, search query, delete mode,
 * and dialogs for updating books and choosing image sources.
 * @param modifier Modifier for this composable.
 * @param userId The ID of the currently logged-in user, used for all database operations.
 */
@Composable
fun BookShelfScreen(modifier: Modifier = Modifier, userId: String) {
    // --- State Management ---
    var searchQuery by remember { mutableStateOf("") }
    var deleteMode by remember { mutableStateOf(false) } // Is the user currently deleting books?
    var selectedBooks by remember { mutableStateOf(setOf<Int>()) } // Which books are selected for deletion?
    var showDeleteConfirmation by remember { mutableStateOf(false) } // Show the delete confirmation dialog?
    var bookToUpdate by remember { mutableStateOf<UserBook?>(null) } // The book currently being edited.
    var showImageSourceDialog by remember { mutableStateOf(false) } // Show the Camera/Gallery choice dialog?

    // --- Core Components ---
    val context = LocalContext.current
    val dbHelper = remember { UserBooksDatabaseHelper(context) }
    var allBooks by remember { mutableStateOf<List<UserBook>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope() // For launching database operations off the main thread.

    /**
     * Reloads the user's book list from the database.
     * This function is called on initial load and after any add, update, or delete operation.
     */
    fun refreshBookList() {
        coroutineScope.launch {
            val id = userId.toIntOrNull()
            if (id != null) {
                allBooks = dbHelper.getBooksByUser(id)
            }
        }
    }

    // --- ActivityResult Launchers for Image Selection ---

    /**
     * A generic handler that takes a URI (from camera or gallery) and updates the book's cover in the database.
     */
    val imageUpdateHandler = { uri: Uri? ->
        if (uri != null) {
            bookToUpdate?.let { book ->
                coroutineScope.launch {
                    val success = dbHelper.updateBookCover(book.bookId, uri.toString())
                    if (success) {
                        Toast.makeText(context, "Cover image updated!", Toast.LENGTH_SHORT).show()
                        refreshBookList() // Refresh the list to show the new image.
                    } else {
                        Toast.makeText(context, "Failed to update cover.", Toast.LENGTH_SHORT).show()
                    }
                }
                bookToUpdate = null // Close the update dialog.
            }
        }
    }

    // Launcher for picking an image from the gallery.
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent(), imageUpdateHandler)

    // Launcher for the camera app. It receives a boolean indicating if the photo was successfully saved.
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUpdateHandler(tempImageUri) // If successful, pass the URI to our handler.
        }
    }

    // Launcher for requesting the CAMERA permission from the user.
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission was granted, now we can safely launch the camera.
            val newImageUri = createImageFile(context)
            tempImageUri = newImageUri
            cameraLauncher.launch(newImageUri)
        } else {
            Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Data Loading and UI Logic ---

    // Load the initial book list when the screen is first composed.
    LaunchedEffect(key1 = userId) {
        refreshBookList()
    }

    // Filter the displayed books based on the current search query.
    val filteredBooks = allBooks.filter {
        it.name.contains(searchQuery, true) ||
                it.author.contains(searchQuery, true) ||
                it.category.contains(searchQuery, true) ||
                it.genre.contains(searchQuery, true)
    }

    // --- Dialogs ---

    // Confirmation dialog before deleting books.
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${selectedBooks.size} book(s)?") },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        selectedBooks.forEach { bookId -> dbHelper.deleteBookFromLibrary(bookId) }
                        refreshBookList()
                        // Reset all delete-related states.
                        selectedBooks = setOf()
                        deleteMode = false
                        showDeleteConfirmation = false
                        Toast.makeText(context, "Book(s) deleted.", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Delete") }
            },
            dismissButton = { Button(onClick = { showDeleteConfirmation = false }) { Text("Cancel") } }
        )
    }

    // The main dialog for updating a book's status or cover.
    bookToUpdate?.let { book ->
        UpdateBookDialog(
            book = book,
            onDismiss = { bookToUpdate = null },
            onStatusChange = { newStatus ->
                coroutineScope.launch {
                    dbHelper.updateBookStatus(book.id, newStatus)
                    refreshBookList() // Refresh to show the new status.
                    Toast.makeText(context, "Status updated!", Toast.LENGTH_SHORT).show()
                }
                bookToUpdate = null // Close the dialog.
            },
            onCoverChangeClick = { showImageSourceDialog = true } // Open the next dialog.
        )
    }

    // The dialog for choosing between Camera and Gallery.
    if (showImageSourceDialog) {
        ImageSourceDialog(
            onDismiss = { showImageSourceDialog = false },
            onCameraClick = {
                showImageSourceDialog = false
                // Check for permission before launching the camera.
                when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                    PackageManager.PERMISSION_GRANTED -> {
                        val newImageUri = createImageFile(context)
                        tempImageUri = newImageUri
                        cameraLauncher.launch(newImageUri)
                    }
                    else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA) // Request permission.
                }
            },
            onGalleryClick = {
                showImageSourceDialog = false
                galleryLauncher.launch("image/*") // Launch gallery picker.
            }
        )
    }

    // --- Main UI Layout ---
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
                if (!deleteMode) selectedBooks = setOf() // Clear selections when exiting delete mode.
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
                            // In delete mode, clicking a book toggles its selection.
                            selectedBooks = if (book.id in selectedBooks) selectedBooks - book.id
                            else selectedBooks + book.id
                        } else {
                            // In normal mode, clicking a book opens the update dialog.
                            bookToUpdate = book
                        }
                    }
                )
            }
        }
    }
}

/**
 * The top toolbar that provides primary actions like adding, deleting, and returning.
 * Its appearance changes based on whether the user is in `deleteMode`.
 */
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
            // Delete mode UI
            Button(onClick = onDeleteToggle) { Text("Cancel") }
            Button(onClick = onConfirmDelete, enabled = booksSelected) { Text("Delete Selected") }
        } else {
            // Normal mode UI
            Button(onClick = onAddClick) { Text("Add Book") }
            Button(onClick = onDeleteToggle, enabled = hasBooks) { Text("Delete") }
            Button(onClick = onReturnClick) { Text("Return") }
        }
    }
}

/**
 * A simple search bar for filtering the book list.
 */
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

/**
 * A single row in the book list. Displays the book's cover, details, and selection state.
 */
@SuppressLint("UseKtx")
@Composable
fun BookRow(book: UserBook, deleteMode: Boolean, isSelected: Boolean, onBookClick: () -> Unit) {
    // Highlight the row if it's selected for deletion.
    val backgroundColor = if (isSelected) Color(0xFFD32F2F) else Color(0xFF0B3954)
    val context = LocalContext.current

    // This state holds the loaded Bitmap for the book cover.
    var bookCoverBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // This effect loads the cover image from its URI asynchronously.
    // It re-runs whenever the `book.coverUri` changes.
    LaunchedEffect(book.coverUri) {
        if (book.coverUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(Uri.parse(book.coverUri))
                bookCoverBitmap = BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
                bookCoverBitmap = null // Failed to load, clear the bitmap.
            }
        } else {
            bookCoverBitmap = null // Clear bitmap if URI is null
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp).background(backgroundColor)
            .clickable(onClick = onBookClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Display the loaded image, or a placeholder if it's null.
        bookCoverBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Book Cover",
                modifier = Modifier.size(60.dp, 90.dp)
            )
        } ?: Box(modifier = Modifier.size(60.dp, 90.dp), contentAlignment = Alignment.Center) {
            Text("📘", fontSize = MaterialTheme.typography.headlineSmall.fontSize)
        }

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text(book.name, color = Color.White, fontWeight = FontWeight.Bold)
            Text("By ${book.author}", color = Color.LightGray)
            Text("${book.category} • ${book.genre}", color = Color.Gray)
            book.status?.let {
                Text("Status: $it", color = Color(0xFF00BCD4), style = MaterialTheme.typography.bodySmall)
            }
        }
        // Show a checkbox in delete mode.
        if (deleteMode) {
            Checkbox(checked = isSelected, onCheckedChange = { onBookClick() })
        }
    }
}

/**
 * The dialog for updating a book's status or initiating a cover change.
 */
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
                        // Option to clear the status.
                        DropdownMenuItem(text = { Text("Clear Status") }, onClick = { onStatusChange(null); expanded = false })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onCoverChangeClick) { Text("Change Cover Image") }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } }
    )
}

/**
 * A simple dialog that presents the user with a choice between Camera and Gallery.
 */
@Composable
fun ImageSourceDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Cover Image") },
        text = { Text("Choose an image source:") },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = onCameraClick) { Text("Camera") }
                Button(onClick = onGalleryClick) { Text("Gallery") }
            }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}


@Preview(showBackground = true)
@Composable
fun BookShelfScreenPreview() {
    BankLoginSystemTheme {
        BookShelfScreen(userId = "1")
    }
}
