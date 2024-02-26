package com.example.msonedrive

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class SplashActivity : AppCompatActivity() {

    private lateinit var publicClientApplication: ISingleAccountPublicClientApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

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

        Handler().postDelayed(Runnable {checkIfUserLoggedIn()},1000)

    }

//    private fun displayActivity() {
//        checkIfUserLoggedIn()
//    }

    private fun checkIfUserLoggedIn() {
        publicClientApplication?.getCurrentAccountAsync(object :
            ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                if (activeAccount != null) {
                    Log.d(ContentValues.TAG, "User is already logged in: ${activeAccount.username}")
                    val intent = Intent(this@SplashActivity, FilePickerActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@SplashActivity, "User is not Signed In", Toast.LENGTH_SHORT)
                        .show()
                    startActivity(Intent(this@SplashActivity,MainActivity::class.java))
                    finish()
                }
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                //Need to handle the account changes
            }

            override fun onError(exception: MsalException) {
                Log.d(ContentValues.TAG, "Error checking login status: ${exception.message}")
            }

        })
    }

    fun generateBase64EncodedSHA1(input: String): String {
        try {
            val info = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hash = Base64.encodeToString(
                    md.digest(),
                    Base64.DEFAULT
                )
                Log.d("KeyHash", "KeyHash:$hash")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("KeyHash", e.toString())
        } catch (e: NoSuchAlgorithmException) {
        }
        // Convert the input string to bytes
        val bytes = input.toByteArray()

        // Compute the SHA1 hash
        val sha1Digest = MessageDigest.getInstance("SHA-1").digest(bytes)

        // Base64 encode the SHA1 hash
        return Base64.encodeToString(sha1Digest, Base64.NO_WRAP)
    }
}