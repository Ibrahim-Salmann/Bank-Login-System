package com.example.bankloginsystem

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme
import com.example.bankloginsystem.ui.theme.ScanLines
import com.google.firebase.auth.FirebaseAuth
import kotlin.system.exitProcess

class LoginPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Session in Android app using SharedPreference
        /**
         * Session Check on App Start:
         * 1. An instance of UserSessionManager is created.
         * 2. It immediately checks if a user is already logged in using `isLoggedIn()`.
         * 3. If true, it skips the LoginPage and navigates directly to the WelcomePage.
         * This is the core of the persistent session experience.
         */
        val userSessionManager = UserSessionManager(this)
        if (userSessionManager.isLoggedIn()){
            val intent = Intent(this, WelcomePage::class.java)
            startActivity(intent)
            finish()
        }
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
fun EmailInput(email: String, onEmailChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email") },
        modifier = modifier.fillMaxWidth()
    )
}


@Composable
fun PasswordInput(
    password: String,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        modifier = modifier.fillMaxWidth(),
        visualTransformation = if (passwordVisible)
            VisualTransformation.None
        else
            PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Image(
                    painter = painterResource(
                        id = if (passwordVisible)
                            R.drawable.ic_visibility_off
                        else
                            R.drawable.ic_visibility
                    ),
                    contentDescription = if (passwordVisible)
                        "Hide password"
                    else
                        "Show password",
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
            }
        }
    )
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


// **Firebase and SQLite Integration**
// This function first validates the login with Firebase, and if successful,
// proceeds with the existing SQLite and session management logic.
fun validateLogin(context: Context, email: String, password: String, onLoginSuccess: () -> Unit) {
    if (email.isBlank() || password.isBlank()) {
        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
        return
    }

    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // If Firebase authentication is successful, call the onLoginSuccess lambda
                // to proceed with the app's existing login logic.
                onLoginSuccess()
            } else {
                Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
}



@Preview(showBackground = true)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val userSessionManager = UserSessionManager(context)
    Box(modifier = modifier.fillMaxSize()) {
        ScanLines()
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Login Screen")
            // Add the EmailInput composable to the HomeScreen
            EmailInput( email = email, onEmailChange = { email = it }, modifier = Modifier.padding(16.dp))
            // Add the PasswordInput composable to the HomeScreen
            PasswordInput(password = password, onPasswordChange = {password = it}, modifier = Modifier.padding(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ButtonClicked("Sign Up", {
                    val intent = Intent(context, SignUpPage::class.java)
                    context.startActivity(intent)
                }, modifier = Modifier.weight(1f))
                ButtonClicked("Login", {
                    // **Firebase and SQLite Integration**
                    // 1. Validate with Firebase
                    validateLogin(context, email, password) {
                        // 2. On successful Firebase login, proceed with SQLite and session management
                        val dbHelper = DatabaseHelper(context)
                        val cursor = dbHelper.getUserByEmail(email)

                        if (cursor.moveToFirst()){
                            val firstName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FIRST_NAME))
                            val lastName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LAST_NAME))
                            val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))

                            cursor.close()
                            dbHelper.close()

                            userSessionManager.saveUser("$firstName $lastName", email, id)
                            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(context, WelcomePage::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            context.startActivity(intent)
                        } else {
                            // This case should ideally not happen if a user is authenticated with Firebase
                            // and their data is in the local SQLite database.
                            Toast.makeText(context, "User data not found in local database.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, modifier = Modifier.weight(1f))
                // finishAffinity() ensures the backstack is cleared, while System.exit(0) stops the app process.
                ButtonClicked("Exit", {
                    // existing the program
                    val activity = (context as? Activity)
                    // closes all activities in the stack
                    activity?.finishAffinity()
//                     terminates the process
                    exitProcess(0)
                }, modifier = Modifier.weight(1f))

            }
        }
    }
}
