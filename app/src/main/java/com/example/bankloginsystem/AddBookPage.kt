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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
 * Creates a temporary image file and returns its URI.
 * This is the modern, secure way to provide a file path to the camera app.
 * @param context The application context.
 * @return A content URI pointing to the newly created file.
 */
private fun createImageFile(context: Context): Uri {
    // 1. Create a unique file name with a timestamp to avoid collisions.
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    // 2. Get the directory where the app can store private pictures.
    //    This is defined in `res/xml/file_paths.xml`.
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)

    // 3. Return a content URI using the FileProvider. This URI is a secure, shareable
    //    reference to the file, which doesn't expose the actual file system path.
    //    The authority string `"${context.packageName}.provider"` MUST match the one in AndroidManifest.xml.
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        imageFile
    )
}

@Composable
fun AddBookScreen(context: Context) {
    val dbHelper = remember { UserBooksDatabaseHelper(context) }
    val userSessionManager = remember { UserSessionManager(context) }

    var bookName by remember { mutableStateOf(TextFieldValue("")) }
    var authorName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    var coverImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    /**
     * --- CAMERA/GALLERY WORKFLOW EXPLANATION: Part 1 ---
     * This LaunchedEffect block is a listener. It triggers whenever `coverImageUri` changes.
     * Its purpose is to take the URI (from either the camera or gallery) and load it into a Bitmap
     * that can be displayed by the `Image` composable. This is done in a coroutine to avoid
     * blocking the main UI thread while reading the file.
     */
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

    val categories = listOf(
        "FICTION", "NON-FICTION", "POETRY", "DRAMA / PLAYS",
        "COMICS / GRAPHIC NOVELS", "CHILDREN’S BOOKS", "YOUNG ADULT (YA)"
    )
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

    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    /**
     * --- CAMERA/GALLERY WORKFLOW EXPLANATION: Part 2 ---
     * This is the launcher for the camera app. Its job is to take a URI we provide,
     * launch the camera, and wait for a result. It does NOT ask for permission itself.
     * When the camera app successfully saves a photo to our URI, `success` will be true.
     */
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            // The photo was taken and saved. We now update our main state with the URI.
            coverImageUri = tempImageUri
        }
    }

    /**
     * --- CAMERA/GALLERY WORKFLOW EXPLANATION: Part 3 ---
     * This is the launcher for the permission request. Its only job is to ask the user
     * for a single permission (in this case, `Manifest.permission.CAMERA`).
     */
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // The user granted permission! We can now proceed to launch the camera.
            val newImageUri = createImageFile(context)
            tempImageUri = newImageUri
            cameraLauncher.launch(newImageUri)
        } else {
            // The user denied the permission. It's good practice to show a message explaining why it's needed.
            Toast.makeText(context, "Camera permission is required to take a picture.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * --- CAMERA/GALLERY WORKFLOW EXPLANATION: Part 4 ---
     * This is the launcher for the photo gallery. It's much simpler because on modern Android,
     * we don't need special permissions just to let the user *pick* a photo.
     * The system handles the permission and file access securely.
     */
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // The user picked a photo, and the system gives us a temporary, secure URI.
        coverImageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .background(Color(0xFFEEEEEE)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add a New Book", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = bookName, onValueChange = { bookName = it }, label = { Text("Book Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = authorName, onValueChange = { authorName = it }, label = { Text("Author Name") }, modifier = Modifier.fillMaxWidth())

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

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Insert Cover Image")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                /**
                 * --- CAMERA/GALLERY WORKFLOW EXPLANATION: Part 5 (The Final Step) ---
                 * This is where everything comes together. When the Camera button is clicked:
                 */
                Button(onClick = {
                    // 1. We check if we ALREADY have camera permission.
                    when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                        PackageManager.PERMISSION_GRANTED -> {
                            // 2a. If permission is granted, we launch the camera directly.
                            val newImageUri = createImageFile(context)
                            tempImageUri = newImageUri
                            cameraLauncher.launch(newImageUri)
                        }
                        else -> {
                            // 2b. If permission is NOT granted, we launch the permission request launcher.
                            //    The result of this request will be handled in Part 3.
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }) { Text("📷 Camera") }

                // The gallery button is simpler; it just launches the gallery picker.
                Button(onClick = { galleryLauncher.launch("image/*") }) { Text("🖼 Gallery") }
            }
        }

        coverImageBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Cover Preview",
                modifier = Modifier.size(150.dp)
            )
        }

        Button(
            onClick = {
                if (bookName.text.isEmpty() || authorName.text.isEmpty() || selectedCategory == null || selectedGenre == null) {
                    Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                } else {
                    val userId = userSessionManager.getUserDetails()[UserSessionManager.PREF_USER_ID]
                    val result = dbHelper.insertUserBook(
                        userId = userId!!.toInt(),
                        name = bookName.text,
                        author = authorName.text,
                        category = selectedCategory!!,
                        genre = selectedGenre!!,
                        coverUri = coverImageUri?.toString(),
                        status = selectedStatus
                    )

                    if (result) {
                        Toast.makeText(context, "Book added successfully!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(context, BookShelfPage::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        (context as? Activity)?.finish()
                    } else {
                        Toast.makeText(context, "Error adding book. It may already be in your library.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A5E2A))
        ) { Text("Submit", color = Color.White) }

        OutlinedButton(
            onClick = {
                (context as? Activity)?.finish()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddBookScreenPreview() {
    BankLoginSystemTheme {
        AddBookScreen(context = LocalContext.current)
    }
}
