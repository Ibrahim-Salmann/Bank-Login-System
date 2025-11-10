package com.example.bankloginsystem

import android.app.Activity
import android.content.Intent
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
        var firstName = ""
        var lastName = ""
        if (cursor.moveToFirst()) {
            userBalance = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BALANCE))
            // Change: Also fetching first and last name to pass to other activities if needed.
            firstName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FIRST_NAME))
            lastName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LAST_NAME))
        }
        cursor.close()
        dbHelper.close()


        setContent {
            BankLoginSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    WelcomePageScreen(
                        modifier = Modifier.padding(padding),
                        fullName = fullName,
                        balance = userBalance,
                        email = email,
                        firstName = firstName,
                        lastName = lastName
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
    balance: Double = 0.0,
    email: String = "",
    firstName: String = "",
    lastName: String = ""
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
                val intent = Intent(context, WithdrawPage::class.java).apply {
                    putExtra("email", email)
                    putExtra("first_name", firstName)
                    putExtra("last_name", lastName)
                }
                context.startActivity(intent)
            }) {
                Text("Withdraw")
            }

            Button(onClick = {
                val intent = Intent(context, DepositPage::class.java).apply {
                    putExtra("email", email)
                    putExtra("first_name", firstName)
                    putExtra("last_name", lastName)
                }
                context.startActivity(intent)
            }) {
                Text("Deposit")
            }
        }

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
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomePageScreenPreview() {
    BankLoginSystemTheme {
        WelcomePageScreen()
    }
}
