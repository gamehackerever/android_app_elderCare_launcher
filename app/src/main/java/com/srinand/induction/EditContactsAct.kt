package com.srinand.induction

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson

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
            locationText.text = """
        Fallback Location for SOS Messages:
        Lat=$latitude, Lon=$longitude
        (Used when current location is unavailable or GPS is off)
    """.trimIndent()
        } else {
            locationText.text = """
        No Fallback Location Set:
        SOS messages may lack location info if GPS is off
    """.trimIndent()
            Toast.makeText(this, "No fallback location available. Please set a default location.", Toast.LENGTH_SHORT).show()
        }


        // Initialize SharedPreferences for contacts
        sharedPreferences = getSharedPreferences("emergency_contacts_map", MODE_PRIVATE)

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
        val contactsMap = loadContactsFromPreferences().toMutableMap()

        // Check if the contact already exists
        if (contactsMap.containsKey(contactName)) {
            Toast.makeText(this, "Contact already exists: $contactName", Toast.LENGTH_SHORT).show()
        } else {
            // Add new contact
            contactsMap[contactName] = phoneNumber
            saveContactsToPreferences(contactsMap)
            contactsList.add("$contactName: $phoneNumber")
            contactsAdapter.notifyItemInserted(contactsList.size - 1)
            Toast.makeText(this, "Contact added: $contactName", Toast.LENGTH_SHORT).show()
        }
        updateUI()
    }


    private fun deleteContact(contact: String) {
        val contactsMap = loadContactsFromPreferences().toMutableMap()

        // Extract name from "name: phone"
        val contactName = contact.substringBefore(":").trim()

        if (contactsMap.remove(contactName) != null) {
            saveContactsToPreferences(contactsMap)
            contactsList.remove(contact)
            contactsAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Contact deleted: $contactName", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to delete contact: $contactName", Toast.LENGTH_SHORT).show()
        }
        updateUI()
    }

    private fun loadContacts() {
        val contactsMap = loadContactsFromPreferences()
        contactsList.clear()
        contactsMap.forEach { (name, phone) ->
            contactsList.add("$name: $phone")
        }
        contactsAdapter.notifyDataSetChanged()
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

    private fun loadContactsFromPreferences(): Map<String, String> {
        val jsonContacts = sharedPreferences.getString("emergency_contacts_map", "{}") ?: "{}"
        return Gson().fromJson(jsonContacts, object : TypeToken<Map<String, String>>() {}.type)
    }

    private fun saveContactsToPreferences(contactsMap: Map<String, String>) {
        val gson = Gson()
        val jsonContacts = gson.toJson(contactsMap)
        sharedPreferences.edit().putString("emergency_contacts_map", jsonContacts).apply()
        Log.d("EditContactsAct", "Contacts saved: $jsonContacts")
    }

    override fun onBackPressed() {
        updateUI()
        val resultIntent = Intent(this@EditContactsAct, MainActivity::class.java)
        resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(resultIntent)
        finish()
    }
}
