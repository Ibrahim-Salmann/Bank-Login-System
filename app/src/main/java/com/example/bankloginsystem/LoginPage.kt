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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme
import com.example.bankloginsystem.ui.theme.ScanLines
import com.google.android.recaptcha.RecaptchaAction
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class LoginPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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


@Composable
fun ButtonClicked(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text = text)
    }
}


fun validateLogin(context: Context, email: String, password: String, onLoginSuccess: () -> Unit) {
    if (email.isBlank() || password.isBlank()) {
        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
        return
    }

    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
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
    val isInPreview = LocalInspectionMode.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val userSessionManager = UserSessionManager(context)
    val app = if(!isInPreview) context.applicationContext as App else null
    val recaptchaScope = CoroutineScope(Dispatchers.Main)
    val firebaseManager = remember { if (!isInPreview) FirebaseManager() else null }


    Box(modifier = modifier.fillMaxSize()) {
        ScanLines()
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Login Screen")
            EmailInput( email = email, onEmailChange = { email = it }, modifier = Modifier.padding(16.dp))
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
                    recaptchaScope.launch {
                        app?.recaptchaClient?.execute(RecaptchaAction.LOGIN)
                            ?.onSuccess {
                                validateLogin(context, email, password) {
                                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                                    if (firebaseUser != null) {
                                        val dbHelper = DatabaseHelper(context)
                                        if (dbHelper.userExists(email)) {
                                            val cursor = dbHelper.getUserByEmail(email)
                                            if (cursor.moveToFirst()) {
                                                val firstName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FIRST_NAME))
                                                val lastName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LAST_NAME))
                                                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                                                cursor.close()

                                                userSessionManager.saveUser("$firstName $lastName", email, id)
                                                Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                                                val intent = Intent(context, WelcomePage::class.java).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                }
                                                context.startActivity(intent)
                                            }
                                        } else {
                                            firebaseManager?.getUser(firebaseUser.uid) { user ->
                                                if (user != null) {
                                                    val nameParts = user.fullName.split(" ")
                                                    val firstName = nameParts.getOrNull(0) ?: ""
                                                    val lastName = nameParts.getOrNull(1) ?: ""
                                                    // This is a new device, so we need to create a new user entry in the local database.
                                                    val success = dbHelper.insertUser(firstName, lastName, "", email, "", "", user.balance)
                                                    if (success) {
                                                        val newCursor = dbHelper.getUserByEmail(email)
                                                        if (newCursor.moveToFirst()) {
                                                            val id = newCursor.getInt(newCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                                                            newCursor.close()
                                                            userSessionManager.saveUser(user.fullName, email, id)
                                                            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                                                            val intent = Intent(context, WelcomePage::class.java).apply {
                                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                            }
                                                            context.startActivity(intent)
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "Failed to save user data locally.", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Failed to fetch user data from cloud.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            ?.onFailure { exception ->
                                Toast.makeText(context, "reCAPTCHA failed: ${exception.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }, modifier = Modifier.weight(1f))
                ButtonClicked("Exit", {
                    val activity = (context as? Activity)
                    activity?.finishAffinity()
                    exitProcess(0)
                }, modifier = Modifier.weight(1f))

            }
        }
    }
}
