package com.example.bankloginsystem

import android.app.Activity
import android.content.ContentValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bankloginsystem.ui.theme.BankLoginSystemTheme
import com.example.bankloginsystem.ui.theme.ScanLines
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.perf.FirebasePerformance

/**
 * The `WelcomePage` is the main screen users see after logging in.
 * It greets the user, displays their current account balance, and provides navigation to other features
 * like withdrawing/depositing funds, managing their bookshelf, and logging out.
 *
 * This activity ensures that only logged-in users can access it, redirecting to the `LoginPage` if no session is found.
 */
class WelcomePage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userSessionManager = UserSessionManager(this)
        if (!userSessionManager.isLoggedIn()) {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
            finish()
            return
        }

        val userDetails = userSessionManager.getUserDetails()
        val fullName = userDetails[UserSessionManager.PREF_FULL_NAME] ?: ""
        val email = userDetails[UserSessionManager.PREF_EMAIL] ?: ""

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
                        initialBalance = userBalance
                    )
                }
            }
        }
    }
}

/**
 * The main composable that builds the UI for the Welcome screen.
 * @param fullName The full name of the logged-in user.
 * @param initialBalance The starting account balance of the user, fetched from the local database.
 */
@Composable
fun WelcomePageScreen(
    modifier: Modifier = Modifier,
    fullName: String = " ",
    initialBalance: Double = 0.0
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val userSessionManager = remember { if (!isInPreview) UserSessionManager(context) else null }
    val firebaseManager = remember { if (!isInPreview) FirebaseManager() else null }
    val firebaseAuth = remember { if (!isInPreview) FirebaseAuth.getInstance() else null }
    val dbHelper = remember { if (!isInPreview) DatabaseHelper(context) else null }

    var balance by remember { mutableStateOf(initialBalance) }

    // This effect runs when the screen is first displayed. It fetches the latest balance from Firebase.
    if (!isInPreview) {
        LaunchedEffect(key1 = Unit) {
            val balanceFetchTrace = FirebasePerformance.getInstance().newTrace("balance_fetch_trace")
            balanceFetchTrace.start()
            val firebaseUserId = firebaseAuth?.currentUser?.uid
            if (firebaseUserId != null) {
                firebaseManager?.getUserBalance(firebaseUserId) { firebaseBalance ->
                    if (firebaseBalance != null && firebaseBalance != balance) {
                        balance = firebaseBalance

                        // Also, update the local SQLite database to keep it in sync.
                        val email = userSessionManager?.getUserDetails()?.get(UserSessionManager.PREF_EMAIL)
                        if (email != null) {
                            val values = ContentValues().apply {
                                put(DatabaseHelper.COLUMN_BALANCE, firebaseBalance)
                            }
                            dbHelper?.writableDatabase?.update(
                                DatabaseHelper.TABLE_USERS,
                                values,
                                "${DatabaseHelper.COLUMN_EMAIL} = ?",
                                arrayOf(email)
                            )
                        }
                    }
                    balanceFetchTrace.stop()
                }
            }
        }
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
                    val intent = Intent(context, WithdrawPage::class.java)
                    context.startActivity(intent)
                }) {
                    Text("Withdraw")
                }

                Button(onClick = {
                    val intent = Intent(context, DepositPage::class.java)
                    context.startActivity(intent)
                }) {
                    Text("Deposit")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(onClick = {
                Toast.makeText(context, "Welcome to your book shelf!", Toast.LENGTH_SHORT).show()
                val intent = Intent(context, BookShelfPage::class.java)
                context.startActivity(intent)
            }) { Text("My Book Shelf")}

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(onClick = {
                if (!isInPreview) {
                    FirebaseAuth.getInstance().signOut()
                    userSessionManager?.logoutUser()

                    val intent = Intent(context, LoginPage::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }
            }) {
                Text("Logout")
            }

            Spacer(modifier = Modifier.height(16.dp))
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
