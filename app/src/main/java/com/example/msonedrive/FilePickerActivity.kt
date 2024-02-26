package com.example.msonedrive

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.JsonPrimitive
import com.microsoft.graph.concurrency.ICallback
import com.microsoft.graph.core.ClientException
import com.microsoft.graph.models.extensions.DriveItem
import com.microsoft.graph.models.extensions.Folder
import com.microsoft.graph.models.extensions.IGraphServiceClient
import com.microsoft.graph.requests.extensions.GraphServiceClient
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Date

class FilePickerActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnUpload: ImageView
    private val requestCodePickFile = 100
    private lateinit var txtLogout: TextView
    private lateinit var publicClientApplication: ISingleAccountPublicClientApplication
    private lateinit var sharedPreference: SharedPreferences
    private lateinit var btnDownload: Button
    private lateinit var etSubFolderName:TextInputEditText
    private lateinit var etSubFolderL:TextInputLayout
    private lateinit var txtSubFolderName:String
    private lateinit var btn: Button

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_picker)
        sharedPreference = getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
        val accessToken = sharedPreference.getString("userAccessToken", null)
        btnUpload = findViewById(R.id.btnUpload)
        listView = findViewById(R.id.listViewFiles)
        btn = findViewById(R.id.btnSync)
        txtLogout = findViewById(R.id.txtLogout)
        btnDownload = findViewById(R.id.btnDownload)

        etSubFolderName = findViewById(R.id.etFolderName)
        etSubFolderL = findViewById(R.id.textFolderInputLayout)

        listFiles()

        //Initializing Microsoft identity client.........
        PublicClientApplication.createSingleAccountPublicClientApplication(
            applicationContext, R.raw.auth_config_single_account,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    publicClientApplication = application
                }

                override fun onError(exception: MsalException) {
                    Log.e("Exception occurred", "Exception:$exception")
                }
            }
        )

        //Button View to pick files from the device internal storage.......
        btnUpload.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, requestCodePickFile)
        }

        //button view to invoke the function that uploads the files into the user's one drive....
        btn.setOnClickListener {
            // val accessToken = intent.getStringExtra("accessToken") // Retrieve the access token
            println("accessToken:$accessToken")
            txtSubFolderName = etSubFolderName.text.toString().trim()
            if (txtSubFolderName.isEmpty()){
                Toast.makeText(this@FilePickerActivity,"Enter Folder Name",Toast.LENGTH_SHORT).show()
            }else if (accessToken != null && !isTokenExpired(accessToken)) {
                uploadAllFilesToOneDrive(accessToken,txtSubFolderName)
            } else {
                Toast.makeText(
                    this@FilePickerActivity,
                    "Session timed out. Signing out the user.",
                    Toast.LENGTH_SHORT
                ).show()
                signOut()
            }
        }

        btnDownload.setOnClickListener {
            downloadAllFilesFromOneDrive()
        }

        txtLogout.setOnClickListener {
            signOut()
        }
    }


    private fun signOut() {
        publicClientApplication.signOut(object :
            ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                val preferences = getSharedPreferences("userAccessToken", MODE_PRIVATE)
                val editor = preferences.edit()
                editor.clear()
                editor.apply()
                // Handle successful sign-out
                Toast.makeText(
                    this@FilePickerActivity,
                    "Logged out successfully",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this@FilePickerActivity, MainActivity::class.java))
                finishAffinity()
            }

            override fun onError(exception: MsalException) {
                // Handle error
                Toast.makeText(
                    this@FilePickerActivity,
                    "Error logging out: ${exception.message}",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodePickFile && resultCode == Activity.RESULT_OK) {
            val selectedFileUri = data?.data
            println("selectedFileUri:$selectedFileUri")
            selectedFileUri?.let {
                saveFileToLocalAppStorage(it)
            }
        }
    }

//    private fun downloadAllFilesFromOneDrive() {
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val accessToken =
//                    sharedPreference.getString("userAccessToken", null) ?: return@launch
//                val client: IGraphServiceClient =
//                    GraphServiceClient.builder().authenticationProvider { request ->
//                        request.addHeader("Authorization", "Bearer $accessToken")
//                    }.buildClient()
//
//                val folderPath = "ReNoteAI" // Your OneDrive folder path here
//                val driveItems =
//                    client.me().drive().root().itemWithPath(folderPath).children().buildRequest()
//                        .get()
//
//                driveItems.currentPage.forEach { driveItem ->
//                    val itemName = driveItem.name
//                    val itemStream =
//                        client.me().drive().items(driveItem.id).content().buildRequest().get()
//
//                    val outputFile = File(filesDir, itemName)
//                    FileOutputStream(outputFile).use { outputStream ->
//                        itemStream.copyTo(outputStream)
//                    }
//                }
//
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        applicationContext,
//                        "Files downloaded successfully",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    listFiles() // Refresh the list to display downloaded files
//                }
//                uploadAllFilesToOneDrive(accessToken,txtSubFolderName)
//            } catch (e: Exception) {
//                Log.e("DownloadError", "Error downloading files: ${e.message}")
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        applicationContext,
//                        "Failed to download files",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//    }

    private fun downloadAllFilesFromOneDrive() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val accessToken = sharedPreference.getString("userAccessToken", null) ?: return@launch
                val client: IGraphServiceClient = GraphServiceClient.builder().authenticationProvider { request ->
                    request.addHeader("Authorization", "Bearer $accessToken")
                }.buildClient()

                val folderPath = "ReNoteAI" // Your OneDrive folder path here
                downloadFolderContents(client, folderPath)

                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Files downloaded successfully", Toast.LENGTH_SHORT).show()
                    listFiles() // Refresh the list to display downloaded files
                }
            } catch (e: Exception) {
                Log.e("DownloadError", "Error downloading files: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Failed to download files", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun downloadFolderContents(client: IGraphServiceClient, folderPath: String) {
        val driveItems = client.me().drive().root().itemWithPath(folderPath).children().buildRequest().get()
        driveItems.currentPage.forEach { driveItem ->
            if (driveItem.folder != null) {
                // It's a folder, recurse into it
                val subFolderPath = "$folderPath/${driveItem.name}"
                downloadFolderContents(client, subFolderPath)
            } else {
                // It's a file, download it
                val itemName = driveItem.name
                val itemStream = client.me().drive().items(driveItem.id).content().buildRequest().get()

                val outputFile = File(filesDir, itemName)
                FileOutputStream(outputFile).use { outputStream ->
                    itemStream.copyTo(outputStream)
                }
            }
        }
    }

    private fun saveFileToLocalAppStorage(fileUri: Uri) {
        val inputStream = contentResolver.openInputStream(fileUri)
        inputStream?.let {
            // Retrieve the actual file name
            val fileName = getFileName(fileUri) ?: "unknownFile"
            val newFile = File(filesDir, fileName)
            FileOutputStream(newFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            listFiles() // Refreshing the list
        }
    }

    private fun getFileName(fileUri: Uri): String? {
        var fileName: String? = null
        val cursor = contentResolver.query(fileUri, null, null, null, null)
        cursor?.let {
            if (it.moveToFirst()) {
                fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
            it.close()
        }
        return fileName
    }

    private fun listFiles() {
        val filesList = filesDir.listFiles()?.toList() ?: listOf()

        // List all files and filter out "rList" and "profileInstalled"
//        val filesList = filesDir.listFiles()?.filterNot {
//            it.name == "rList" || it.name == "profileInstalled"
//        }?.toList() ?: listOf()
        val adapter = FileListAdapter(this, filesList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val file = filesList[position]
            openFile(file)
            println("file:$file")
        }
    }

    private fun openFile(file: File) {
        val uri =
            FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
        println("uri:$file")
        println("URI2:$uri")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, contentResolver.getType(uri))
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(Intent.createChooser(intent, "Open File"))
    }

    private fun uploadAllFilesToOneDrive(accessToken: String,folderName:String) {
        val files =
            filesDir.listFiles() // Getting all files in our app's internal storage directory

        files?.let { fileList ->
            lifecycleScope.launch(Dispatchers.IO) {
                fileList.forEach { file ->
                    try {
                        val inputStream = FileInputStream(file)
                        val fileContent =
                            inputStream.readBytes() // Reading file content into a ByteArray as the put method accepts the input stream data only in byte arrays.

                        val client: IGraphServiceClient =
                            GraphServiceClient.builder().authenticationProvider { request ->
                                request.addHeader("Authorization", "Bearer $accessToken")
                            }.buildClient()

                        val folderPath = "ReNoteAI/$folderName" // Specify the MainFolder OneDrive
                        val filePath = "$folderPath/${file.name}"

                        client.me().drive().root().itemWithPath(filePath)
                            .content().buildRequest()
                            .put(fileContent, object : ICallback<DriveItem> {
                                override fun success(result: DriveItem) {
                                    Log.d(
                                        "UploadSuccess",
                                        "File uploaded successfully: ${file.name}"
                                    )
                                    showOnMainThread("File Uploaded")
                                    saveFileMapping(file.name, result.id)
                                }

                                override fun failure(ex: ClientException) {
                                    Log.e(
                                        "UploadError",
                                        "Error uploading file: ${file.name}, ${ex.message}"
                                    )
//                                    Toast.makeText(
//                                        this@FilePickerActivity,
//                                        "Failed:Check your Internet connectivity",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
                                }
                            })

                        inputStream.close()
                    } catch (e: Exception) {
                        Log.e("UploadException", "Exception during file upload: ${e.message}")
//                        Toast.makeText(
//                            this@FilePickerActivity,
//                            "Failed:Check your Internet connectivity",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "Syncing Initiated",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showOnMainThread(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(this@FilePickerActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFileMapping(localFileName: String, oneDriveFileId: String) {
        val sharedPreferences = getSharedPreferences("FileMappings", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(localFileName, oneDriveFileId).apply()
    }

    fun getOneDriveFileId(localFileName: String): String? {
        val sharedPreferences = getSharedPreferences("FileMappings", Context.MODE_PRIVATE)
        return sharedPreferences.getString(localFileName, null)
    }

    fun deleteFileFromOneDrive(oneDriveFileId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                sharedPreference = getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
                val accessToken =
                    sharedPreference.getString("userAccessToken", "defaultAccessToken")
                accessToken?.let {
                    val client: IGraphServiceClient =
                        GraphServiceClient.builder().authenticationProvider { request ->
                            request.addHeader("Authorization", "Bearer $accessToken")
                        }.buildClient()

                    client.me().drive().items(oneDriveFileId).buildRequest().delete()

                    showOnMainThread("File deleted from OneDrive")
                }
            } catch (e: Exception) {
                Log.e("DeletionError", "Error deleting file from OneDrive: ${e.message}")
            }
        }
    }

    private fun isTokenExpired(token: String): Boolean {
        // Decode the token (assuming it's a JWT)
        val tokenParts = token.split(".")
        if (tokenParts.size < 2) {
            // Invalid token format
            return true
        }

        // Decode the payload (second part of the token)
        val decodedPayload = android.util.Base64.decode(tokenParts[1], android.util.Base64.DEFAULT)
        val payloadString = String(decodedPayload)

        // Extract expiration time from the payload
        val payloadJson = JSONObject(payloadString)
        val expirationTimeSeconds = payloadJson.optLong("exp", -1)

        // Check if expiration time is valid
        if (expirationTimeSeconds == -1L) {
            // Expiration time not found in the token
            return true
        }

        // Convert expiration time to Date object
        val expirationDate = Date(expirationTimeSeconds * 1000)

        // Compare with current time
        val currentTime = Date()
        return currentTime.after(expirationDate)
    }

}
