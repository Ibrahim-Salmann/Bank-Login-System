package com.example.bankloginsystem

import android.app.Application
import android.util.Log
import com.google.android.recaptcha.Recaptcha
import com.google.android.recaptcha.RecaptchaClient
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {

    lateinit var recaptchaClient: RecaptchaClient
    private val recaptchaScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Firebase App Check
        val providerFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(providerFactory)

        // Initialize reCAPTCHA
        initializeRecaptchaClient()
    }

    private fun initializeRecaptchaClient() {
        recaptchaScope.launch {
            Recaptcha.getClient(this@App, "6LdnuhYsAAAAAEPdTgmP1L_1-zsqahDRwbiW737z")
                .onSuccess { client ->
                    recaptchaClient = client
                }
                .onFailure { exception ->
                    Log.e("App", "reCAPTCHA client initialization failed", exception)
                }
        }
    }
}
