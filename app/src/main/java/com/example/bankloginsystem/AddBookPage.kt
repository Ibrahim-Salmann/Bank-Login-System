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

private fun createImageFile(context: Context): Uri {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
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
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            coverImageUri = tempImageUri
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val newImageUri = createImageFile(context)
            tempImageUri = newImageUri
            cameraLauncher.launch(newImageUri)
        } else {
            Toast.makeText(context, "Camera permission is required to take a picture.", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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
                Button(onClick = {
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
                }) { Text("📷 Camera") }
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

        // IMPLEMENTATION: Added a button to cancel and return to the bookshelf
        OutlinedButton(
            onClick = {
                // Simply finish the current activity to go back.
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
