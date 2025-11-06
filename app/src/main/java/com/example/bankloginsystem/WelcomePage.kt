package com.example.bankloginsystem

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme

class WelcomePage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieving the user's full name when logging in
        val firstName = intent.getStringExtra("first_name") ?: ""
        val lastName = intent.getStringExtra("last_name") ?: ""
        val fullName = "$firstName $lastName".trim()

        setContent {
            BankLoginSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    WelcomePageScreen(modifier = Modifier.padding(padding),
                        fullName = fullName)
                }
            }
        }
    }
}

@Composable
fun WelcomePageScreen(modifier: Modifier = Modifier, fullName: String = " ") {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display name
        if (fullName.isNotEmpty()) {
            Text(text = "Welcome, $fullName!", modifier = Modifier.padding(bottom = 24.dp))
        } else {
            Text(text = "Welcome!", modifier = Modifier.padding(bottom = 24.dp))
        }

        Button(onClick = {
            val intent = Intent(context, LoginPage::class.java)
            context.startActivity(intent)
            (context as? Activity)?.finish() // optional: prevents back navigation
        }) {
            Text("Logout")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomePageScreenPreview() {
    BankLoginSystemTheme {
        WelcomePageScreen()
    }
}
