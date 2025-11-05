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
            // Add validation + save to SQLite
            // simple synchronous DB call (fine for a learning app with small data)
            val dbHelper = DatabaseHelper(context)

            val fName = firstName.value.trim()
            val lName = lastName.value.trim()
            val gChoice = gender.value.trim()
            val mail = email.value.trim()
            val pass = password.value // note: not saving password in this schema; // TODO MUST needed
            val conf = confirmPassword.value
            val phone = phoneNumber.value.trim()
            val initialBalance = 500

            // Validation: Checking if text fields are empty
            if (fName.isEmpty() || lName.isEmpty() || gChoice.isEmpty() || mail.isEmpty() || pass.isEmpty() || conf.isEmpty() || phone.isEmpty()) {
                // The pop-up message
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                // Handle empty fields
                return@Button
            }

            // Validation: Checking if an email is already taken or registered
            if (dbHelper.userExists(mail)) {
                Toast.makeText(context, "Email already registered", Toast.LENGTH_LONG).show()
                return@Button
            }



            // TODO() More validations



            // User is inserted
            val okay = dbHelper.insertUser(fName, lName, gChoice, mail, phone, initialBalance.toDouble())
            if (okay){
                Toast.makeText(context, "User added successfully", Toast.LENGTH_LONG).show()
                // Navigate to the login page
                val intent = Intent(context, LoginPage::class.java)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Error adding user", Toast.LENGTH_LONG).show()
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
    fun insertUser(firstName: String, lastName: String, gender: String, email: String, phoneNumber: String, balance: Double) : Boolean {
        val db = this.writableDatabase
        return try {
            val values = ContentValues().apply {
                put(COLUMN_FIRST_NAME, firstName)
                put(COLUMN_LAST_NAME, lastName)
                put(COLUMN_GENDER, gender)
                put(COLUMN_EMAIL, email)
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
    fun userExists(email: String) : Boolean {
        val db = this.readableDatabase
        var cursor: Cursor? = null
        return try {
            cursor = db.rawQuery("SELECT $COLUMN_ID FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?", arrayOf(email))
            val exists = cursor.count > 0
            exists
        } finally {
            cursor?.close()
            db.close()
        }
    }


    // Get user details by email (example utility for login later)
    fun getUserByEmail(email: String): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?", arrayOf(email))
        // Caller must close cursor
        // Suggested by online resource
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
