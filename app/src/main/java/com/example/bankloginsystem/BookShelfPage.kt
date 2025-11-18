package com.example.bankloginsystem

import android.Manifest
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import com.example.bankloginsystem.ui.theme.ScanLines
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri

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

private fun createImageFile(context: Context): Uri {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
}

@Composable
fun BookShelfScreen(modifier: Modifier = Modifier, userId: String) {
    var searchQuery by remember { mutableStateOf("") }
    var deleteMode by remember { mutableStateOf(false) }
    var selectedBooks by remember { mutableStateOf(setOf<Int>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var bookToUpdate by remember { mutableStateOf<UserBook?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

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

    val imageUpdateHandler = { uri: Uri? ->
        if (uri != null) {
            bookToUpdate?.let { book ->
                coroutineScope.launch {
                    val success = dbHelper.updateBookCover(book.bookId, uri.toString())
                    if (success) {
                        Toast.makeText(context, "Cover image updated!", Toast.LENGTH_SHORT).show()
                        refreshBookList()
                    } else {
                        Toast.makeText(context, "Failed to update cover.", Toast.LENGTH_SHORT).show()
                    }
                }
                bookToUpdate = null
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent(), imageUpdateHandler)

    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUpdateHandler(tempImageUri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val newImageUri = createImageFile(context)
            tempImageUri = newImageUri
            cameraLauncher.launch(newImageUri)
        } else {
            Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_SHORT).show()
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
            text = { Text("Are you sure you want to delete ${selectedBooks.size} book(s)?") },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        selectedBooks.forEach { bookId -> dbHelper.deleteBookFromLibrary(bookId) }
                        refreshBookList()
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
                bookToUpdate = null
            },
            onCoverChangeClick = { showImageSourceDialog = true }
        )
    }

    if (showImageSourceDialog) {
        ImageSourceDialog(
            onDismiss = { showImageSourceDialog = false },
            onCameraClick = {
                showImageSourceDialog = false
                when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                    PackageManager.PERMISSION_GRANTED -> {
                        val newImageUri = createImageFile(context)
                        tempImageUri = newImageUri
                        cameraLauncher.launch(newImageUri)
                    }
                    else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onGalleryClick = {
                showImageSourceDialog = false
                galleryLauncher.launch("image/*")
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        ScanLines()
        Column(modifier = Modifier.fillMaxSize()) {
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
                    if (!deleteMode) selectedBooks = setOf()
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
                    BookItem(
                        book = book,
                        isSelected = book.id in selectedBooks,
                        deleteMode = deleteMode,
                        onSelect = {
                            selectedBooks = if (it in selectedBooks) {
                                selectedBooks - it
                            } else {
                                selectedBooks + it
                            }
                        },
                        onLongPress = { bookToUpdate = book }
                    )
                }
            }
        }
    }
}

@Composable
fun TopToolbar(
    deleteMode: Boolean,
    hasBooks: Boolean,
    booksSelected: Boolean,
    onAddClick: () -> Unit,
    onDeleteToggle: () -> Unit,
    onConfirmDelete: () -> Unit,
    onReturnClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (deleteMode) {
            Button(onClick = onConfirmDelete, enabled = booksSelected) {
                Text("Confirm Delete")
            }
            Button(onClick = onDeleteToggle) {
                Text("Cancel")
            }
        } else {
            Button(onClick = onAddClick) {
                Text("Add Book")
            }
            Button(onClick = onDeleteToggle, enabled = hasBooks) {
                Text("Delete Book(s)")
            }
            Button(onClick = onReturnClick) {
                Text("Return")
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search Books") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookItem(
    book: UserBook,
    isSelected: Boolean,
    deleteMode: Boolean,
    onSelect: (Int) -> Unit,
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(book.coverUri) {
        if (!book.coverUri.isNullOrEmpty()) {
            try {
                val uri = book.coverUri.toUri()
                val inputStream = context.contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
                bitmap = null
            }
        } else {
            bitmap = null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (deleteMode) {
                        onSelect(book.id)
                    } else {
                        onLongPress() // Open dialog on simple click
                    }
                },
                onLongClick = onLongPress // Also open on long click
            )
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = book.name,
                modifier = Modifier.size(60.dp, 90.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(60.dp, 90.dp)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text("No Image", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(text = book.name, fontWeight = FontWeight.Bold)
            Text(text = "by ${book.author}")
            Text(text = "Category: ${book.category}")
            Text(text = "Genre: ${book.genre}")
            book.status?.let { Text(text = "Status: $it") }
        }
    }
}

@Composable
fun UpdateBookDialog(
    book: UserBook,
    onDismiss: () -> Unit,
    onStatusChange: (String?) -> Unit,
    onCoverChangeClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val statuses = listOf("Currently Reading", "Completed", "Paused", "Dropped", "Plan to Read")

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
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ImageSourceDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Image Source") },
        text = { Text("Select a source for the book cover image.") },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onCameraClick) {
                    Text("Camera")
                }
                Button(onClick = onGalleryClick) {
                    Text("Gallery")
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
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
