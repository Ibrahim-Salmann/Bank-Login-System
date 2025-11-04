package com.example.bankloginsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
<<<<<<< HEAD
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme

class LoginPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BankLoginSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun EmailInput(modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("") }
    Box(
        modifier = modifier
            // Clip the shape of the Box to have rounded corners
            .clip(RoundedCornerShape(12.dp))
            // Set the background color of the Box
            .background(Color(0xFFFFC107))
            // Apply padding around the content inside the Box
            .padding(horizontal = 24.dp, vertical = 16.dp),
        // Align the content within the Box to the center
        contentAlignment = Alignment.Center
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Scaffold {
        Column(
            modifier = modifier.padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Login Screen")
            // Add the EmailInput composable to the HomeScreen
            EmailInput(modifier = Modifier.padding(16.dp))
        }
    }
}
=======
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme

>>>>>>> 49a9a3c5929e010c3990c576f6a38407ae5a0578
