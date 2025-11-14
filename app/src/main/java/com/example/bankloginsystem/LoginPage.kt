package com.example.bankloginsystem

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme
import kotlin.jvm.java
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
//    var input by remember { mutableStateOf("") }
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
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
fun PasswordInput(
    password: String,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFC107))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
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
                        colorFilter = ColorFilter.tint(Color.Black)
                    )
                }
            }
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


fun validateLogin(context: Context, email: String, password: String): Boolean {
    val dbHelper = DatabaseHelper(context)
    var cursor: Cursor? = null

    try {
        // 1 Check for blank fields
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return false
        }

        // 2 Check if user exists in the database
        cursor = dbHelper.getUserByEmail(email)
        if (!cursor.moveToFirst()) { // cursor == null || will always be false
            Toast.makeText(context, "User not found. Please sign up first.", Toast.LENGTH_SHORT).show()
            return false
        }

        // 3 Retrieve stored hashed password
        val storedPassword = cursor.getString(
            cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PASSWORD)
        )

        // 4 Hash input password and compare
        val hashedInput = hashPassword(password)
        if (storedPassword != hashedInput) {
            Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
            return false
        }

        // 5 Success
        Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
        return true

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Login failed due to an error", Toast.LENGTH_SHORT).show()
        return false

    } finally {
        cursor?.close()
        dbHelper.close()
    }
}



@Preview(showBackground = true)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val userSessionManager = UserSessionManager(context)
    Scaffold { it ->
        Column(
            modifier = modifier.padding(it),
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
//                    val context = LocalContext.current // error: context is not available here. Fixed by Placing it in the main body of this function. Thus calling it from a @Composable context, which is valid context variable is then "captured" by the onClick lambda, allowing you to use it to create an Intent and start the SignUpPage activity without any errors.
                    val intent = Intent(context, SignUpPage::class.java) // error: context is not available here. Fixed by creating: class SignUpPage : ComponentActivity(){} in the SighUpPage.kt file
                    context.startActivity(intent)
                }, modifier = Modifier.weight(1f))
                ButtonClicked("Login", {
                    // Every validation will be examined using the validateLogin function making sure all are true
                    val isValid = validateLogin(context, email, password)
                    if (isValid) {

                        val dbHelper = DatabaseHelper(context)
                        val cursor = dbHelper.getUserByEmail(email)

                        if (cursor.moveToFirst()){
                            val firstName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FIRST_NAME))
                            val lastName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LAST_NAME))
                            val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))

                            cursor.close()
                            dbHelper.close()

                            // Pass user data to WelcomePage
                            /**
                             * Creating the Session:
                             * Upon successful login, `saveUser()` is called.
                             * This stores the user's name and email in SharedPreferences, officially starting the session.
                             */
                            userSessionManager.saveUser("$firstName $lastName", email, id)
                            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(context, WelcomePage::class.java).apply {
                                // Add flags to prevent back navigation
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            context.startActivity(intent)
                        }
//                        Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
//                        val intent = Intent(context, WelcomePage::class.java)
//                        context.startActivity(intent)
                    }
//                    else {
//                        Toast.makeText(context, "Invalid email or password", Toast.LENGTH_SHORT).show()
//                    }
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
