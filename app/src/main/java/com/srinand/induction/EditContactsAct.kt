package com.srinand.induction

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import androidx.cardview.widget.CardView

class EditContactsAct : AppCompatActivity() {

    private lateinit var contactsRecyclerView: RecyclerView
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var noContactsText: TextView
    private lateinit var recyclerView: RecyclerView
    private var contactsList: MutableList<String> = mutableListOf()

    private lateinit var locationText: TextView
    private lateinit var setDefaultLocationButton: CardView

    private val pickContactLauncher =
        registerForActivityResult(ActivityResultContracts.PickContact()) { contactUri: Uri? ->
            contactUri?.let {
                try {
                    val cursor = contentResolver.query(contactUri, null, null, null, null)
                    cursor?.apply {
                        if (moveToFirst()) {
                            val nameColumnIndex = getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                            val contactName = if (nameColumnIndex != -1) getString(nameColumnIndex) else null

                            val contactIdColumnIndex = getColumnIndex(ContactsContract.Contacts._ID)
                            val contactId = if (contactIdColumnIndex != -1) getString(contactIdColumnIndex) else null

                            if (contactName != null && contactId != null) {
                                val phoneCursor = contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    arrayOf(contactId),
                                    null
                                )
                                phoneCursor?.use { phoneCursor ->
                                    if (phoneCursor.moveToFirst()) {
                                        val phoneNumberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                        val phoneNumber = if (phoneNumberIndex != -1) phoneCursor.getString(phoneNumberIndex) else null
                                        if (phoneNumber != null) {
                                            addContactToEmergencyList(contactName, phoneNumber)
                                        } else {
                                            Toast.makeText(this@EditContactsAct, "Phone number not available", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(this@EditContactsAct, "Failed to retrieve contact information", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@EditContactsAct, "Failed to retrieve contact information", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_contacts)

        // Initialize views
        locationText = findViewById(R.id.locationText)
        setDefaultLocationButton = findViewById(R.id.cardSetLocation)

        recyclerView = findViewById(R.id.contactsRecyclerView)
        noContactsText = findViewById(R.id.noContactsText)
        updateUI()

        // Retrieve location data from SharedPreferences
        val sharedPreferencesLocation = getSharedPreferences("SelectedLocation", MODE_PRIVATE)
        val latitude = sharedPreferencesLocation.getFloat("latitude", 0.0f).toDouble()
        val longitude = sharedPreferencesLocation.getFloat("longitude", 0.0f).toDouble()

        if (latitude != 0.0 && longitude != 0.0) {
            // If valid location data is available, display it
            locationText.text = "Default Location:\nLat=$latitude, Lon=$longitude"
        } else {
            locationText.text = "No valid location data found"
            Toast.makeText(this, "No location data available", Toast.LENGTH_SHORT).show()
        }

        // Initialize SharedPreferences for contacts
        sharedPreferences = getSharedPreferences("emergency_contacts", MODE_PRIVATE)

        // Set up RecyclerView
        contactsRecyclerView = findViewById(R.id.contactsRecyclerView)
        contactsRecyclerView.layoutManager = LinearLayoutManager(this)
        contactsAdapter = ContactsAdapter(contactsList) { contact ->
            deleteContact(contact)
        }
        contactsRecyclerView.adapter = contactsAdapter

        // Set Default Location Button
        setDefaultLocationButton.setOnClickListener {
            val intent = Intent(this, LocationPickerActivity::class.java)
            startActivity(intent)
        }

        // Pick Contact Button
        val btnPickContact: CardView = findViewById(R.id.btnPickContact)
        btnPickContact.setOnClickListener {
            pickContactLauncher.launch(null)
        }

        loadContacts()
        updateUI()
    }

    private fun addContactToEmergencyList(contactName: String, phoneNumber: String) {
        updateUI()  // Update UI state
        val contacts = sharedPreferences.getStringSet("emergency_contacts", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        val contactEntry = "$contactName: $phoneNumber"
        if (contacts.add(contactEntry)) {
            sharedPreferences.edit().putStringSet("emergency_contacts", contacts).apply()
            contactsList.add(contactEntry)  // Adding to the in-memory list
            contactsAdapter.notifyItemInserted(contactsList.size - 1)

            Toast.makeText(this, "Contact added: $contactEntry", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Contact already exists", Toast.LENGTH_SHORT).show()
        }
        updateUI()
    }


    private fun deleteContact(contact: String) {
        updateUI()
        if (contactsList.remove(contact)) {
            contactsAdapter.notifyDataSetChanged()

            sharedPreferences.edit().putStringSet("emergency_contacts", contactsList.toSet()).apply()
            Toast.makeText(this, "$contact deleted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to delete contact", Toast.LENGTH_SHORT).show()
        }
        updateUI()
    }

    private fun updateUI() {
        if (contactsList.isEmpty()) {
            noContactsText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noContactsText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun loadContacts() {
        val savedContacts = sharedPreferences.getStringSet("emergency_contacts", mutableSetOf()) ?: mutableSetOf()
        contactsList.clear()
        contactsList.addAll(savedContacts)
        contactsAdapter.notifyDataSetChanged()
        updateUI()  // Update UI after the contacts are loaded
    }

    override fun onBackPressed() {
        updateUI()
        val resultIntent = Intent(this@EditContactsAct, MainActivity::class.java)
        resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(resultIntent)
        finish()
    }
}
