package com.example.bankloginsystem

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme
import com.example.bankloginsystem.ui.theme.ScanLines
import com.google.firebase.auth.FirebaseAuth

/**
 * The `DepositPage` activity provides a user interface for depositing funds into the user's account.
 * It handles the transaction logic, including input validation and updating the user's balance
 * in both the local SQLite database and the Firebase Realtime Database.
 */
class DepositPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BankLoginSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    DepositPageScreen(modifier = Modifier.padding(padding))
                }
            }
        }
    }
}

/**
 * The main composable for the deposit screen.
 * It manages the state of the input field, validates the user's input, and processes the deposit transaction.
 */
@Composable
fun DepositPageScreen(modifier: Modifier = Modifier){
    val context = LocalContext.current
    val dbHelper = DatabaseHelper(context)
    val userSessionManager = UserSessionManager(context)
    val firebaseManager = remember { FirebaseManager() }
    val firebaseAuth = FirebaseAuth.getInstance()

    val depositAmount = remember { mutableStateOf("") }
    val depositAmountError = remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        ScanLines()
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally)
        {
            Text(text = "How much would you like to deposit?")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = depositAmount.value,
                onValueChange = { newText -> depositAmount.value = newText.filter { it.isDigit() } },
                label = { Text(text = " Enter amount ") },
                isError = depositAmountError.value.isNotEmpty(),
                supportingText = { Text(text = depositAmountError.value) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            if (depositAmountError.value.isNotEmpty()) {
                Text(depositAmountError.value,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            ButtonClicked("Submit", {
                val amount = depositAmount.value.trim()

                val userDetails = userSessionManager.getUserDetails()
                val email = userDetails[UserSessionManager.PREF_EMAIL]
                val firebaseUserId = firebaseAuth.currentUser?.uid

                if (email.isNullOrEmpty() || firebaseUserId == null) {
                    Toast.makeText(context, "Error: User not logged in", Toast.LENGTH_SHORT).show()
                    return@ButtonClicked
                }

                if (amount.isEmpty()) {
                    depositAmountError.value = "Please enter an amount."
                    return@ButtonClicked
                }

                val amountN = amount.toDoubleOrNull()
                if (amountN == null || amountN <= 0) {
                    depositAmountError.value = "Enter a valid positive number."
                    return@ButtonClicked
                }

                depositAmountError.value = ""

                var cursor: Cursor? = null
                val db = dbHelper.readableDatabase

                try {
                    cursor = db.rawQuery(
                        "SELECT ${DatabaseHelper.COLUMN_BALANCE} FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_EMAIL} = ?",
                        arrayOf(email)
                    )

                    if (cursor.moveToFirst()) {
                        val currentBalance = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BALANCE))
                        val newBalance = currentBalance + amountN

                        // 1. Update the balance in the local SQLite database.
                        val writableDb = dbHelper.writableDatabase
                        val values = ContentValues().apply {
                            put(DatabaseHelper.COLUMN_BALANCE, newBalance)
                        }
                        val rowsUpdated = writableDb.update(
                            DatabaseHelper.TABLE_USERS,
                            values,
                            "${DatabaseHelper.COLUMN_EMAIL} = ?",
                            arrayOf(email)
                        )

                        if (rowsUpdated > 0) {
                            // 2. If local update is successful, update Firebase Realtime Database.
                            firebaseManager.updateBalance(firebaseUserId, newBalance) { success ->
                                if (success) {
                                    Toast.makeText(context, "Deposit successful!", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Notify user if Firebase sync fails, but proceed since local DB is updated.
                                    Toast.makeText(context, "Deposit successful (cloud sync failed).", Toast.LENGTH_LONG).show()
                                }
                                // Navigate back to WelcomePage regardless of Firebase sync status.
                                val intent = Intent(context, WelcomePage::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                context.startActivity(intent)
                            }
                        } else {
                            Toast.makeText(context, "Failed to update balance.", Toast.LENGTH_SHORT).show()
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error processing deposit.", Toast.LENGTH_SHORT).show()
                } finally {
                    cursor?.close()
                    db.close()
                }
            })

            Spacer(modifier = Modifier.height(24.dp))

            ButtonClicked("Return", {
                val intent = Intent(context, WelcomePage::class.java)
                context.startActivity(intent)
            })

        }
    }
}

@Preview(showBackground = true)
@Composable
fun DepositPagePreview() {
    BankLoginSystemTheme {
        DepositPageScreen()
    }
}
