package com.example.bankloginsystem

import android.content.Context
import android.graphics.Bitmap
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme

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

@Composable
fun AddBookScreen(context: Context) {
    val dbHelper = remember { UserBooksDatabaseHelper(context) }
    val userSessionManager = remember { UserSessionManager(context) }

    var bookName by remember { mutableStateOf(TextFieldValue("")) }
    var authorName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val categories = listOf(
        "FICTION", "NON-FICTION", "POETRY", "DRAMA / PLAYS",
        "COMICS / GRAPHIC NOVELS", "CHILDREN’S BOOKS", "YOUNG ADULT (YA)"
    )

    val genreMap = mapOf(
        "FICTION" to listOf("Fantasy", "Mystery", "Romance", "Adventure"),
        "NON-FICTION" to listOf("Biography", "History", "Self-Help", "Science"),
        "POETRY" to listOf("Epic", "Lyric", "Narrative", "Free Verse"),
        "DRAMA / PLAYS" to listOf("Tragedy", "Comedy", "Modern Drama"),
        "COMICS / GRAPHIC NOVELS" to listOf("Superhero", "Manga", "Graphic Memoir"),
        "CHILDREN’S BOOKS" to listOf("Picture Books", "Middle Grade", "Fairytale"),
        "YOUNG ADULT (YA)" to listOf("Fantasy", "Romance", "Dystopian")
    )

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { coverBitmap = it }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { coverImageUri = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFFEEEEEE)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add a New Book", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = bookName,
            onValueChange = { bookName = it },
            label = { Text("Book Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = authorName,
            onValueChange = { authorName = it },
            label = { Text("Author Name") },
            modifier = Modifier.fillMaxWidth()
        )

        // CATEGORY DROPDOWN
        var categoryExpanded by remember { mutableStateOf(false) }
        Box {
            Button(
                onClick = { categoryExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedCategory ?: "Select Category")
            }
            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            selectedCategory = category
                            selectedGenre = null
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        // GENRE DROPDOWN (depends on category)
        val genres = genreMap[selectedCategory] ?: emptyList()
        var genreExpanded by remember { mutableStateOf(false) }

        Box {
            Button(
                onClick = { if (selectedCategory != null) genreExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedCategory != null
            ) {
                Text(selectedGenre ?: "Select Genre")
            }
            DropdownMenu(
                expanded = genreExpanded,
                onDismissRequest = { genreExpanded = false }
            ) {
                genres.forEach { genre ->
                    DropdownMenuItem(
                        text = { Text(genre) },
                        onClick = {
                            selectedGenre = genre
                            genreExpanded = false
                        }
                    )
                }
            }
        }

        // IMAGE SELECTION
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Insert Cover Image")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { cameraLauncher.launch(null) }) {
                    Text("📷 Camera")
                }
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("🖼 Gallery")
                }
            }
        }

        coverBitmap?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = "Cover Preview", modifier = Modifier.size(150.dp))
        }

        coverImageUri?.let {
            Text("Selected image: ${it.lastPathSegment}", color = Color.DarkGray)
        }

        // SUBMIT BUTTON
        Button(
            onClick = {
                if (bookName.text.isEmpty() || authorName.text.isEmpty() ||
                    selectedCategory == null || selectedGenre == null
                ) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                } else {
                    val userId = userSessionManager.getUserDetails()[UserSessionManager.PREF_USER_ID]
                    val result = dbHelper.insertUserBook(
                        userId = userId!!.toInt(),
                        bookId = null,
                        name = bookName.text,
                        author = authorName.text,
                        category = selectedCategory!!,
                        genre = selectedGenre!!,
                        coverUri = coverImageUri?.toString(),
                        status = null
                    )

                    if (result) {
                        Toast.makeText(context, "Book added successfully!", Toast.LENGTH_SHORT).show()
                        bookName = TextFieldValue("")
                        authorName = TextFieldValue("")
                        selectedCategory = null
                        selectedGenre = null
                        coverImageUri = null
                        coverBitmap = null
                    } else {
                        Toast.makeText(context, "Error adding book.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A5E2A))
        ) {
            Text("Submit", color = Color.White)
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
