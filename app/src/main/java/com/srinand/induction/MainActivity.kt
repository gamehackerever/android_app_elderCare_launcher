package com.srinand.induction

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.telephony.gsm.SmsManager
import android.util.Log
import android.provider.MediaStore
import android.provider.Settings
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
import com.google.android.material.button.MaterialButton
import java.util.*

class MainActivity : AppCompatActivity(), OnInitListener {
    private var isFlashlightOn = false
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var tts: TextToSpeech
    private lateinit var vibrator: Vibrator
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val REQUEST_CODE_PERMISSION = 1001
    private var isCancelPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsIfNeeded() // Check and request permissions at the beginning
        setContentView(R.layout.home_page)

        enableImmersiveMode()

        // Get the layout inflater
        val inflater = layoutInflater

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
        sharedPreferences = getSharedPreferences("emergency_contacts", MODE_PRIVATE)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        tts = TextToSpeech(this, this)

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

    private fun showFirstLaunchHint() {
        AlertDialog.Builder(this)
            .setTitle("Welcome!")
            .setMessage("Long press the SOS button to edit emergency contacts.")
            .setPositiveButton("Got it") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
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
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivity(intent)
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
        // Retrieve the emergency contacts from SharedPreferences
        val emergencyContacts = sharedPreferences.getStringSet("emergency_contacts", setOf())
        if (emergencyContacts.isNullOrEmpty()) {
            // Handle empty or null emergency contacts (inform the user)
            Log.e("MainActivity", "Emergency contact not found.")
            Toast.makeText(this, "Please set an emergency contact first.", Toast.LENGTH_SHORT).show()
            return
        }

        val message = "SOS Alert: Emergency! Location: $locationDetails"
        val smsManager = SmsManager.getDefault()

        // Loop through each emergency contact and send SMS
        for (phoneNumber in emergencyContacts) {
            Log.d("MainActivity", "Sending SMS to: $phoneNumber")
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "SMS sent to $phoneNumber", Toast.LENGTH_SHORT).show()
        }
    }


    private fun sendSMS(latitude: Double, longitude: Double) {
        // Retrieve the emergency contacts from SharedPreferences
        val emergencyContacts = sharedPreferences.getStringSet("emergency_contacts", setOf())
        if (emergencyContacts.isNullOrEmpty()) {
            // Handle empty or null phone number (inform the user)
            Log.e("MainActivity", "Emergency contact not found.")
            Toast.makeText(this, "Please set an emergency contact first.", Toast.LENGTH_SHORT).show()
            return
        }

        val smsManager: SmsManager = SmsManager.getDefault()
        val message = "SOS Alert. My Location: Latitude: $latitude, Longitude: $longitude."

        // Send the SMS to each contact in the set
        for (contact in emergencyContacts) {
            smsManager.sendTextMessage(contact, null, message, null, null)
            Toast.makeText(this, "SMS sent to $contact", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "Emergency SMS sent to $contact.")
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
