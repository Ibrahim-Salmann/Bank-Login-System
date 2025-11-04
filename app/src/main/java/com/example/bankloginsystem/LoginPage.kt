package com.example.bankloginsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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


@Composable
fun PasswordInput(modifier: Modifier = Modifier){
    var input by remember { mutableStateOf("") }
    Box(modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFFFFC107))
        .padding(horizontal = 24.dp, vertical = 16.dp),
    contentAlignment = Alignment.Center
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Creating a button composable that will be used across all pages.
@Composable
fun ButtonClicked(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text = text)
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
            // Add the PasswordInput composable to the HomeScreen
            PasswordInput(modifier = Modifier.padding(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ButtonClicked("Sign Up", { TODO(/* navigate to signup */) }, modifier = Modifier.weight(1f))
                ButtonClicked("Login", { TODO(/* navigate to login */) }, modifier = Modifier.weight(1f))
                ButtonClicked("Exit", {TODO(/* exit app or break */)}, modifier = Modifier.weight(1f))

            }
        }
    }
}
