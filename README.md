# **Elderly-Friendly Emergency Launcher**

## **Overview**
The **Elderly-Friendly Emergency Launcher** is a custom Android launcher designed specifically for elderly users. It provides an easy-to-use interface with **large buttons** that allow users to access essential apps and tools. The launcher focuses on **emergency accessibility**, featuring a dedicated **SOS button** to send an SMS to emergency contacts with the user's **location**. The launcher also provides access to basic apps like **Hotstar**, **YouTube**, **Chrome**, **Google Maps**, **Camera**, and **Flashlight**, making it simple for elderly users to access what they need.

## **Key Features**

### 🔹 **Home Screen with Large, Accessible Icons**
- Simple **home screen** interface with **large buttons** for easy access.
- Quick access to frequently used apps like **Gallery**, **Calculator**, **Phone**, **Messages**, **Camera**, and **Settings**.

### 🔹 **Emergency SOS Button**
- An **SOS button** that sends an SMS to predefined emergency contacts with the current **location** (latitude and longitude).
- The **SMS** allows contacts to be notified, while **no phone calls** are made—making the app useful in environments where placing a call is difficult.

### 🔹 **Location Picker & GPS Integration**
- Allows the user to **select** or **auto-fetch** their emergency location via GPS.
- Location can be viewed on **OpenStreetMap** and saved for future reference during emergencies.

### 🔹 **Access to Basic Apps**
Beyond the emergency tools, the launcher provides easy access to a selection of apps, including:
  - **Hotstar**: Streaming service for movies and TV shows.
  - **Gallery**: For browsing photos and videos on the device.
  - **Camera**: Direct access to take photos.
  - **Flashlight**: Button to turn on the phone’s flashlight.
  - **YouTube**: For watching videos.
  - **Chrome**: For browsing the web.
  - **Google Maps**: For navigation and location tracking.
  - **Phone**: Easy access to dial numbers.
  - **Messages**: For sending and reading text messages.
  - **Calculator**: For quick calculations.
  - **Calendar**: For managing dates and scheduling events.
  - **Settings**: To access device settings.

### 🔹 **Emergency Contact Management**
- Manage emergency contacts directly in the app.
- Contacts are saved for later use in sending **SMS messages** during emergencies.

### 🔹 **Customizable Interface**
- A **customizable** layout allowing users to select and rearrange available apps for easy access.
- The design is specifically tailored to cater to the needs of elderly users, reducing complexity and unnecessary distractions.

---

## **Project Structure**

### **MainActivity (Home Screen and App Launcher)**

This is the **main home screen** of the app. It consists of the following components:
- **Emergency SOS Button**: Sends location via SMS to selected contacts when pressed.
- **Basic Apps**: Large clickable buttons for quick access to camera, gallery, calendar, flashlight, YouTube, Chrome, Google Maps, phone, etc.

### **LocationPickerActivity (Location Picker for Emergencies)**

This activity allows users to select their **emergency location** via a map:
- **GPS Location** is fetched and shown on a map.
- **Manual selection** of the location using OpenStreetMap is available.
- The selected location is saved for use in sending emergency SMS alerts.

### **ContactsAdapter (Adapter for Emergency Contacts)**

Displays and manages a list of emergency contacts using a **RecyclerView**. Users can:
- Add contacts from their phone book.
- Delete contacts.
- View contact information.

### **Location and Contact Management**

- **Location** and **contacts** are saved in **SharedPreferences**, ensuring persistent data even after the app is closed.

### **Messaging (Sending SMS during SOS)**

Upon pressing the **SOS button**, the app fetches the user's current **GPS location** and sends an **SMS** with the coordinates to all selected emergency contacts.

---

## **Requirements**

- Android 5.0 (Lollipop) or higher.
- **Android Studio** with the **Android SDK** (minimum version 30).
- **Internet connection** (for browsing with Chrome, YouTube, and Google Maps).

### **Required Permissions**
The app uses the following permissions:
- **ACCESS_FINE_LOCATION**: To fetch the device’s location for displaying on the map and sending during SOS.
- **SEND_SMS**: For sending SMS during an emergency.
- **READ_CONTACTS**: To access and choose contacts from the user’s phone.
- **CAMERA**: Access to the camera for taking photos.
- **FLASHLIGHT**: To toggle the flashlight.
- **INTERNET**: Used by Chrome, Google Maps, YouTube, and other online apps.
- **CALL_PHONE**: Potential future feature for emergency calling (currently disabled in the app).
- **WAKE_LOCK**: Ensures the phone remains awake when using specific apps like flashlight.

---

## **Getting Started**

### **Setup Instructions**
1. Clone the repository:
   ```bash
   git clone <repository_url>
   cd <project_directory>
2. Open the project in Android Studio.
3. Build and run the project on your emulator or device.

---

## **Contributing**
Contributions are welcome! If you'd like to contribute to this project, please fork the repository, create a new branch, and submit a pull request for review.

---

## **Contact**
If you have any questions or suggestions, please feel free to contact the project maintainer:

Email: srinandek@gmail.com
