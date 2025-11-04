package com.example.bankloginsystem

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme

class SignUpPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BankLoginSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    SignUpScreen(modifier = Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
fun SignUpScreen(modifier: Modifier = Modifier) {

    val firstName = remember { mutableStateOf("") }
    val lastName = remember { mutableStateOf("") }
    val gender = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val phoneNumber = remember { mutableStateOf("") }
    val initialAmount = 500

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Enable vertical scrolling; easier to read for the user
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Create Account", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = firstName.value, onValueChange = { firstName.value = it }, label = { Text(text = "First Name") })

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = lastName.value, onValueChange = { lastName.value = it }, label = { Text(text = "Last Name") })

        Spacer(modifier = Modifier.height(12.dp))

        GenderSelection(selectedOption = gender.value, onOptionSelected = { gender.value = it })

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = email.value, onValueChange = { email.value = it }, label = { Text(text = "Email") })

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = password.value, onValueChange = { password.value = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = confirmPassword.value, onValueChange = { confirmPassword.value = it }, label = { Text("Confirm Password") }, visualTransformation = PasswordVisualTransformation())

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = phoneNumber.value, onValueChange = { phoneNumber.value = it }, label = { Text("Phone Number") })

        Spacer(modifier = Modifier.height(16.dp))


        Button(onClick = {
            // TODO: Add validation + save to SQLite
        }) {
            Text("Submit")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = {

            val intent = Intent(context, LoginPage::class.java)
            context.startActivity(intent)
        }) {
            Text("Back to Login")
        }
    }


}

@Composable
fun GenderSelection(selectedOption: String, onOptionSelected: (String) -> Unit, modifier: Modifier = Modifier){
    TODO()
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    val context = LocalContext.current
    BankLoginSystemTheme{
        Scaffold {
            SignUpScreen(modifier = Modifier.padding(it))
        }
    }
}