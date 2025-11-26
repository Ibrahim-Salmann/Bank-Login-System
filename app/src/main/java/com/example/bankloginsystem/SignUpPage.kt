package com.example.bankloginsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme
import com.example.bankloginsystem.ui.theme.ScanLines
import com.google.android.recaptcha.RecaptchaAction
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * The SignUpPage activity hosts the user registration screen.
 * It uses Jetpack Compose to build the UI and handles the logic for creating a new user account.
 * This activity is responsible for:
 * - Displaying the sign-up form.
 * - Validating user input.
 * - Creating a new user in Firebase Authentication.
 * - Inserting the new user into the local SQLite database and Firebase Realtime Database.
 * - Navigating to the LoginPage upon successful registration.
 */
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
    val initialBalance = 500

    // Error states for each field to display inline error messages.
    val firstNameError = remember { mutableStateOf("") }
    val lastNameError = remember { mutableStateOf("") }
    val emailError = remember { mutableStateOf("") }
    val passwordError = remember { mutableStateOf("") }
    val confirmError = remember { mutableStateOf("") }
    val phoneError = remember { mutableStateOf("") }
    val genderError = remember { mutableStateOf("") }

    // Get the application context for database operations and navigation.
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    val passwordVisible = remember { mutableStateOf(false) }
    val confirmPasswordVisible = remember { mutableStateOf(false) }
    val firebaseAuth = if (!isInPreview) FirebaseAuth.getInstance() else null
    val firebaseManager = remember { if (!isInPreview) FirebaseManager() else null }
    val app = if (!isInPreview) context.applicationContext as App else null
    val recaptchaScope = CoroutineScope(Dispatchers.Main)


    Box(modifier = modifier.fillMaxSize()) {
        ScanLines()
        // The main layout is a scrollable Column to accommodate all input fields on any screen size.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Create Account", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(24.dp))

            // --- Input Fields with Validation ---
            OutlinedTextField(value = firstName.value, onValueChange = { firstName.value = it; firstNameError.value = "" }, label = { Text(text = "First Name") }, isError = firstNameError.value.isNotEmpty())
            if (firstNameError.value.isNotEmpty()) Text(firstNameError.value, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = lastName.value, onValueChange = { lastName.value = it; lastNameError.value = "" }, label = { Text(text = "Last Name") }, isError = lastNameError.value.isNotEmpty())
            if (lastNameError.value.isNotEmpty()) Text(lastNameError.value, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(12.dp))

            GenderSelection(selectedOption = gender.value, onOptionSelected = { gender.value = it; genderError.value = "" })
            if (genderError.value.isNotEmpty()) Text(genderError.value, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = email.value, onValueChange = { email.value = it; emailError.value = "" }, label = { Text(text = "Email") }, isError = emailError.value.isNotEmpty())
            if (emailError.value.isNotEmpty()) Text(emailError.value, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(12.dp))

            // Password field with a button to toggle visibility.
            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it; passwordError.value = ""},
                label = { Text("Password") },
                isError = passwordError.value.isNotEmpty(),
                visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                        Text(if (passwordVisible.value) "Hide" else "Show")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (passwordError.value.isNotEmpty()) Text( passwordError.value, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword.value,
                onValueChange = { confirmPassword.value = it; confirmError.value = "" },
                label = { Text("Confirm Password") },
                isError = confirmError.value.isNotEmpty(),
                visualTransformation = if (confirmPasswordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { confirmPasswordVisible.value = !confirmPasswordVisible.value }) {
                        Text(if (confirmPasswordVisible.value) "Hide" else "Show")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (confirmError.value.isNotEmpty()) Text(confirmError.value, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)


            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = phoneNumber.value, onValueChange = { newText -> phoneNumber.value = newText.filter { it.isDigit() }; phoneError.value = "" },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                label = { Text("Phone Number") }, isError = phoneError.value.isNotEmpty())
            if (phoneError.value.isNotEmpty()) Text(phoneError.value, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // The Button for submitting the form, which triggers reCAPTCHA and the registration process.
            Button(onClick = {
                recaptchaScope.launch {
                    app?.recaptchaClient?.execute(RecaptchaAction.SIGNUP)
                        ?.onSuccess {
                            var validationFailed = false

                            val fName = firstName.value.trim()
                            val lName = lastName.value.trim()
                            val gChoice = gender.value.trim()
                            val mail = email.value.trim()
                            val pass = password.value
                            val conf = confirmPassword.value
                            val phone = phoneNumber.value.trim()

                            // --- Input Validation ---
                            if (fName.isEmpty()) {
                                firstNameError.value = "First name is required"
                                validationFailed = true
                            }

                            if (lName.isEmpty()) {
                                lastNameError.value = "Last name is required"
                                validationFailed = true
                            }

                            if (gChoice.isEmpty()) {
                                genderError.value = "Please select a gender"
                                validationFailed = true
                            }

                            if (mail.isEmpty()) {
                                emailError.value = "Email is required"
                                validationFailed = true
                            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
                                emailError.value = "Please enter a valid email address"
                                validationFailed = true
                            }

                            if (pass.isEmpty()) {
                                passwordError.value = "Password is required"
                                validationFailed = true
                            } else if (pass.length <= 9) {
                                passwordError.value = "Password must be at least 9 characters long"
                                validationFailed = true
                            }

                            if (conf.isEmpty()) {
                                confirmError.value = "Please confirm your password"
                                validationFailed = true
                            } else if (pass != conf) {
                                confirmError.value = "Passwords do not match"
                                validationFailed = true
                            }

                            if (phone.isEmpty()) {
                                phoneError.value = "Phone number is required"
                                validationFailed = true
                            } else if (phone.length !in 11..<14) {
                                phoneError.value = "Please enter a valid phone number (10–13 digits)"
                                validationFailed = true
                            }

                            if (validationFailed) return@onSuccess

                            // --- Database Operations ---
                            val dbHelper = DatabaseHelper(context)
                            if (dbHelper.userExists(mail)) {
                                emailError.value = "Email already registered"
                                return@onSuccess
                            }

                            // Create the user in Firebase Authentication.
                            firebaseAuth?.createUserWithEmailAndPassword(mail, pass)
                                ?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val firebaseUser = firebaseAuth.currentUser
                                        val uid = firebaseUser?.uid

                                        if (uid != null) {
                                            // Save user to the local SQLite database.
                                            val okay = dbHelper.insertUser(fName, lName, gChoice, mail, hashPassword(pass), phone, initialBalance.toDouble())
                                            if (okay) {
                                                // Save user to Firebase Realtime Database.
                                                firebaseManager?.saveUser(uid, "$fName $lName", mail)

                                                Toast.makeText(context, "User added successfully", Toast.LENGTH_LONG).show()
                                                val intent = Intent(context, LoginPage::class.java)
                                                context.startActivity(intent)
                                            } else {
                                                // If creating the user in the local database fails, delete the user from Firebase to avoid inconsistency.
                                                firebaseUser.delete().addOnCompleteListener { deleteTask ->
                                                    if (deleteTask.isSuccessful) {
                                                        Toast.makeText(context, "Error adding user to local database. Please try again.", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        Toast.makeText(context, "Critical error: user created in Firebase but not locally. Please contact support.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Error getting user ID", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }

                        }
                        ?.onFailure { exception ->
                            Toast.makeText(context, "reCAPTCHA failed: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                }
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
        Text(text = "Gender", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp), fontSize = 20.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { text ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .selectable(
                            selected = (text == selectedOption),
                            onClick = { onOptionSelected(text) }
                        )
                        .padding(horizontal = 4.dp, vertical = 8.dp)
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
 * Hashes a password using the SHA-256 algorithm.
 * This is a one-way hashing function, so the original password cannot be recovered.
 * Storing hashed passwords is a critical security measure.
 *
 * @param password The password to hash.
 * @return The hashed password as a hex string.
 */
fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
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
