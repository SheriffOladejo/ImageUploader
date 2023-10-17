package com.example.imageuploader

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ndhunju.folderpicker.SampleActivity
import com.ndhunju.folderpicker.library.FolderPickerDialogFragment
import com.ndhunju.folderpicker.library.OnDialogBtnClickedListener
import kotlinx.android.synthetic.main.activity_start_monitor_service.*



class StartMonitorService : AppCompatActivity(), OnDialogBtnClickedListener {

    private lateinit var dbHelper: DbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_monitor_service)

        dbHelper = DbHelper(applicationContext)

        checkAndRequestPermissions()

        path.setOnClickListener{
            val fpdf = FolderPickerDialogFragment.newInstance(
                    null,
            REQUEST_CODE_DIR
            )
            fpdf.show(getSupportFragmentManager(), TAG)
        }

        val isServiceRunning = isServiceRunning(applicationContext, ImageMonitorService::class.java)

        if (isServiceRunning) {
            start_service.visibility = View.INVISIBLE
            stop_service.visibility = View.VISIBLE
        }
        else {
            stop_service.visibility = View.INVISIBLE
            start_service.visibility = View.VISIBLE
        }

        var status = dbHelper.getServiceStatus()
        if (status.isEmpty()) {
            status = "/storage/emulated/0/Pictures/Screenshot"
        }
        path.setText(status)

        start_service.setOnClickListener{
            checkAndRequestPermissions()
            val serviceIntent = Intent(applicationContext, ImageMonitorService::class.java)
            startService(serviceIntent)
            start_service.visibility = View.INVISIBLE
            stop_service.visibility = View.VISIBLE
            val p = dbHelper.getServiceStatus()
            println("path: ${path.text.toString()}")
            dbHelper.insertServiceStatus(path.text.toString())
        }

        stop_service.setOnClickListener{
            val serviceIntent = Intent(applicationContext, ImageMonitorService::class.java)
            stopService(serviceIntent)

            val stopNotificationIntent = Intent(applicationContext, ImageMonitorService::class.java)
            stopNotificationIntent.action = "STOP_NOTIFICATION"
            applicationContext.startService(stopNotificationIntent)
            stop_service.visibility = View.INVISIBLE
            start_service.visibility = View.VISIBLE
        }

    }

    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = manager.getRunningServices(Int.MAX_VALUE)

        for (service in services) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }

        return false
    }

    private val REQUEST_PERMISSIONS_CODE = 123

    private val permissionsToRequest = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.INTERNET,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.MANAGE_EXTERNAL_STORAGE,
    )


    private fun allPermissionsGranted(): Boolean {
        for (permission in permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requestPermissionsIfNecessary() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_PERMISSIONS_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    // The permission has been granted.
                } else {
                    // The permission has been denied.
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (allPermissionsGranted()) {

        } else {
            requestPermissionsIfNecessary()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_contacts, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_contacts -> {
                val intent = Intent(this, ContactSelection::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private val REQUEST_CODE_DIR = 1
    private val TAG = StartMonitorService::class.java.simpleName

    override fun onDialogBtnClicked(data: Intent?, whichBtn: Int, result: Int, requestCode: Int) {
        when (requestCode) {
            REQUEST_CODE_DIR -> {
                if (result != RESULT_OK) return
                // Get the selected folder path through intent
                val selectedFolderDir =
                    data!!.getStringExtra(FolderPickerDialogFragment.KEY_CURRENT_DIR)
                Toast.makeText(baseContext, selectedFolderDir, Toast.LENGTH_LONG).show()
                path.setText(selectedFolderDir)
            }
        }
    }


}