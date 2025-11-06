package com.example.bankloginsystem

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
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
import android.database.sqlite.SQLiteDatabase // using SQLite to store user data
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast
import androidx.compose.ui.text.input.VisualTransformation
import java.security.MessageDigest // For 'HASHing values of password
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
    val initialBalance = 500

    // Error states for each field to display inline error messages.
    val firstNameError = remember { mutableStateOf("") }
    val lastNameError = remember { mutableStateOf("") }
    val emailError = remember { mutableStateOf("") }
    val passwordError = remember { mutableStateOf("") }
    val confirmError = remember { mutableStateOf("") }
    val phoneError = remember { mutableStateOf("") }
    val genderError = remember { mutableStateOf("") }

    // LocalContext is used for creating Intents to navigate between activities.
    val context = LocalContext.current

    val passwordVisible = remember { mutableStateOf(false) }
    val confirmPasswordVisible = remember { mutableStateOf(false) }


    // The main layout is a scrollable Column to accommodate all input fields on any screen size.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Create Account", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        // --- First Name ---
        OutlinedTextField(value = firstName.value, onValueChange = { firstName.value = it; firstNameError.value = "" }, label = { Text(text = "First Name") }, isError = firstNameError.value.isNotEmpty())
        if (firstNameError.value.isNotEmpty()) Text(firstNameError.value, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(12.dp))

        // --- Last Name ---
        OutlinedTextField(value = lastName.value, onValueChange = { lastName.value = it; lastNameError.value = "" }, label = { Text(text = "Last Name") }, isError = lastNameError.value.isNotEmpty())
        if (lastNameError.value.isNotEmpty()) Text(lastNameError.value, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(12.dp))

        // --- Gender Selection ---
        GenderSelection(selectedOption = gender.value, onOptionSelected = { gender.value = it; genderError.value = "" })
        if (genderError.value.isNotEmpty()) Text(genderError.value, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(12.dp))

        // --- Email ---
        OutlinedTextField(value = email.value, onValueChange = { email.value = it; emailError.value = "" }, label = { Text(text = "Email") }, isError = emailError.value.isNotEmpty())
        if (emailError.value.isNotEmpty()) Text(emailError.value, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(12.dp))

        // Password field with "Show/Hide"
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                    Text(if (passwordVisible.value) "Hide" else "Show")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm Password field with "Show/Hide"
        OutlinedTextField(
            value = confirmPassword.value,
            onValueChange = { confirmPassword.value = it },
            label = { Text("Confirm Password") },
            visualTransformation = if (confirmPasswordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { confirmPasswordVisible.value = !confirmPasswordVisible.value }) {
                    Text(if (confirmPasswordVisible.value) "Hide" else "Show")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(modifier = Modifier.height(12.dp))

        // --- Phone Number ---
        OutlinedTextField(value = phoneNumber.value, onValueChange = { phoneNumber.value = it; phoneError.value = "" }, label = { Text("Phone Number") }, isError = phoneError.value.isNotEmpty())
        if (phoneError.value.isNotEmpty()) Text(phoneError.value, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // The Button for submitting the form
        Button(onClick = {
            // This flag tracks if any validation check has failed.
            var validationFailed = false

            val fName = firstName.value.trim()
            val lName = lastName.value.trim()
            val gChoice = gender.value.trim()
            val mail = email.value.trim()
            val pass = password.value
            val conf = confirmPassword.value
            val phone = phoneNumber.value.trim()

            // --- Start Validation: Replaced Toasts with inline error states ---
            // Each validation now sets a specific error message on failure,
            // which is displayed below the corresponding input field.
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

            // If any validation failed, stop the submission process.
            if (validationFailed) return@Button
            // --- End Validation ---

            val dbHelper = DatabaseHelper(context)
            // Check for existing user after passing all other validations.
            if (dbHelper.userExists(mail)) {
                emailError.value = "Email already registered"
                return@Button
            }

            // If all checks pass, proceed with user insertion.
            val okay = dbHelper.insertUser(fName, lName, gChoice, mail, hashPassword(pass), phone, initialBalance.toDouble())
            if (okay){
                Toast.makeText(context, "User added successfully", Toast.LENGTH_LONG).show()
                val intent = Intent(context, LoginPage::class.java)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Error adding user", Toast.LENGTH_LONG).show()
            }

        }) { // end of onClick
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
        Text(text = "Gender", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp), fontSize = 20.sp)
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


// Creating the database for the entries for each new user
// Makes DatabaseHelper a concrete class that you can create instances of.
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // Defining the database structure
    companion object {
        private const val DATABASE_NAME = "BankUsers.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_USERS = "users"
        const val COLUMN_ID = "id"
        const val COLUMN_FIRST_NAME = "first_name"
        const val COLUMN_LAST_NAME = "last_name"
        const val COLUMN_GENDER = "gender"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password" // Newly added; Will be stored as a HASH
        const val COLUMN_PHONE_NUMBER = "phone_number"
        const val COLUMN_BALANCE = "balance"
    }

    // Creating the table
    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE_USERS (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_FIRST_NAME TEXT,
            $COLUMN_LAST_NAME TEXT,
            $COLUMN_GENDER TEXT,
            $COLUMN_EMAIL TEXT,
            $COLUMN_PASSWORD TEXT, 
            $COLUMN_PHONE_NUMBER TEXT,
            $COLUMN_BALANCE REAL
            );
        """.trimIndent()
        db?.execSQL(createTable)
    }

    // Handle database upgrades if needed; Suggested by online resource
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // Inserting the added users; Returns true if successful, false otherwise in case of validations.
    fun insertUser(
        firstName: String,
        lastName: String,
        gender: String,
        email: String,
        password: String,
        phoneNumber: String,
        balance: Double
    ) : Boolean {
        val db = this.writableDatabase
        return try {
            val values = ContentValues().apply {
                put(COLUMN_FIRST_NAME, firstName)
                put(COLUMN_LAST_NAME, lastName)
                put(COLUMN_GENDER, gender)
                put(COLUMN_EMAIL, email)
                put(COLUMN_PASSWORD, password)
                put(COLUMN_PHONE_NUMBER, phoneNumber)
                put(COLUMN_BALANCE, balance)
            }
            val id = db.insert(TABLE_USERS, null, values)
            id != -1L
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.close()
        }
    }


    // Checking if user exists in the database
    /**
     * Checks if a user with the given email already exists in the database.
     *
     * @param email The email to check.
     * @return True if the user exists, false otherwise.
     */
    fun userExists(email: String) : Boolean {
        val db = this.readableDatabase
        var cursor: Cursor? = null
        return try {
            cursor = db.rawQuery("SELECT $COLUMN_ID FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?", arrayOf(email))
            val exists = cursor.count > 0
            exists
        } finally {
            cursor?.close()
            db.close() // Better to close later
        }
    }


//     Get user details by email (example utility for login later)
    fun getUserByEmail(email: String): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?", arrayOf(email))
        // Caller must close cursor
    }


    // Singleton accessor
//    private var instance: DatabaseHelper? = null
//    fun getInstance(context: Context): DatabaseHelper {
//        if (instance == null) {
//            instance = DatabaseHelper(context.applicationContext)
//        }
//        return instance!!
//    }


}

// The password the user will create will be added to the database but will be stored as a HASH (SHA-256); Usually used in security management
/**
 * Hashes a password using the SHA-256 algorithm.
 * This is a one-way hashing function, so the original password cannot be recovered.
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
