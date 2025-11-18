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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme
import com.example.bankloginsystem.ui.theme.ScanLines

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

@Composable
fun WithdrawPageScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dbHelper = DatabaseHelper(context)
    // Change: Initialize UserSessionManager to retrieve session data.
    val userSessionManager = UserSessionManager(context)

    val withdrawnAmount = remember { mutableStateOf("") }
    val withdrawnAmountError = remember { mutableStateOf("") }

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

                // Change: Fetch user's email from the session.
                val userDetails = userSessionManager.getUserDetails()
                val email = userDetails[UserSessionManager.PREF_EMAIL]

                // Change: Check if email is null or empty. If so, the user is not logged in.
                if (email.isNullOrEmpty()) {
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
                                Toast.makeText(context, "Withdrawal successful!", Toast.LENGTH_SHORT)
                                    .show()

                                // Change: No longer need to pass data via intent.
                                // Send updated balance to WelcomePage
                                val intent = Intent(context, WelcomePage::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Failed to update balance.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error processing withdrawal.", Toast.LENGTH_SHORT).show()
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
fun WithdrawPagePreview() {
    BankLoginSystemTheme {
        WithdrawPageScreen()
    }
}
