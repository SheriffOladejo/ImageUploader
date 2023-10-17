package com.example.imageuploader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.contact_selection.*

class ContactSelection : AppCompatActivity() {

    private lateinit var listView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private val contactList = ArrayList<ContactItem>()
    private lateinit var dbHelper: DbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contact_selection)

        listView = findViewById(R.id.listView)
        listView.setHasFixedSize(true)
        listView.setLayoutManager(LinearLayoutManager(this))
        contactAdapter = ContactAdapter(this, contactList)
        listView.setAdapter(contactAdapter)

        dbHelper = DbHelper(applicationContext)


    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 123)
        } else {
            showProgressBar()
            loadContacts()
        }
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_contact_selection, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isEmpty()) {
                    contactAdapter = ContactAdapter(applicationContext, contactList)
                    listView.setAdapter(contactAdapter)
                }
                else {
                    filterContacts(query)
                }

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    if (newText.isEmpty()) {
                        contactAdapter = ContactAdapter(applicationContext, contactList)
                        listView.setAdapter(contactAdapter)
                    }
                }
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    private fun filterContacts(query: String) {
        val filteredList = contactList.filter { contact ->
            contact.name.contains(query, ignoreCase = true) ||
                    contact.phoneNumber.contains(query)
        }
        println("filtered list size: ${filteredList.size}")
        contactAdapter = ContactAdapter(this, ArrayList(filteredList))
        listView.setAdapter(contactAdapter)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                // Handle the search icon click if needed
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showProgressBar()
            loadContacts()
        }
    }

    private fun loadContacts() {
        Toast.makeText(applicationContext, "Loading phone contacts", Toast.LENGTH_LONG).show()
        val selectedContacts = dbHelper.getAllContacts()

        Handler().postDelayed({
            val cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI, null, null, null, null
            )
            while (cursor?.moveToNext() == true) {
                val contactId =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                val contactName =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                val hasPhoneNumber =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)).toInt()

                if (hasPhoneNumber > 0) {
                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(contactId),
                        null
                    )

                    phoneCursor?.moveToFirst()
                    val phoneNumber = phoneCursor?.getString(
                        phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    ) ?: ""

                    var isSelected  = false
                    for (x in selectedContacts) {
                        if (x.id == contactId) {
                            isSelected = true
                        }
                    }

                    contactList.add(ContactItem(contactId, contactName, phoneNumber, isSelected))
                    contactAdapter.notifyDataSetChanged()
                    phoneCursor?.close()
                }
            }
            cursor?.close()
            hideProgressBar()
        },1000)
}

}