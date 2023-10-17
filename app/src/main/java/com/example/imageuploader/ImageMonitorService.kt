package com.example.imageuploader

import android.app.*
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.net.Uri
import android.os.FileObserver
import android.os.IBinder
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException

private val storage = Firebase.storage


class ImageMonitorService : Service() {
    private var directoryPath = ""
    private lateinit var fileObserver: FileObserver

    private lateinit var dbHelper: DbHelper

    private val NOTIFICATION_ID = 256
    private val NOTIFICATION_CHANNEL_ID = "IMAGE_MONITOR_CHANNEL_ID"

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
    private fun uploadImageToFirebase(newImagePath: File) {
        val storageRef = storage.reference.child("rayban/${newImagePath.name}")

        val uploadTask = storageRef.putFile(Uri.fromFile(newImagePath))

        uploadTask.addOnSuccessListener { taskSnapshot ->
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                Log.d("ImageMonitorService", "Image uploaded. URL: $imageUrl")
                var contactList: ArrayList<ContactItem> = dbHelper.getAllContacts()
                val url = "https://graph.facebook.com/v18.0/310179018297056/messages"
                if (contactList.isEmpty()) {
                    Toast.makeText(applicationContext, "No recipient selected", Toast.LENGTH_LONG).show()
                }
                for (x in contactList) {
                    println("saved contacts " + x.phoneNumber)
                    val jsonData = """
    {
        "messaging_product": "whatsapp",
        "to": "${x.phoneNumber}",
        "type": "image",
        "image": {"link": "$imageUrl"}
    }
"""
                    postJsonDataToUrl(jsonData, url)
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
            .addHeader("Authorization", "Bearer EAACPQFNzqpABO2PZAStayIfGNYhZBAZBCtaAl4xVx4S8LCPBmZAZAO6B4AZCnfcDvoxZBHr3WJlXmwNZCWlUFF9IdeIu89PnFEr6x4JVCHT9XellzZCwZBn0tYKtBIFRFtDGaFweB05sssU6moWYDhurfwREhLrBtcTdbmtcUj8RpZAehKpIZCbLTyZCHxjpXIRqDnaCwFOrl5cjLBfLfGajGX08ZD")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Whatsapp Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    println("Whatsapp Response: $responseBody")
                } else {
                    println("Whatsapp Request failed: ${response.message}")
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
