@file:Suppress("UNCHECKED_CAST")

package com.example.bankloginsystem

import  android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme
import com.example.bankloginsystem.ui.theme.ScanLines
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The AddBookPage activity provides a user interface for adding a new book to the user's library.
 * It includes fields for book details, category and genre selection, and options to add a cover image
 * from the device's camera or gallery.
 *
 * This activity handles:
 * - Rendering the form using Jetpack Compose.
 * - Managing the state of all input fields.
 * - Handling runtime permissions for camera access.
 * - Processing image selection from the camera and gallery.
 * - Saving the new book to both the local SQLite database and Firebase.
 */
class AddBookPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BankLoginSystemTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AddBookScreen(this)
                }
            }
        }
    }
}

/**
 * Creates a temporary image file in the app's private cache directory and returns a content URI for it.
 * Using a FileProvider is the modern, secure way to share file access with other apps like the camera.
 * The authority for the FileProvider must be declared in the AndroidManifest.xml file.
 *
 * @param context The application context.
 * @return A content URI for the new image file.
 */
private fun createImageFile(context: Context): Uri {
    // Create a unique file name using a timestamp to avoid collisions.
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir: File = context.cacheDir // Use the private app cache directory.
    val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)

    // Generate a content URI using the FileProvider.
    // The authority MUST match the one defined in AndroidManifest.xml.
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider", // This must match the manifest
        imageFile
    )
}

/**
 * Stateful composable that manages the logic for adding a new book.
 * It handles state, user input, permissions, and database interactions.
 *
 * @param context The context of the activity.
 */
@Composable
fun AddBookScreen(context: Context) {
    val isInPreview = LocalInspectionMode.current
    val activity = context as? Activity

    // --- DEPENDENCY INITIALIZATION ---
    val dbHelper = remember { if (!isInPreview) DatabaseHelper(context) else null }
    val userSessionManager = remember { if (!isInPreview) UserSessionManager(context) else null }
    val firebaseManager = remember { if (!isInPreview) FirebaseManager() else null }
    val firebaseAuth = remember { if (!isInPreview) FirebaseAuth.getInstance() else null }

    // --- FORM STATE MANAGEMENT ---
    var bookName by remember { mutableStateOf(TextFieldValue("")) }
    var authorName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    // --- IMAGE STATE MANAGEMENT ---
    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    var coverImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(coverImageUri) {
        if (coverImageUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(coverImageUri!!)
                coverImageBitmap = BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        } else {
            coverImageBitmap = null
        }
    }

    // --- DROPDOWN MENU DATA ---
    val categories = listOf("FICTION", "NON-FICTION", "POETRY", "DRAMA / PLAYS", "COMICS / GRAPHIC NOVELS", "CHILDREN’S BOOKS", "YOUNG ADULT (YA)")
    val genreMap = mapOf(
        "FICTION" to listOf("Fantasy", "Mystery", "Romance", "Adventure", "Horror", "Science-Fiction", "Drama", "Comedy", "Thriller", "Historical Fiction", "Action"),
        "NON-FICTION" to listOf("Biography", "History", "Self-Help", "Science", "Philosophy", "Religion", "Politics", "Art Criticism"),
        "POETRY" to listOf("Epic", "Lyric", "Narrative", "Free Verse", "Verse", "Prose", "Epistolary"),
        "DRAMA / PLAYS" to listOf("Tragedy", "Comedy", "Modern Drama", "Historical Drama", "Surreal"),
        "COMICS / GRAPHIC NOVELS" to listOf("Superhero", "Manga", "Graphic Memoir", "Graphic Novel", "Single-issue Comic", "Annual-issue"),
        "CHILDREN’S BOOKS" to listOf("Picture Books", "Middle Grade", "Fairytale", "Religious picture book", "Anthologies"),
        "YOUNG ADULT (YA)" to listOf("Fantasy", "Romance", "Dystopian", "Drama", "Vampires", "High-School Drama")
    )
    val statuses = listOf("Currently Reading", "Completed", "Paused", "Dropped", "Plan to Read")

    // --- ACTIVITY RESULT LAUNCHERS ---
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            coverImageUri = tempImageUri
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val newImageUri = createImageFile(context)
            tempImageUri = newImageUri
            cameraLauncher.launch(newImageUri)
        } else {
            Toast.makeText(context, "Camera permission is required to take a picture.", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            coverImageUri = uri
        }
    }

    // --- EVENT HANDLERS ---
    val onCameraClick = {
        if (!isInPreview) {
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                PackageManager.PERMISSION_GRANTED -> {
                    val newImageUri = createImageFile(context)
                    tempImageUri = newImageUri
                    cameraLauncher.launch(newImageUri)
                }
                else -> {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }

    val onGalleryClick = {
        galleryLauncher.launch("image/*")
    }

    val onReturnClick = {
        activity?.finish()
    }

    val onSubmitClick = {
        if (!isInPreview) {
            val name = bookName.text.trim()
            val author = authorName.text.trim()
            if (name.isEmpty() || author.isEmpty() || selectedCategory == null || selectedGenre == null) {
                Toast.makeText(context, "Please fill all required fields.", Toast.LENGTH_SHORT).show()
            } else {
                val userDetails = userSessionManager?.getUserDetails()
                val userId = userDetails?.get(UserSessionManager.PREF_USER_ID)?.toIntOrNull() ?: 0
                val firebaseUser = firebaseAuth?.currentUser

                if (userId == 0 || firebaseUser == null) {
                    Toast.makeText(context, "Error: User not logged in.", Toast.LENGTH_SHORT).show()
                } else {
                    fun saveBookData(coverUrl: String?) {
                        val bookForFirebase = Book(
                            name = name,
                            author = author,
                            category = selectedCategory!!,
                            genre = selectedGenre!!,
                            coverUri = coverUrl,
                            status = selectedStatus
                        )

                        firebaseManager?.saveBook(userId = firebaseUser.uid, book = bookForFirebase) { firebaseKey ->
                            isLoading = false
                            if (firebaseKey != null) {
                                val success = dbHelper?.insertUserBook(
                                    userId, name, author, selectedCategory!!, selectedGenre!!, coverUrl, selectedStatus, firebaseKey
                                ) ?: false
                                if (success) {
                                    Toast.makeText(context, "Book added successfully!", Toast.LENGTH_SHORT).show()
                                    activity?.finish()
                                } else {
                                    Toast.makeText(context, "Error saving book to local database.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Error saving book to Firebase.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    isLoading = true
                    if (coverImageUri != null) {
                        firebaseManager?.uploadImage(coverImageUri!!) { downloadUrl ->
                            if (downloadUrl != null) {
                                saveBookData(downloadUrl)
                            } else {
                                isLoading = false
                                Toast.makeText(context, "Failed to upload image.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        saveBookData(null)
                    }
                }
            }
        }
    }

    AddBookForm(
        bookName = bookName,
        onBookNameChange = { bookName = it },
        authorName = authorName,
        onAuthorNameChange = { authorName = it },
        categories = categories,
        selectedCategory = selectedCategory,
        onCategorySelected = {
            selectedCategory = it
            selectedGenre = null // Reset genre when category changes
        },
        genreMap = genreMap,
        selectedGenre = selectedGenre,
        onGenreSelected = { selectedGenre = it },
        statuses = statuses,
        selectedStatus = selectedStatus,
        onStatusSelected = { selectedStatus = it },
        onCameraClick = onCameraClick,
        onGalleryClick = onGalleryClick,
        coverImageBitmap = coverImageBitmap,
        isLoading = isLoading,
        onSubmitClick = onSubmitClick,
        onReturnClick = onReturnClick as () -> Unit
    )
}

/**
 * A stateless composable that displays the book creation form.
 * This composable is optimized for previewing as it contains no business logic.
 */
@Composable
fun AddBookForm(
    bookName: TextFieldValue,
    onBookNameChange: (TextFieldValue) -> Unit,
    authorName: TextFieldValue,
    onAuthorNameChange: (TextFieldValue) -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    genreMap: Map<String, List<String>>,
    selectedGenre: String?,
    onGenreSelected: (String) -> Unit,
    statuses: List<String>,
    selectedStatus: String?,
    onStatusSelected: (String) -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    coverImageBitmap: Bitmap?,
    isLoading: Boolean,
    onSubmitClick: () -> Unit,
    onReturnClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ScanLines()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Add a New Book", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(value = bookName, onValueChange = onBookNameChange, label = { Text("Book Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = authorName, onValueChange = onAuthorNameChange, label = { Text("Author Name") }, modifier = Modifier.fillMaxWidth())

            var categoryExpanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedCategory ?: "Select Category") }
                DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    categories.forEach { category ->
                        DropdownMenuItem(text = { Text(category, color = MaterialTheme.colorScheme.onSurface) }, onClick = { onCategorySelected(category); categoryExpanded = false })
                    }
                }
            }

            val genres = genreMap[selectedCategory] ?: emptyList()
            var genreExpanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { if (selectedCategory != null) genreExpanded = true }, modifier = Modifier.fillMaxWidth(), enabled = selectedCategory != null) { Text(selectedGenre ?: "Select Genre") }
                DropdownMenu(expanded = genreExpanded, onDismissRequest = { genreExpanded = false }) {
                    genres.forEach { genre ->
                        DropdownMenuItem(text = { Text(genre, color = MaterialTheme.colorScheme.onSurface) }, onClick = { onGenreSelected(genre); genreExpanded = false })
                    }
                }
            }

            var statusExpanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { statusExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedStatus ?: "Set Initial Status (Optional)") }
                DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    statuses.forEach { status ->
                        DropdownMenuItem(text = { Text(status, color = MaterialTheme.colorScheme.onSurface) }, onClick = { onStatusSelected(status); statusExpanded = false })
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Insert Cover Image")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onCameraClick) { Text("Camera") }
                    Button(onClick = onGalleryClick) { Text("Gallery") }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .padding(8.dp)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (coverImageBitmap != null) {
                    Image(
                        bitmap = coverImageBitmap.asImageBitmap(),
                        contentDescription = "Cover Image Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("No Image Selected", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onReturnClick,
                ) {
                    Text("Return")
                }
                Button(
                    onClick = onSubmitClick,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Add Book")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddBookFormPreview() {
    BankLoginSystemTheme {
        AddBookForm(
            bookName = TextFieldValue("The Great Gatsby"),
            onBookNameChange = {},
            authorName = TextFieldValue("F. Scott Fitzgerald"),
            onAuthorNameChange = {},
            categories = listOf("FICTION", "NON-FICTION"),
            selectedCategory = "FICTION",
            onCategorySelected = {},
            genreMap = mapOf("FICTION" to listOf("Historical Fiction", "Drama")),
            selectedGenre = "Historical Fiction",
            onGenreSelected = {},
            statuses = listOf("Currently Reading", "Completed"),
            selectedStatus = "Plan to Read",
            onStatusSelected = {},
            onCameraClick = {},
            onGalleryClick = {},
            coverImageBitmap = null,
            isLoading = false,
            onSubmitClick = {},
            onReturnClick = {}
        )
    }
}
