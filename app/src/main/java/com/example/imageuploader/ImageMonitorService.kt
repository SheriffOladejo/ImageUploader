package com.example.imageuploader

import TelegramClient
import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.FileObserver
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException


private val storage = Firebase.storage


class ImageMonitorService : Service() {
    private var directoryPath = ""
    private lateinit var fileObserver: FileObserver

    private lateinit var dbHelper: DbHelper

    private val NOTIFICATION_ID = 256
    private val NOTIFICATION_CHANNEL_ID = "IMAGE_MONITOR_CHANNEL_ID"

    val ACCOUNT_SID = "ACcb831ae210b3295bcce7b7ebb2f21e38"
    val AUTH_TOKEN = "58889b7bc48d0f7a80790c7fd18a8977"

    override fun onCreate() {
        super.onCreate()
        dbHelper = DbHelper(applicationContext)
        directoryPath = dbHelper.getServiceStatus()
        if (directoryPath.isEmpty()) {
            directoryPath = "/storage/emulated/0/Pictures/Screenshot"
        }
        startFileObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_NOTIFICATION") {
            stopServiceAndNotification()
        } else {
            val notification = createForegroundNotification()
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }


    override fun onDestroy() {
        stopFileObserver()
        super.onDestroy()
    }

    private fun createForegroundNotification(): Notification {
        val notificationTitle = "Image Monitor Service"
        val notificationText = "Service is running"

        val notificationChannelId = NOTIFICATION_CHANNEL_ID

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                "Image Monitor Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, StartMonitorService::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        println("created notification")

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentIntent(pendingIntent)
            .build()
    }


    private fun startFileObserver() {
        println("directory path: $directoryPath")
        fileObserver = object : FileObserver(directoryPath, CREATE) {
            override fun onEvent(event: Int, path: String?) {
                if (event and CREATE == CREATE) {
                    Log.d("ImageMonitorService", "File event: $event for path: $path")
                    val newImagePath = File(directoryPath, path.toString())
                    uploadImageToFirebase(newImagePath = newImagePath)
                }
            }
        }
        fileObserver.startWatching()
    }

    private val storage = Firebase.storage

    fun removeMiddleWhitespace(input: String): String {
        // Use regular expression to match and replace whitespace in the middle
        return input.replace(Regex("\\s+"), " ")
    }

    private fun uploadImageToFirebase(newImagePath: File) {

        val storageRef = storage.reference.child("rayban/${newImagePath.name}")

        val uploadTask = storageRef.putFile(Uri.fromFile(newImagePath))

        uploadTask.addOnSuccessListener { taskSnapshot ->
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                Log.d("ImageMonitorService", "Image uploaded. URL: $imageUrl")
                var contactList: ArrayList<ContactItem> = dbHelper.getAllContacts()
                //val url = "https://graph.facebook.com/v17.0/158591567331641/messages"
                if (contactList.isEmpty()) {
                    Toast.makeText(applicationContext, "No recipient selected", Toast.LENGTH_LONG).show()
                }
                for (x in contactList) {
//                    val client = TelegramClient("837463392")
//                    client.sendPhoto(imageUrl)
                    val ACCOUNT_SID = "ACcb831ae210b3295bcce7b7ebb2f21e38"
                    val AUTH_TOKEN = "58889b7bc48d0f7a80790c7fd18a8977"
                    val EXCLAMATION_MARK = "!"

                    println("saved contacts2: ${x.phoneNumber.replace("\\s".toRegex(), "")}")

                    val fromNumber = "whatsapp:+14155238886"
                    val toNumber = "whatsapp:${x.phoneNumber.replace("\\s".toRegex(), "")}"
                    val message = "Hello there$EXCLAMATION_MARK"

                    val client = OkHttpClient()

                    val url = "https://api.twilio.com/2010-04-01/Accounts/$ACCOUNT_SID/Messages.json"
                    val mediaType = "application/x-www-form-urlencoded".toMediaType()

                    val requestBody = FormBody.Builder()
                        .add("From", fromNumber)
                        .add("To", toNumber)
                        .add("MediaUrl", imageUrl)
                        .build()

                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .header("Authorization", Credentials.basic(ACCOUNT_SID, AUTH_TOKEN))
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            println("Request failed: ${e.message}")
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) {
                                val responseBody = response.body?.string()
                                println("Response: $responseBody")
                            } else {
                                println("Request failed with code: ${response.code} ${response.body?.string()}")
                            }
                        }
                    })


                    val jsonData = """
    {
        "messaging_product": "whatsapp",
        "to": "9779826301645",
        "type": "template",
        "template": {"name": "hello_world", "language": {"code": "en_US" }}
    }
"""

                    /*
                    *                     val jsonData = """
    {
        "messaging_product": "whatsapp",
        "to": "${x.phoneNumber}",
        "type": "template",
        "template": {"name": "hello_world", "language": {"code": "en_US" }}
    }
                    * */
                    //postJsonDataToUrl(jsonData, url)
                }
            }.addOnFailureListener { exception ->
                Log.e("ImageMonitorService", "Error getting download URL: ${exception.message}")
            }
        }.addOnFailureListener { exception ->
            Log.e("ImageMonitorService", "Error uploading image: ${exception.message}")
        }
    }

    fun postJsonDataToUrl(jsonData: String, url: String) {
        val client = OkHttpClient()

        val mediaType = "application/json; charset=utf-8".toMediaType()

        val requestBody = jsonData.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer EAACPQFNzqpABOylLMrfZB9uMYBQjkTpvPMjti7U4pqs0dpZCRvr9KHbrQ4McSxIoZByNbhHi7pGkddtDPjhp0kxZC1F5Ec6ZA5Lk5tSzSpknZBKZC8F0LJC6QsZB7H7QdqOKfxV4d4OROO5WJbvrSFITYoupPOpIaZB5RjZAYOhMXOjORLEuIZCb8LyQQTHZARNC5wukvxVenedJGHauhTsZD")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Whatsapp Request failed1: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    println("Whatsapp Response: $responseBody")
                } else {
                    println("Whatsapp Request failed2: ${response.code}")
                }
            }
        })
    }

    private fun stopFileObserver() {
        fileObserver.stopWatching()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    fun stopServiceAndNotification() {
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

}
