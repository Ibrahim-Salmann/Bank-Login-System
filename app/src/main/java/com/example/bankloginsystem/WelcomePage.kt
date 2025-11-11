package com.example.bankloginsystem

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme

class WelcomePage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Change: Initialize UserSessionManager to retrieve session data.
        val userSessionManager = UserSessionManager(this)
        // Change: Check if the user is logged in. If not, redirect to LoginPage.
        if (!userSessionManager.isLoggedIn()) {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Change: Fetch user details from the session.
        val userDetails = userSessionManager.getUserDetails()
        val fullName = userDetails[UserSessionManager.PREF_FULL_NAME] ?: ""
        val email = userDetails[UserSessionManager.PREF_EMAIL] ?: ""
//        val id = userDetails[UserSessionManager.PREF_USER_ID] ?: ""


//        // Retrieving the user's full name when logging in
//        val firstName = intent.getStringExtra("first_name") ?: ""
//        val lastName = intent.getStringExtra("last_name") ?: ""
//        val fullName = "$firstName $lastName".trim()
//        val userBalance = intent.getDoubleExtra("balance", 0.0)
//        val email = intent.getStringExtra("email") ?: ""

        // Change: Fetch the user's balance from the database using the email from the session.
        val dbHelper = DatabaseHelper(this)
        val cursor = dbHelper.getUserByEmail(email)
        var userBalance = 0.0
        if (cursor.moveToFirst()) {
            userBalance = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BALANCE))
        }
        cursor.close()
        dbHelper.close()


        setContent {
            BankLoginSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    WelcomePageScreen(
                        modifier = Modifier.padding(padding),
                        fullName = fullName,
                        balance = userBalance
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomePageScreen(
    modifier: Modifier = Modifier,
    fullName: String = " ",
    balance: Double = 0.0
) {
    val context = LocalContext.current
    // Change: Initialize UserSessionManager for logout functionality.
    val userSessionManager = UserSessionManager(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display name
        if (fullName.isNotEmpty()) {
            Text(text = "Welcome, $fullName!", modifier = Modifier.padding(bottom = 24.dp))
        } else {
            Text(text = "Welcome!", modifier = Modifier.padding(bottom = 24.dp))
        }

        Text(text = "Current Balance: $$balance")

        Spacer(modifier = Modifier.height(24.dp))

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        )
        {
            Button(onClick = {
                // The Intent now only needs to specify the destination.
                // WithdrawPage will get the user's email from the session itself.
                val intent = Intent(context, WithdrawPage::class.java)
                context.startActivity(intent)
            }) {
                Text("Withdraw")
            }

            Button(onClick = {
                // The Intent now only needs to specify the destination.
                // DepositPage will get the user's email from the session itself.
                val intent = Intent(context, DepositPage::class.java)
                context.startActivity(intent)
            }) {
                Text("Deposit")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))


        OutlinedButton(onClick = {
            Toast.makeText(context, "Welcome to your book shelf!", Toast.LENGTH_SHORT).show()
            val intent = Intent(context, BookShelfPage::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }) { Text("My Book Shelf")}

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = {
            // Change: Log out the user by clearing the session.
            userSessionManager.logoutUser()
            val intent = Intent(context, LoginPage::class.java)
            // Change: Add flags to clear the back stack and prevent returning to this page.
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
            (context as? Activity)?.finish() // optional: prevents back navigation
        }) {
            Text("Logout")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomePageScreenPreview() {
    BankLoginSystemTheme {
        WelcomePageScreen()
    }
}
