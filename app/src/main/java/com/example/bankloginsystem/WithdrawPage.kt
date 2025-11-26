package com.example.bankloginsystem

import android.app.Activity
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme
import com.example.bankloginsystem.ui.theme.ScanLines
import com.google.firebase.auth.FirebaseAuth

/**
 * The `WithdrawPage` activity provides a user interface for withdrawing funds from the user's account.
 * It ensures that the user is logged in and handles the transaction logic, including input validation
 * and updating the user's balance in both the local SQLite database and the Firebase Realtime Database.
 */
class WithdrawPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BankLoginSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    WithdrawPageScreen(
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

/**
 * The main composable for the withdrawal screen.
 * It manages the state of the input field, validates the user's input, and processes the withdrawal transaction.
 */
@Composable
fun WithdrawPageScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val dbHelper = DatabaseHelper(context)
    val userSessionManager = UserSessionManager(context)
    val firebaseManager = remember { if (!isInPreview) FirebaseManager() else null }
    val firebaseAuth = remember { if (!isInPreview) FirebaseAuth.getInstance() else null }

    val withdrawnAmount = remember { mutableStateOf("") }
    val withdrawnAmountError = remember { mutableStateOf("") }
    val showSuccessDialog = remember { mutableStateOf(false) }

    if (showSuccessDialog.value) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog.value = false },
            title = { Text("Success") },
            text = { Text("Your withdrawal was successful.") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog.value = false
                    val intent = Intent(context, WelcomePage::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                }) {
                    Text("Go to Welcome Page")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        ScanLines()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "How much would you like to withdraw?")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = withdrawnAmount.value,
                onValueChange = { newText -> withdrawnAmount.value = newText.filter { it.isDigit() } },
                label = { Text(text = " Enter amount ") },
                isError = withdrawnAmountError.value.isNotEmpty(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (withdrawnAmountError.value.isNotEmpty()) {
                Text(
                    withdrawnAmountError.value,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ButtonClicked("Submit", {
                val amount = withdrawnAmount.value.trim()

                val userDetails = userSessionManager.getUserDetails()
                val email = userDetails[UserSessionManager.PREF_EMAIL]
                val firebaseUserId = firebaseAuth?.currentUser?.uid

                if (email.isNullOrEmpty() || firebaseUserId == null) {
                    Toast.makeText(context, "Error: User not logged in", Toast.LENGTH_SHORT).show()
                    return@ButtonClicked
                }

                if (amount.isEmpty()) {
                    withdrawnAmountError.value = "Please enter an amount."
                    return@ButtonClicked
                }

                val amountN = amount.toDoubleOrNull()
                if (amountN == null || amountN <= 0) {
                    withdrawnAmountError.value = "Enter a valid positive number."
                    return@ButtonClicked
                }

                withdrawnAmountError.value = ""

                var cursor: Cursor? = null
                val db = dbHelper.readableDatabase
                try {
                    cursor = db.rawQuery(
                        "SELECT ${DatabaseHelper.COLUMN_BALANCE} FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_EMAIL} = ?",
                        arrayOf(email)
                    )

                    if (cursor.moveToFirst()) {
                        val currentBalance = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BALANCE))

                        if (amountN > currentBalance) {
                            Toast.makeText(context, "Insufficient funds.", Toast.LENGTH_SHORT).show()
                        } else {
                            val newBalance = currentBalance - amountN

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
                                firebaseManager?.updateBalance(firebaseUserId, newBalance) { _ ->
                                    (context as? Activity)?.runOnUiThread {
                                        showSuccessDialog.value = true
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Failed to update balance.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error processing withdrawal.", Toast.LENGTH_SHORT).show()
                } finally {
                    cursor?.close()
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
fun WithdrawPagePreview() {
    BankLoginSystemTheme {
        WithdrawPageScreen()
    }
}
