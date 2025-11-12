package com.example.bankloginsystem

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme
import kotlin.jvm.java
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight

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
                    BookShelfPage(modifier = Modifier.padding(padding),
                        userId = userId.toString()) } }
        }
    }
}

@Composable
fun BookShelfPage(modifier: Modifier = Modifier, userId: String) {
    var searchQuery by remember { mutableStateOf("") }
    var deleteMode by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Placeholder data (we’ll replace with DB data later)
    /**
     * This is just for code functionality and will later be replaced by the database
     */
    val allBooks = remember {
        mutableStateListOf(
            BookDisplay("The Hobbit", "J.R.R. Tolkien", "Fantasy", "Adventure"),
            BookDisplay("Dune", "Frank Herbert", "Sci-Fi", "Epic"),
            BookDisplay("1984", "George Orwell", "Fiction", "Dystopia")
        )
    }


    val filteredBooks = allBooks.filter {
        it.name.contains(searchQuery, true) ||
                it.author.contains(searchQuery, true) ||
                it.category.contains(searchQuery, true) ||
                it.genre.contains(searchQuery, true)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top tab bar
        TopToolbar(
            deleteMode = deleteMode,
            onAddClick = { /* TODO: Intent to AddBookPage */ },
            onDeleteToggle = { deleteMode = !deleteMode },
            onReturnClick = {
//                val context = LocalContext.current
                val intent = Intent(context, WelcomePage::class.java)
                context.startActivity(intent)
                (context as? Activity)?.finish()
            }
        )

        // Search Bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        // Book List
        LazyColumn {
            items(filteredBooks) { book ->
                BookRow(book = book, deleteMode = deleteMode)
            }
        }
    }



}

@Composable
fun TopToolbar(deleteMode: Boolean, onAddClick: () -> Unit, onDeleteToggle: () -> Unit, onReturnClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF063041))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = onAddClick, enabled = !deleteMode) { Text("Add Book") }
        Button(onClick = onDeleteToggle) { Text(if (deleteMode) "Cancel" else "Delete") }
        Button(onClick = onReturnClick, enabled = !deleteMode) { Text("Return") }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        leadingIcon = { Text("🔍") },
        placeholder = { Text("Search books...") },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color(0xFFEEEEEE),
            focusedContainerColor = Color(0xFFFFFFFF)
        )
    )
}

@Composable
fun BookRow(book: BookDisplay, deleteMode: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color(0xFF0B3954))
            .clickable { /* TODO: select for delete or change status */ }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(book.name, color = Color.White, fontWeight = FontWeight.Bold)
            Text("By ${book.author}", color = Color.LightGray)
            Text("${book.category} • ${book.genre}", color = Color.Gray)
        }
        Text("📘", fontSize = MaterialTheme.typography.headlineSmall.fontSize)
    }
}

data class BookDisplay(
    val name: String,
    val author: String,
    val category: String,
    val genre: String
)

@Preview(showBackground = true)
@Composable
fun BookShelfScreenPreview() {
    BankLoginSystemTheme {
        BookShelfPage(userId = "1")
    }
}