package com.example.bankloginsystem

import android.Manifest
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import java.util.*

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
 * The main composable that builds the UI for the "Add Book" screen.
 * It manages the entire state of the form, including text inputs, dropdowns, image selection,
 * and the submission process.
 *
 * @param context The context of the activity, used for creating intents and accessing resources.
 */
@Composable
fun AddBookScreen(context: Context) {
    val isInPreview = LocalInspectionMode.current
    // Safely cast the context to an Activity to allow finishing the activity.
    val activity = context as? Activity

    // --- DEPENDENCY INITIALIZATION ---
    val dbHelper = remember { DatabaseHelper(context) }
    val userSessionManager = remember { UserSessionManager(context) }
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
    var isLoading by remember { mutableStateOf(false) } // To show a loading indicator during image upload.

    // This listener reacts to changes in `coverImageUri`. When a new URI is set
    // (from camera or gallery), it loads the image into a Bitmap for display.
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
            coverImageBitmap = null // Clear preview if URI is removed
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
    var tempImageUri by remember { mutableStateOf<Uri?>(null) } // Temp storage for camera URI

    // Launcher for the camera app. It takes a picture and saves it to the provided URI.
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            coverImageUri = tempImageUri // On success, update the main URI state
        }
    }

    // Launcher for the camera permission request.
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val newImageUri = createImageFile(context)
            tempImageUri = newImageUri
            cameraLauncher.launch(newImageUri) // If permission is granted, launch the camera
        } else {
            Toast.makeText(context, "Camera permission is required to take a picture.", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher for the gallery. It lets the user pick an image.
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            coverImageUri = uri // On selection, update the main URI state
        }
    }

    // --- UI LAYOUT ---
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

            // --- INPUT FIELDS ---
            OutlinedTextField(value = bookName, onValueChange = { bookName = it }, label = { Text("Book Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = authorName, onValueChange = { authorName = it }, label = { Text("Author Name") }, modifier = Modifier.fillMaxWidth())

            // --- DROPDOWNS for Category, Genre, and Status ---
            var categoryExpanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedCategory ?: "Select Category") }
                DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    categories.forEach { category ->
                        DropdownMenuItem(text = { Text(category) }, onClick = { selectedCategory = category; selectedGenre = null; categoryExpanded = false })
                    }
                }
            }

            val genres = genreMap[selectedCategory] ?: emptyList()
            var genreExpanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { if (selectedCategory != null) genreExpanded = true }, modifier = Modifier.fillMaxWidth(), enabled = selectedCategory != null) { Text(selectedGenre ?: "Select Genre") }
                DropdownMenu(expanded = genreExpanded, onDismissRequest = { genreExpanded = false }) {
                    genres.forEach { genre ->
                        DropdownMenuItem(text = { Text(genre) }, onClick = { selectedGenre = genre; genreExpanded = false })
                    }
                }
            }

            var statusExpanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { statusExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedStatus ?: "Set Initial Status (Optional)") }
                DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    statuses.forEach { status ->
                        DropdownMenuItem(text = { Text(status) }, onClick = { selectedStatus = status; statusExpanded = false })
                    }
                }
            }

            // --- IMAGE SELECTION BUTTONS (Camera and Gallery) ---
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Insert Cover Image")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = {
                        // Check for camera permission before launching the camera.
                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                            PackageManager.PERMISSION_GRANTED -> {
                                val newImageUri = createImageFile(context)
                                tempImageUri = newImageUri
                                cameraLauncher.launch(newImageUri)
                            }
                            else -> {
                                // If not granted, request permission.
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }) { Text("Camera") }
                    Button(onClick = { galleryLauncher.launch("image/*") }) { Text("Gallery") }
                }
            }

            // --- IMAGE PREVIEW ---
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
                        bitmap = coverImageBitmap!!.asImageBitmap(),
                        contentDescription = "Cover Image Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("No Image Selected", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Pushes the button to the bottom

            // --- SUBMIT BUTTON ---
            Button(
                onClick = {
                    // --- INPUT VALIDATION ---
                    val name = bookName.text.trim()
                    val author = authorName.text.trim()
                    if (name.isEmpty() || author.isEmpty() || selectedCategory == null || selectedGenre == null) {
                        Toast.makeText(context, "Please fill all required fields.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // --- USER AUTHENTICATION CHECK ---
                    val userDetails = userSessionManager.getUserDetails()
                    val userId = userDetails[UserSessionManager.PREF_USER_ID]?.toIntOrNull() ?: 0
                    val firebaseUser = firebaseAuth?.currentUser

                    if (userId == 0 || firebaseUser == null) {
                        Toast.makeText(context, "Error: User not logged in.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // --- DATABASE OPERATIONS ---
                    // This function saves the book data to both Firebase and the local SQLite database.
                    fun saveBookData(coverUrl: String?) {
                        val bookForFirebase = Book(
                            name = name,
                            author = author,
                            category = selectedCategory!!,
                            genre = selectedGenre!!,
                            coverUri = coverUrl,
                            status = selectedStatus
                        )

                        firebaseManager?.saveBook(
                            userId = firebaseUser.uid,
                            book = bookForFirebase
                        ) { firebaseKey ->
                            isLoading = false
                            if (firebaseKey != null) {
                                // If Firebase save is successful, save to the local SQLite database.
                                val success = dbHelper.insertUserBook(
                                    userId,
                                    name,
                                    author,
                                    selectedCategory!!,
                                    selectedGenre!!,
                                    coverUrl,
                                    selectedStatus,
                                    firebaseKey // Pass the key from Firebase here.
                                )
                                if (success) {
                                    Toast.makeText(context, "Book added successfully!", Toast.LENGTH_SHORT).show()
                                    activity?.finish() // Close the activity on success.
                                } else {
                                    Toast.makeText(context, "Error saving book to local database.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Error saving book to Firebase.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    // If a cover image was selected, upload it to Firebase Storage first.
                    if (coverImageUri != null) {
                        isLoading = true
                        Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
                        firebaseManager?.uploadImage(coverImageUri!!) { downloadUrl ->
                            if (downloadUrl != null) {
                                // Once the image is uploaded, save the book data with the public download URL.
                                saveBookData(downloadUrl)
                            } else {
                                isLoading = false
                                Toast.makeText(context, "Error uploading image. Please try again.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        // If no image was selected, save the book data with a null cover URL.
                        isLoading = true
                        saveBookData(null)
                    }
                },
                enabled = !isLoading, // Disable the button while an operation is in progress.
                modifier = Modifier.fillMaxWidth()
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

/**
 * A preview function to render the `AddBookScreen` in Android Studio's design view.
 * It's wrapped in the app's theme to ensure consistent styling.
 */
@Preview(showBackground = true)
@Composable
fun AddBookScreenPreview() {
    BankLoginSystemTheme {
        AddBookScreen(LocalContext.current)
    }
}
