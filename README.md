# RPG-Handy-Helper-App  

# BETA
Only local server availability yet.

**RPG-Handy-Helper** is a mobile application designed to support both players and game masters during tabletop RPG sessions. It allows users to authenticate, view their profile and check upcoming games, all based on data fetched from a connected backend server. 
Also simulate animated dice rolls.

## 📱 Features

- **Authentication system** – login using JWT-based token authentication.
- **Registration via WebView** – users are redirected to an external registration form inside a WebView.
- **3D Dice Rolling** – animated 3D dice (d6, d10, more upcoming) rendered using OpenGL/GLSurfaceView.
- **User Profile** – view user information retrieved from the server.
- **Game & Schedule Viewer** – display upcoming RPG sessions assigned to the logged-in user.
- **Offline Support** – previously fetched data is cached locally using Room.
- **Push Notifications** – automatic reminders for scheduled games.
- **REST API Integration** – the app reads data from a remote server using Retrofit and local Room persistence.

> ℹ️ **Note**: This application currently offers read-only access to server data. Editing or creating content is not supported in this version.

## 🧱 Technologies Used

- **Android** (Kotlin)
- **OpenGL (GLSurfaceView)** – for rendering animated 3D dice rolls
- **Room** (local SQLite database)
- **Retrofit + OkHttp** (network communication)
- **JWT** (authentication)
- **WebView** – for user registration
- **AlarmManager** (notifications)
- **Gradle Kotlin DSL**


## 🏗️ Project Structure

```
app/
├── src/
│ ├── main/
│ │ ├── java/com/example/rpgdiceapp/
│ │ │ ├── activities (MainActivity, DiceActivity, LoginActivity, etc.)
│ │ │ ├── api (ApiService, ApiInterface, AuthInterceptor)
│ │ │ ├── data/local (Room: DAO, Entity, Database)
│ │ │ └── utils, repository
│ │ └── res/ (XML layouts, icons, fonts)
├── build.gradle.kts
```


## 🧪 Getting Started

1. **Requirements**:
   - Android Studio Flamingo or later
   - Android SDK 33+
   - Emulator or physical Android device

2. **Configuration**:
   - Set the backend API URL in `ApiService.kt` (`baseUrl`)
   - Ensure that your REST API is running and responds to `/api/mobile/v1/`

3. **Build & Run**:
   ```bash
   ./gradlew clean assembleDebug
   ```

4. Import in Android Studio:

Open the RPG-Handy-Helper_App folder as a Gradle project.

🔐 Security Notes
JWT tokens are stored in SharedPreferences. For production use, consider migrating to Android Keystore.

Local data is automatically synced at login and app startup when online .

📂 License
This project is intended for educational use only. Commercial use requires the author’s permission.
