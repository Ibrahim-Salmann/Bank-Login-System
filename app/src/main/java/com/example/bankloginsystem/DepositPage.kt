package com.example.bankloginsystem

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme

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

@Composable
fun DepositPageScreen(modifier: Modifier = Modifier){
    val context = LocalContext.current
    val dbHelper = DatabaseHelper(context)
    // Change: Initialize UserSessionManager to retrieve session data.
    val userSessionManager = UserSessionManager(context)

    val depositAmount = remember { mutableStateOf("") }
    val depositAmountError = remember { mutableStateOf("") }

    Column(modifier = modifier
        .fillMaxSize()
        .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally)
    {
        Text(text = "How much would you like to deposit?")

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = depositAmount.value,
            onValueChange = { depositAmount.value = it },
            label = { Text(text = " Enter amount ") },
            isError = depositAmountError.value.isNotEmpty(),
            supportingText = { Text(text = depositAmountError.value) }
        )

        if (depositAmountError.value.isNotEmpty()) {
            Text(depositAmountError.value,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        ButtonClicked("Submit", {
            val amount = depositAmount.value.trim()

            // Change: Fetch user's email from the session.
            val userDetails = userSessionManager.getUserDetails()
            val email = userDetails[UserSessionManager.PREF_EMAIL]

//            /*
//             * BUG FIX: The context was being cast to WithdrawPage, which caused a
//             * ClassCastException and crashed the app.
//             *
//             * CHANGE: The context is now correctly cast to DepositPage to retrieve the
//             * intent extras.
//             */
//            val email = (context as DepositPage).intent.getStringExtra("email")
//            val firstName = context.intent.getStringExtra("first_name") ?: ""
//            val lastName = context.intent.getStringExtra("last_name") ?: ""

            // Change: Check if email is null or empty. If so, the user is not logged in.
            if (email.isNullOrEmpty()) {
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

                    /**
                     * BUG FIX: The previous logic incorrectly checked if the deposit amount was less
                     * than the current balance, which is meant for withdrawals. This prevented
                     * the deposit from ever being processed.
                     *
                     * CHANGE: The incorrect conditional check has been removed. The new balance is
                     * now calculated by adding the deposit amount to the current balance, and the
                     * database is updated accordingly.
                     */
                    val newBalance = currentBalance + amountN

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
                        Toast.makeText(context, "Deposit successful!", Toast.LENGTH_SHORT)
                            .show()

                        // Change: No longer need to pass data via intent.
                        // Send updated balance to WelcomePage
                        val intent = Intent(context, WelcomePage::class.java).apply {
//                            putExtra("email", email)
//                            putExtra("first_name", firstName)
//                            putExtra("last_name", lastName)
//                            putExtra("balance", newBalance)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Failed to update balance.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                /**
                 * CHANGE: The error message in the catch block has been updated to be more
                 * specific to the deposit operation, changing from "withdrawal" to "deposit".
                 */
                Toast.makeText(context, "Error processing deposit.", Toast.LENGTH_SHORT).show()
            } finally {
                cursor?.close()
                db.close()
            }
        })

    }
}

@Preview(showBackground = true)
@Composable
fun DepositPagePreview() {
    BankLoginSystemTheme {
        DepositPageScreen()
    }
}
