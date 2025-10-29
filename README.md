# 💬 WhatsApp Clone – Secure Messaging App

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-brightgreen)
![Java](https://img.shields.io/badge/Language-Java-orange)
![Firebase](https://img.shields.io/badge/Backend-Firebase-yellow)
![Encryption](https://img.shields.io/badge/Encryption-RSA%2BAES-blue)
![License](https://img.shields.io/badge/License-MIT-red)

A modern, secure real-time messaging application built with **Java** and **Firebase**, featuring industry-standard **RSA+AES hybrid encryption** for end-to-end message security.

[Features](#-features) • [Tech Stack](#-tech-stack) • [Security](#-security-architecture) • [Installation](#-installation) • [Usage](#-usage) • [Contributing](#-contributing)

</div>

---

## � Screenshots

> Add your app screenshots here

---

## ✨ Features

### � Security
- **RSA+AES Hybrid Encryption** – Industry-standard encryption (same as WhatsApp/Signal)
- **2048-bit RSA Keys** – Public/private key pairs for secure key exchange
- **256-bit AES Session Keys** – Unique encryption key per message
- **Auto Key Generation** – Seamless key management on signup/login
- **Private Key Protection** – Keys stored securely in device SharedPreferences

### � Messaging
- **Real-time Chat** – Instant message delivery using Firebase Realtime Database
- **End-to-End Encryption** – Messages encrypted before leaving your device
- **Last Message Preview** – See decrypted message previews in chat list
- **Smart Chat Sorting** – Most recent conversations appear at the top
- **Message Timestamps** – Track when messages were sent/received

### 👤 User Management
- **Firebase Authentication** – Secure user signup and login
- **User Profiles** – Display names and profile pictures
- **Online Status** – See when users were last active
- **User Discovery** – Find and chat with other users

### 🎨 UI/UX
- **Material Design** – Clean, modern Android interface
- **Responsive Layout** – Works on various screen sizes
- **Chat Bubbles** – Distinct sender/receiver message styling
- **Smooth Animations** – Polished user experience

---

## 🛠 Tech Stack

### Frontend
- **Language**: Java
- **Platform**: Android SDK
- **UI Framework**: Android XML Layouts
- **Design**: Material Design Components

### Backend
- **Database**: Firebase Realtime Database
- **Authentication**: Firebase Auth (Email/Password)
- **Storage**: Firebase Storage (for profile pictures)
- **Cloud**: Google Firebase Platform

### Security
- **Encryption**: RSA (2048-bit) + AES (256-bit)
- **Key Management**: Custom RSAKeyManager
- **Hybrid Encryption**: Custom HybridEncryption utility
- **Storage**: Android SharedPreferences (encrypted keys)

### Build Tools
- **Build System**: Gradle
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

---

## � Security Architecture

### Encryption Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    MESSAGE ENCRYPTION                        │
└─────────────────────────────────────────────────────────────┘

1. Sender Types Message: "Hello!"
         ↓
2. Generate Random AES-256 Key (Session Key)
         ↓
3. Encrypt Message with AES Key
   "Hello!" → "U2FsdGVkX1+..." (encrypted)
         ↓
4. Fetch Recipient's Public RSA Key (from Firebase)
         ↓
5. Encrypt AES Key with Recipient's Public Key
   AES Key → "MIGfMA0GCSqGSIb3..." (encrypted session key)
         ↓
6. Send Both to Firebase:
   - encryptedMessage: "U2FsdGVkX1+..."
   - encryptedSessionKey: "MIGfMA0GCSqGSIb3..."
         ↓
7. Recipient Decrypts Session Key with Private Key
         ↓
8. Recipient Decrypts Message with Decrypted AES Key
         ↓
9. Display: "Hello!"
```

### Key Management

- **Key Generation**: RSA key pairs generated on first signup/login
- **Public Keys**: Stored in Firebase `/PublicKeys/{userId}`
- **Private Keys**: Stored locally in device SharedPreferences
- **Key Size**: 2048-bit RSA, 256-bit AES
- **Key Rotation**: Auto-regenerated if missing

---

## 📦 Installation

### Prerequisites

Before you begin, ensure you have:
- **Android Studio** (Hedgehog or newer)
- **JDK 8+** installed
- **Firebase Account** (free tier works)
- **Git** installed

### Step 1: Clone the Repository

```bash
git clone https://github.com/UdayBhoyar/Whatsapp-Clone.git
cd Whatsapp-Clone
```

### Step 2: Set Up Firebase

1. **Create a Firebase Project**:
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Click "Add Project" and follow the wizard
   - Enable Google Analytics (optional)

2. **Register Your Android App**:
   - In Firebase Console, click "Add App" → Android
   - Package name: `com.example.whatsappclone`
   - Download `google-services.json`
   - Place it in `app/` directory

3. **Enable Firebase Services**:
   - **Authentication**: Enable Email/Password sign-in method
   - **Realtime Database**: Create database in test mode
   - **Storage**: Enable for profile pictures (optional)

4. **Set Database Rules**:
```json
{
  "rules": {
    "Users": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "PublicKeys": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "chats": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

### Step 3: Build the Project

1. **Open in Android Studio**:
   ```bash
   # Open the project folder in Android Studio
   # Or use: studio .
   ```

2. **Sync Gradle**:
   - Android Studio will automatically sync Gradle
   - Wait for dependencies to download

3. **Build APK**:
   ```bash
   # Using Gradle wrapper (recommended)
   ./gradlew assembleDebug
   
   # Or via Android Studio: Build → Build Bundle(s)/APK(s) → Build APK(s)
   ```

4. **Install on Device**:
   ```bash
   # Via ADB
   adb install app/build/outputs/apk/debug/app-debug.apk
   
   # Or via Android Studio: Run → Run 'app'
   ```

---

## 🚀 Usage

### First Time Setup

1. **Launch the App**
2. **Sign Up**:
   - Enter your name, email, and password
   - RSA keys are automatically generated
   - Public key uploaded to Firebase
   - Private key saved securely on device

3. **Find Users**:
   - Navigate to "Find Friends" or "Users" tab
   - See list of all registered users
   - Click on a user to start chatting

### Sending Messages

1. **Select a User** from the chat list
2. **Type Your Message** in the text field
3. **Click Send** ✉️
   - Message is encrypted with recipient's public key
   - Encrypted data sent to Firebase
   - Recipient decrypts with their private key
   - Both users see the original message

### How Encryption Works (Behind the Scenes)

```java
// When you send "Hello!"
String message = "Hello!";

// 1. Generate random AES key
SecretKey aesKey = generateAESKey();

// 2. Encrypt message with AES
String encryptedMsg = encryptWithAES(message, aesKey);

// 3. Encrypt AES key with recipient's RSA public key
String encryptedKey = encryptWithRSA(aesKey, recipientPublicKey);

// 4. Send to Firebase
firebase.send({
    encryptedMessage: encryptedMsg,
    encryptedSessionKey: encryptedKey
});

// 5. Recipient decrypts
SecretKey decryptedKey = decryptWithRSA(encryptedKey, myPrivateKey);
String originalMsg = decryptWithAES(encryptedMsg, decryptedKey);
// Result: "Hello!"
```

---

## 📂 Project Structure

```
WhatsappClone/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/whatsappclone/
│   │   │   │   ├── Adapter/
│   │   │   │   │   ├── ChatAdapter.java          # Chat message RecyclerView adapter
│   │   │   │   │   └── UsersAdapter.java         # User list adapter
│   │   │   │   ├── Fragment/
│   │   │   │   │   ├── ChatsFragment.java        # Chat list fragment
│   │   │   │   │   └── ProfileFragment.java      # User profile fragment
│   │   │   │   ├── Models/
│   │   │   │   │   ├── MessageModel.java         # Message data model
│   │   │   │   │   └── Users.java                # User data model
│   │   │   │   ├── utils/
│   │   │   │   │   ├── RSAKeyManager.java        # RSA key generation & management
│   │   │   │   │   └── HybridEncryption.java     # RSA+AES encryption utilities
│   │   │   │   ├── MainActivity.java             # Main activity with fragments
│   │   │   │   ├── ChatdetailActivity.java       # Individual chat screen
│   │   │   │   ├── SignUpActivity.java           # User registration
│   │   │   │   └── SignInActivity.java           # User login
│   │   │   └── res/
│   │   │       ├── layout/                        # XML layouts
│   │   │       ├── drawable/                      # Images & icons
│   │   │       └── values/                        # Strings, colors, themes
│   │   └── google-services.json                   # Firebase config
│   └── build.gradle                               # App-level Gradle config
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle                                   # Project-level Gradle config
├── settings.gradle
└── README.md
```

---

## 🔑 Key Components

### RSAKeyManager.java
Handles RSA key pair lifecycle:
- `generateKeyPair()` - Creates 2048-bit RSA keys
- `saveKeyPair()` - Stores keys in SharedPreferences
- `loadPrivateKey()` - Retrieves private key from storage
- `loadPublicKey()` - Retrieves public key from Firebase
- `publicKeyToString()` - Converts key to Base64 string
- `stringToPublicKey()` - Converts Base64 to PublicKey object

### HybridEncryption.java
Implements RSA+AES encryption:
- `encrypt(message, publicKey)` - Encrypts message with hybrid approach
- `decrypt(encryptedMessage, privateKey)` - Decrypts message
- `EncryptedMessage` - Inner class wrapping encrypted data + session key

### ChatdetailActivity.java
Manages individual chats:
- Loads sender's private key and recipient's public key
- Encrypts messages twice (for sender and recipient)
- Decrypts incoming messages with private key
- Retry mechanism for missing recipient keys
- Real-time message synchronization

### UsersAdapter.java
Displays chat list:
- Shows last message preview (decrypted)
- Sorts chats by last message timestamp
- Handles user click events

---

## 🐛 Troubleshooting

### Common Issues

**Q: "Encryption keys not found" error**  
**A:** Keys are auto-generated on app startup. If you see this:
1. Force close the app
2. Clear app data (Settings → Apps → WhatsApp Clone → Clear Data)
3. Login again - keys will be regenerated

**Q: "Recipient encryption not set up" error**  
**A:** The recipient hasn't logged in yet or their public key is missing:
1. Ask the recipient to open the app and login
2. Wait a few seconds and try sending again
3. The app will retry automatically up to 10 times

**Q: Messages showing "[Decryption failed]"**  
**A:** This usually means:
1. Keys were regenerated and old messages can't be decrypted (by design)
2. Message was corrupted during transmission
3. Try sending a new message - it should work

**Q: Chat list not showing latest conversation on top**  
**A:** Clear app cache and restart:
```bash
adb shell pm clear com.example.whatsappclone
```

**Q: Build failing with "Duplicate class" error**  
**A:** Clean and rebuild:
```bash
./gradlew clean
./gradlew assembleDebug
```

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

### How to Contribute

1. **Fork the Repository**
   ```bash
   # Click "Fork" button on GitHub
   ```

2. **Clone Your Fork**
   ```bash
   git clone https://github.com/YOUR_USERNAME/Whatsapp-Clone.git
   cd Whatsapp-Clone
   ```

3. **Create a Feature Branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```

4. **Make Your Changes**
   - Write clean, documented code
   - Follow existing code style
   - Test thoroughly

5. **Commit Your Changes**
   ```bash
   git add .
   git commit -m "Add amazing feature"
   ```

6. **Push to Your Fork**
   ```bash
   git push origin feature/amazing-feature
   ```

7. **Open a Pull Request**
   - Go to original repository
   - Click "New Pull Request"
   - Describe your changes

### Contribution Guidelines

- 📝 **Code Quality**: Follow Java best practices
- 🧪 **Testing**: Test on multiple devices/Android versions
- 📖 **Documentation**: Update README if needed
- 🔒 **Security**: Never commit API keys or secrets
- 💬 **Communication**: Be respectful and constructive

---

## 🗺️ Roadmap

### Planned Features

- [ ] 📸 **Image/Video Sharing** - Send photos and videos
- [ ] 🎤 **Voice Messages** - Record and send audio clips
- [ ] 👥 **Group Chats** - Multi-user conversations
- [ ] 📞 **Voice/Video Calls** - Real-time communication
- [ ] ✅ **Read Receipts** - See when messages are read
- [ ] ⌨️ **Typing Indicators** - Show when someone is typing
- [ ] 🌙 **Dark Mode** - Eye-friendly night theme
- [ ] 🔔 **Push Notifications** - FCM integration
- [ ] 📱 **Status/Stories** - 24-hour disappearing updates
- [ ] 🔍 **Message Search** - Find old messages quickly
- [ ] 📌 **Pin Chats** - Keep important chats at top
- [ ] 🗑️ **Delete Messages** - Remove sent messages
- [ ] ⏰ **Scheduled Messages** - Send messages later
- [ ] 🌐 **Multi-language Support** - Internationalization

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 Uday Bhoyar

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 👨‍💻 Author

**Uday Bhoyar**

- GitHub: [@UdayBhoyar](https://github.com/UdayBhoyar)
- Repository: [Whatsapp-Clone](https://github.com/UdayBhoyar/Whatsapp-Clone)

---

## 🙏 Acknowledgments

- **Firebase** - For providing excellent backend services
- **Android Team** - For the robust Android SDK
- **Material Design** - For beautiful UI components
- **Stack Overflow Community** - For solving countless issues
- **WhatsApp** - For inspiration on messaging UX

---

## 📞 Support

If you encounter any issues or have questions:

1. **Check** the [Troubleshooting](#-troubleshooting) section
2. **Search** existing [Issues](https://github.com/UdayBhoyar/Whatsapp-Clone/issues)
3. **Open** a new issue if your problem isn't covered
4. **Provide** detailed information:
   - Android version
   - Device model
   - Steps to reproduce
   - Error messages/logs

---

## ⭐ Show Your Support

If you found this project helpful, please consider:

- ⭐ **Starring** this repository
- 🍴 **Forking** to create your own version
- 📢 **Sharing** with others who might find it useful
- 💖 **Contributing** to make it even better

---

<div align="center">

**Made with ❤️ and ☕ by [Uday Bhoyar](https://github.com/UdayBhoyar)**

*Secure messaging for everyone, everywhere.*

</div>
