package com.srinand.induction

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.telephony.gsm.SmsManager
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.util.*

class MainActivity : AppCompatActivity(), OnInitListener {
    private var isFlashlightOn = false
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var tts: TextToSpeech
    private lateinit var vibrator: Vibrator
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesHome: SharedPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val REQUEST_CODE_PERMISSION = 1001
    private var isCancelPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsIfNeeded() // Check and request permissions at the beginning
        setContentView(R.layout.home_page)

        enableImmersiveMode()

        sharedPreferencesHome = getSharedPreferences("app_preferences", MODE_PRIVATE)
        if (!sharedPreferencesHome.getBoolean("isDontAskAgain", false) || isDefaultHomeApp()) {
            val builder = AlertDialog.Builder(this)
                .setView(R.layout.dialog_set_launcher)
                .setCancelable(false)

            val dialog = builder.create()
            dialog.show()

            dialog.findViewById<TextView>(R.id.btnSetDefault)?.setOnClickListener {
                vibrate()
                openHomeSettings()
                dialog.dismiss()
            }

            dialog.findViewById<TextView>(R.id.btnNotNow)?.setOnClickListener {
                vibrate()
                dialog.dismiss()
            }

            dialog.findViewById<TextView>(R.id.btnDoNotAskAgain)?.setOnClickListener {
                vibrate()
                sharedPreferencesHome.edit().putBoolean("isDontAskAgain", true).apply()
                dialog.dismiss()
            }
        }

        val btnHotstar = findViewById<CardView>(R.id.btnHotstar)

        // Check if Hotstar app is installed
        if (isAppInstalled("com.hotstar.android") or isAppInstalled("in.startv.hotstar")) {
            // Show the Hotstar button if the app is installed
            btnHotstar.visibility = View.VISIBLE
        } else {
            // Hide the Hotstar button if the app is not installed
            btnHotstar.visibility = View.GONE
        }

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        sharedPreferences = getSharedPreferences("emergency_contacts_map", MODE_PRIVATE)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        tts = TextToSpeech(this, this)

        migrateLegacyContacts()

        // Check first launch
        val isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true)
        if (isFirstLaunch) {
            showFirstLaunchHint()
            sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()
        }

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0]

        findViewById<CardView>(R.id.cardCalls).setOnClickListener {
            vibrate()
            speakOut("Dialing phone")
            startActivity(Intent(Intent.ACTION_DIAL))
        }

        findViewById<CardView>(R.id.cardMessages).setOnClickListener {
            vibrate()
            speakOut("Opening WhatsApp")
            openWhatsApp()
        }

        findViewById<CardView>(R.id.cardCamera).setOnClickListener {
            vibrate()
            speakOut("Opening camera")
            openCameraApp()
        }

        findViewById<CardView>(R.id.flashlight).setOnClickListener {
            vibrate()
            toggleFlashlight(findViewById(R.id.flashlight))
        }

        // Set an OnClickListener for the SOS button
        findViewById<CardView>(R.id.sos).setOnClickListener {
            vibrateSOS()

            // Create a custom dialog to confirm SOS with countdown
            val builder = AlertDialog.Builder(this)
                .setView(R.layout.dialog_sos)  // Custom layout with countdown view
                .setCancelable(false)  // Disabling touch outside dialog to cancel

            val dialog = builder.create()
            dialog.show()

            val countdownText: TextView? = dialog.findViewById(R.id.countdown_text)
            val confirmButton: CardView? = dialog.findViewById(R.id.confirm_card)
            val cancelButton: CardView? = dialog.findViewById(R.id.cancel_card)

            // Set countdown starting time (10 seconds)
            var countdownTime = 10
            countdownText?.text = "$countdownTime seconds"  // Initial display of 10 seconds

            // Using CountDownTimer to update the countdown
            val countdownTimer =
                object : CountDownTimer(10000, 1000) {  // Updated timer duration to 10 seconds
                    override fun onTick(millisUntilFinished: Long) {
                        countdownTime = (millisUntilFinished / 1000).toInt()
                        countdownText?.text = "$countdownTime seconds"  // Update countdown display
                    }

                    override fun onFinish() {
                        // Timer finished, so we trigger the SOS activation
                        activateSOS()  // Trigger SOS action after countdown finishes
                        dialog.dismiss()  // Close the dialog
                        Toast.makeText(this@MainActivity, "SOS Activated", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

            countdownTimer.start()  // Start the countdown timer

            // Confirm SOS button click
            confirmButton?.setOnClickListener {
                vibrator.cancel()
                activateSOS()  // Trigger the actual SOS activation
                countdownTimer.cancel()  // Cancel the countdown timer
                dialog.dismiss()  // Close the dialog
            }

            // Cancel SOS button click
            cancelButton?.setOnClickListener {
                vibrator.cancel()
                isCancelPressed = true
                countdownTimer.cancel()  // Cancel the countdown timer
                dialog.dismiss()  // Close the dialog
                Toast.makeText(this@MainActivity, "SOS Cancelled", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<CardView>(R.id.sos).setOnLongClickListener {
            vibrate()
            speakOut("Editing Emergency Contact List")
            openEditContactsActivity()
            true
        }


        findViewById<CardView>(R.id.cardCalendar).setOnClickListener {
            vibrate()
            speakOut("Opening Calendar")
            openCalendar()
        }

        findViewById<CardView>(R.id.cardGallery).setOnClickListener {
            vibrate()
            speakOut("Opening Gallery")
            openGallery()
        }

        findViewById<CardView>(R.id.btnHotstar).setOnClickListener {
            vibrate()
            speakOut("Opening Hotstar")
            openHotstar()
        }

        // OnClick listener for YouTube
        findViewById<CardView>(R.id.cardYoutube).setOnClickListener {
            vibrate()
            speakOut("Opening YouTube")
            openYouTube()
        }

        // OnClick listener for Maps
        findViewById<CardView>(R.id.cardMaps).setOnClickListener {
            vibrate()
            speakOut("Opening Maps")
            openMaps()
        }

        findViewById<CardView>(R.id.chrome).setOnClickListener {
            vibrate()
            speakOut("Opening Chrome")
            openChrome()
        }

        findViewById<CardView>(R.id.cardCalculator).setOnClickListener {
            vibrate()
            speakOut("Opening Calculator")
            openCalculator()
        }

        findViewById<CardView>(R.id.cardSettings).setOnClickListener { v ->
            // Intent to navigate to Settings screen
            vibrate()
            speakOut("Opening Settings")
            val intent = Intent(Settings.ACTION_SETTINGS)  // This opens the general settings screen
            startActivity(intent)
        }
    }

    private fun enableImmersiveMode() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
    }

    private fun requestPermissionsIfNeeded() {
        // Check and request permissions
        val permissionsToRequest = mutableListOf<String>()

        // Contacts, SMS, and Location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSION)
        }
    }

    // Method to check if your app is the default home app
    private fun isDefaultHomeApp(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)

        // Resolve the activity and check if it's your app
        val currentHomePackage = intent.resolveActivity(packageManager)?.packageName

        return TextUtils.equals(currentHomePackage, packageName)
    }

    // Open the Home Settings screen for selecting the default home app
    private fun openHomeSettings() {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        startActivity(intent)
    }
    private fun showDefaultHomeDialog() {
        // Inflate the custom dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_launcher, null)

        // Create and configure dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Prevent accidental dismissal
            .create()

        // Access the dialog elements (change to TextViews for styled buttons)
        val btnSetDefault: TextView = dialogView.findViewById(R.id.btnSetDefault)
        val btnNotNow: TextView = dialogView.findViewById(R.id.btnNotNow)
        val btnDoNotAskAgain: TextView = dialogView.findViewById(R.id.btnDoNotAskAgain)

        // Set up "Set Default" action
        btnSetDefault.setOnClickListener {
            openHomeSettings() // Open default launcher settings
            dialog.dismiss()
        }

        // Set up "Not Now" action
        btnNotNow.setOnClickListener {
            dialog.dismiss()
        }

        // Set up "Do Not Ask Again" action
        btnDoNotAskAgain.setOnClickListener {
            // Save preference to SharedPreferences
            sharedPreferences.edit().putBoolean("isDontAskAgain", true).apply()
            dialog.dismiss()
        }

        dialog.show()
    }




    private fun showFirstLaunchHint() {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_first_launch, null)

        // Create the dialog and apply consistent style
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Handle button click
        val gotItButton: Button = dialogView.findViewById(R.id.gotItButton)
        gotItButton.setOnClickListener {
            dialog.dismiss() // Dismiss the dialog
        }

        // Show the dialog
        dialog.show()
    }



    @RequiresApi(Build.VERSION_CODES.Q)
    private fun activateSOS() {
        vibrator.vibrate(VibrationEffect.createOneShot(3000, VibrationEffect.EFFECT_TICK))
        Toast.makeText(this, "SOS Activated!", Toast.LENGTH_SHORT).show()
        speakOut("SOS ACTIVATED! SOS ACTIVATED!")
        getLocationAndSendSOS()
    }

    private fun openEditContactsActivity() {
        speakOut("Editing Emergency Contact List")
        startActivity(Intent(this, EditContactsAct::class.java))
    }

    private fun toggleFlashlight(btnFlashlight: CardView) {
        try {
            // Check if the device supports a flashlight
            if (cameraId == null) {
                speakOut("This device does not have a flashlight.")
                Toast.makeText(this, "Flashlight not supported on this device.", Toast.LENGTH_SHORT).show()
                return
            }

            if (isFlashlightOn) {
                // Turn OFF the flashlight
                cameraManager.setTorchMode(cameraId, false)
                isFlashlightOn = false

                // Update UI elements (text & icon)
                findViewById<TextView>(R.id.flashlight_text).text = "Turn On Flashlight"
                findViewById<ImageView>(R.id.flashlight_icon).setImageDrawable(getDrawable(R.drawable.flashlight_off_icon))

                // Provide feedback
                speakOut("Flashlight turned OFF")
            } else {
                // Turn ON the flashlight
                cameraManager.setTorchMode(cameraId, true)
                isFlashlightOn = true

                // Update UI elements (text & icon)
                findViewById<TextView>(R.id.flashlight_text).text = "Turn Off Flashlight"
                findViewById<ImageView>(R.id.flashlight_icon).setImageDrawable(getDrawable(R.drawable.flashlight_on_icon))

                // Provide feedback
                speakOut("Flashlight turned ON")
            }

            // Optional: Provide a small vibration feedback to signal change in state
            vibrate()

        } catch (e: CameraAccessException) {
            // Handle error if the camera is not accessible (may happen in some configurations)
            Log.e("MainActivity", "Error accessing the camera", e)
            speakOut("Error accessing flashlight.")
            Toast.makeText(this, "Error accessing the flashlight", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Handle any other errors gracefully
            Log.e("MainActivity", "An error occurred while toggling flashlight", e)
            speakOut("Error toggling flashlight")
            Toast.makeText(this, "Error toggling flashlight", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCalculator() {
        try {
            // Implicit intent to open the default calculator
            var intent = packageManager.getLaunchIntentForPackage("com.google.android.calculator")

            // Check for Samsung Calculator
            if (intent == null) {
                intent = packageManager.getLaunchIntentForPackage("com.sec.android.app.popupcalculator")
            }

            // Check for Xiaomi Calculator
            if (intent == null) {
                intent = packageManager.getLaunchIntentForPackage("com.miui.calculator")
            }

            // If the intent is not null, it means we have the Calculator app installed
            if (intent != null) {
                startActivity(intent)
            } else {
                // If default calculator is not found, show a message or fallback to play store
                // You can log an error message or do something else
                val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.android.calculator2"))
                startActivity(storeIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: Open Play Store for calculator app if there was an issue
            val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.android.calculator2"))
            startActivity(storeIntent)
        }
    }


    private fun openCalendar() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALENDAR)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No Calendar app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCameraApp() {
        try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openChrome() {
        val packageName = "com.android.chrome"
        val intent = packageManager.getLaunchIntentForPackage(packageName)

        if (intent != null) {
            // Launch Chrome app
            startActivity(intent)
        } else {
            // If Chrome is not installed, show a message or handle the case
            // You can open a generic browser instead, or show an alert to the user
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            startActivity(fallbackIntent)
        }
    }

    private fun openHotstar() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("in.startv.hotstar")
            startActivity(intent)
        } catch (e: Exception) {
            // Handle case where Hotstar might not open as expected
            e.printStackTrace()
        }
    }

    private fun openYouTube() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "YouTube is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMaps() {
        // Check location permission before accessing the location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->

                if (location != null) {
                    // Location retrieved successfully. Build the URI.
                    val latitude = location.latitude
                    val longitude = location.longitude

                    // URI with current location
                    val locationUri = "geo:$latitude,$longitude?q=$latitude,$longitude"

                    try {
                        // Try opening Google Maps with the current location
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(locationUri))
                        intent.setPackage("com.google.android.apps.maps")
                        startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback in case of an error
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com"))
                        startActivity(browserIntent)
                    }

                } else {
                    // Fallback if location is null (fallback could be based on last known location or default location)
                    Toast.makeText(this, "Unable to retrieve current location", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Handle if permission is denied
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWhatsApp() {
        val packageManager = packageManager
        val packageName = "com.whatsapp"
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            startActivity(intent)
        } catch (e: Exception) {
            // If WhatsApp is not installed, show a message or handle the case
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me"))
            startActivity(fallbackIntent)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.type = "image/*" // Opens gallery apps that handle images
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Handle the case where no gallery app is found
            e.printStackTrace()
            Toast.makeText(this, "No default gallery app found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLocationAndSendSOS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        sendSMS(latitude, longitude)
                    } else {
                        sendSMS("Location unavailable")
                    }
                }
        }
    }

    private fun sendSMS(locationDetails: String) {
        val contactsMap = loadContactsFromPreferences()

        if (contactsMap.isEmpty()) {
            Toast.makeText(this, "Please set an emergency contact first.", Toast.LENGTH_SHORT).show()
            return
        }

        val message = "SOS Alert: Emergency! Location: $locationDetails"
        val smsManager = SmsManager.getDefault()

        for ((name, phoneNumber) in contactsMap) {
            if (phoneNumber.isBlank()) continue
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "SMS sent to $name ($phoneNumber)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSMS(latitude: Double, longitude: Double) {
        val contactsMap = loadContactsFromPreferences()

        if (contactsMap.isEmpty()) {
            Toast.makeText(this, "Please set an emergency contact first.", Toast.LENGTH_SHORT).show()
            return
        }

        val message = "SOS Alert. My Location: Latitude: $latitude, Longitude: $longitude."
        val smsManager = SmsManager.getDefault()

        for ((name, phoneNumber) in contactsMap) {
            if (phoneNumber.isBlank()) continue
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "SMS sent to $name ($phoneNumber)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveContactsToPreferences(contactsMap: Map<String, String>) {
        val gson = Gson()
        val jsonContacts = gson.toJson(contactsMap)
        sharedPreferences.edit().putString("emergency_contacts_map", jsonContacts).apply()
        Log.d("MainActivity", "Saved contacts as JSON: $jsonContacts")
    }

    private fun loadContactsFromPreferences(): Map<String, String> {
        val jsonContacts = sharedPreferences.getString("emergency_contacts_map", "{}") ?: "{}"
        Log.d("MainActivity", "Loaded contacts JSON: $jsonContacts")
        return Gson().fromJson(jsonContacts, object : TypeToken<Map<String, String>>() {}.type)
    }

    private fun migrateLegacyContacts() {
        // Check if the "emergency_contacts_map" contains old data
        if (sharedPreferences.contains("emergency_contacts_map")) {
            try {
                // Attempt to retrieve as a string (modern format)
                val jsonContacts = sharedPreferences.getString("emergency_contacts_map", null)
                if (jsonContacts != null) {
                    Log.d("MainActivity", "Contacts are already in JSON format: $jsonContacts")
                    return
                }
            } catch (e: ClassCastException) {
                Log.w("MainActivity", "Legacy data detected. Migrating...")
            }

            // Retrieve legacy `Set<String>` format if present
            val legacySet = sharedPreferences.getStringSet("emergency_contacts_map", null)
            if (legacySet != null) {
                Log.d("MainActivity", "Legacy contact set: $legacySet")

                // Convert `Set<String>` to a `Map<String, String>`
                val contactsMap = legacySet.associateWith { "Unknown Name" }

                // Save the new JSON structure
                saveContactsToPreferences(contactsMap)

                // Remove the old format from SharedPreferences
                sharedPreferences.edit().remove("emergency_contacts_map").apply()
            }
        }
    }



    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun speakOut(text: String) {
        if (tts.isSpeaking) {
            tts.stop()
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For Android O and above, use VibrationEffect
            val vibrationEffect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(vibrationEffect)
        } else {
            // For below Android O, use the deprecated vibrate method
            vibrator.vibrate(200)
        }
    }

    private fun vibrateSOS() {
        // SOS pattern: short, long, long, each with a 1-second interval
        // Short = 200ms, Long = 600ms
        // 0ms delay before first vibration
        val pattern = longArrayOf(0, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000)

        // Check Android version and apply vibration pattern
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For devices running Android 8.0 (API level 26) and above
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            // For devices running below Android 8.0
            vibrator.vibrate(pattern, -1)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val langResult = tts.setLanguage(Locale.US)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language is not supported or missing data.")
            } else {
                Log.i("TTS", "Text-to-Speech Initialized.")
            }
        } else {
            Log.e("TTS", "Initialization failed.")
        }
    }

}
