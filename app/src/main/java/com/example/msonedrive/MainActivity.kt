package com.example.msonedrive

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {

    private lateinit var publicClientApplication: ISingleAccountPublicClientApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //checkIfUserLoggedIn()
        setContentView(R.layout.activity_main)

        PublicClientApplication.createSingleAccountPublicClientApplication(
            applicationContext, R.raw.auth_config_single_account,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    publicClientApplication = application

                }

                override fun onError(exception: MsalException) {
                    // Handle error
                }
            }
        )

        findViewById<CardView>(R.id.btnSignIn).setOnClickListener {
            publicClientApplication.signIn(
                this,
                null,
                arrayOf("Files.ReadWrite", "User.Read"),
                object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {

                        //shared preferences to store the accessToken of the user once he successfully signedIn into the application.........
                        val userAccessToken = authenticationResult.accessToken
//                        val account = authenticationResult.account
//                        val userRefreshToken = account.idToken

                        val sharedPreference = this@MainActivity.getSharedPreferences(
                            "PREFERENCE_NAME",
                            Context.MODE_PRIVATE
                        )
                        var editor = sharedPreference.edit()

                        editor.putString("userAccessToken", userAccessToken)
                       // editor.putString("userRefreshToken",userRefreshToken)
                        editor.apply()
                        editor.commit()

                        val intent =
                            Intent(this@MainActivity, FilePickerActivity::class.java)
                        // putExtra("accessToken", authenticationResult.accessToken)
                        startActivity(intent)
                    }

                    override fun onError(exception: MsalException) {
                        // Handle error
                        Toast.makeText(this@MainActivity,"$exception", Toast.LENGTH_SHORT)
                            .show()
                        println("exception:$exception")
                        recreate()

                    }

                    override fun onCancel() {
                        // Handle cancellation
                    }
                })
        }

    }

}