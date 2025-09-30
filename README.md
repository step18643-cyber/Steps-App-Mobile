# Step Counter System

This repository contains the complete implementation of a step counter system as per the Technical Skill Assessment assignment. The system includes three core components:
 
1. **Mobile Application**: An Android app that collects step data from the device's sensor, stores it locally, and transmits it to the cloud backend.
 
2. **Cloud Backend**: A Node.js/Express API with MongoDB for storing and retrieving step data.
 
3. **Web Application**: A React-based dashboard that visualizes the step data in graphs and tables.
 
 
**Live Demo:**
 
 - Frontend deployed on Vercel: [https://step-counter-app-frontend.vercel.app](https://step-counter-app-frontend.vercel.app/)
- Backend deployed on Vercel: [https://step-counter-app-backend-yc2i-fsx1nwv7b.vercel.app](https://step-counter-app-backend-yc2i-fsx1nwv7b.vercel.app)  
- Web app (deploy separately, e.g., on Vercel/Netlify, pointing to the backend).  
- Mobile: Build and run on Android device/emulator.  
 

## Mobile Application (Android)

### How to Set Up and Run

1. Clone the repository:
   ```bash
   git clone https://github.com/step18643-cyber/Steps-App-Mobile.git
   ```
2. Open in Android Studio.
3. Update API URL in `ApiClient.java` if needed (default: backend URL above).
4. Ensure `AndroidManifest.xml` has:

   ```xml
   <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
   ```
5. Build and run on a device/emulator (API 29+).
6. Grant activity recognition permission.

### Testing Notes
- On emulators: Step sensors may not work without hardware; simulate steps by manually triggering sensor events in code or using a physical device.
- Offline Handling: If no network, data queues in Room DB and syncs on reconnect.

### Dependencies

* AndroidX
* Room (local DB)
* Retrofit (API calls)
* SensorManager
