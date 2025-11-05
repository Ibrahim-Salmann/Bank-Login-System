package com.example.bankloginsystem

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme

// The activity responsible for hosting the user sign-up screen.
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

/**
 * The main UI for the user registration screen.
 * It includes fields for personal details, credentials, and navigation controls.
 *
 * @param modifier Modifier for this composable.
 */
@Composable
fun SignUpScreen(modifier: Modifier = Modifier) {

    // State holders for each input field to observe and react to user input.
    val firstName = remember { mutableStateOf("") }
    val lastName = remember { mutableStateOf("") }
    val gender = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val phoneNumber = remember { mutableStateOf("") }
    // A fixed initial amount for every new user account.
    val initialAmount = 500

    // LocalContext is used for creating Intents to navigate between activities.
    val context = LocalContext.current

    // The main layout is a scrollable Column to accommodate all input fields on any screen size.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Great choice! This makes the UI adapt to smaller screens.
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

        // Provides a way for the user to navigate back to the login screen.
        TextButton(onClick = {
            val intent = Intent(context, LoginPage::class.java)
            context.startActivity(intent)
        }) {
            Text("Back to Login")
        }
    }
}

/**
 * A reusable composable that displays a set of radio buttons for gender selection.
 *
 * @param selectedOption The currently selected gender option.
 * @param onOptionSelected A callback function that is invoked when a new option is selected.
 * @param modifier Modifier for this composable.
 */
@Composable
fun GenderSelection(selectedOption: String, onOptionSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf("Male", "Female", "Rather Not Say")

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Gender", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp), fontSize = 24.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Iterate through the options and create a radio button for each.
            options.forEach { text ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .selectable(
                            selected = (text == selectedOption),
                            onClick = { onOptionSelected(text) }
                        )
                        .padding(horizontal = 4.dp, vertical = 8.dp) // Added vertical padding for a better touch target.
                ) {
                    RadioButton(
                        selected = (text == selectedOption),
                        onClick = { onOptionSelected(text) }
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * A preview function to render the `SignUpScreen` in Android Studio's design view.
 * It's wrapped in the app's theme to ensure consistent styling.
 */
@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    BankLoginSystemTheme {
        Scaffold {
            SignUpScreen(modifier = Modifier.padding(it))
        }
    }
}
