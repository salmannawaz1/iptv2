# IPTV System Guide - Panel + App Connection

## Overview: How Everything Works

```
┌─────────────────────────────────────────────────────────────────────┐
│                        YOUR SYSTEM                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   ┌──────────────┐         ┌──────────────┐         ┌────────────┐  │
│   │   ADMIN      │ creates │   RESELLER   │ creates │   USER     │  │
│   │   (You)      │────────>│   (Seller)   │────────>│  (Client)  │  │
│   └──────────────┘         └──────────────┘         └────────────┘  │
│          │                        │                       │         │
│          │                        │                       │         │
│          ▼                        ▼                       ▼         │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │              RESELLER PANEL (Web App)                       │   │
│   │              http://localhost:3000                          │   │
│   │                                                             │   │
│   │  • Admin logs in → Manages resellers, adds credits          │   │
│   │  • Reseller logs in → Creates users, gets playlist URLs     │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                              │                                       │
│                              │ API                                   │
│                              ▼                                       │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │              BACKEND API (Node.js)                          │   │
│   │              http://localhost:5000                          │   │
│   │                                                             │   │
│   │  • Stores users, resellers, subscriptions                   │   │
│   │  • Validates IPTV user credentials                          │   │
│   │  • Generates M3U playlist URLs                              │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                              │                                       │
│                              │ Validates                             │
│                              ▼                                       │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │              ANDROID APP (ProIPTV)                          │   │
│   │              Installed on client's phone/TV                 │   │
│   │                                                             │   │
│   │  • Client enters username + password from reseller          │   │
│   │  • App validates with your server                           │   │
│   │  • App plays live TV, movies, series                        │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Step-by-Step Flow

### 1. YOU (Admin) - Setup
```
1. Start the servers:
   - Backend: cd server && npm run dev (port 5000)
   - Frontend: cd client && npm run dev (port 3000)

2. Login to panel: http://localhost:3000
   - Username: admin
   - Password: admin123

3. Create a Reseller account and give them credits
```

### 2. RESELLER - Creates Users
```
1. Reseller logs into panel with their account
2. Goes to "Users" page
3. Creates a new IPTV user:
   - Username: client1
   - Password: pass123
   - Expiry: 30 days
   - Max connections: 1

4. Gets the playlist info for that user:
   - M3U URL: http://yourserver:5000/api/playlists/m3u/client1
   - Or Xtream credentials: username=client1, password=pass123
```

### 3. CLIENT - Uses the App
```
1. Client installs ProIPTV app on their Android device
2. Opens the app and enters:
   - Server: http://yourserver:5000
   - Username: client1
   - Password: pass123

3. App validates credentials with your server
4. If valid → App loads channels and starts streaming
```

---

## Testing the Panel

### Test 1: Login as Admin
1. Open http://localhost:3000
2. Login: admin / admin123
3. You should see the Dashboard

### Test 2: Create a Reseller
1. Go to "Resellers" page
2. Click "Add Reseller"
3. Fill: username=reseller1, password=test123, credits=100
4. Save

### Test 3: Add Credits
1. Find reseller1 in the list
2. Click "Add Credits" → Add 50 credits

### Test 4: Login as Reseller
1. Logout
2. Login as: reseller1 / test123
3. Go to "Users" page
4. Create a user: username=testuser, password=test123, expiry=30 days

### Test 5: Get Playlist URL
1. Click on the user you created
2. Click "Playlist Info"
3. You'll see the M3U URL and Xtream credentials

### Test 6: Validate API (using browser or Postman)
```
POST http://localhost:5000/api/playlists/validate
Content-Type: application/json

{
  "username": "testuser",
  "password": "test123"
}
```

---

## Packaging Android App in Android Studio

### Prerequisites
1. Install Android Studio (https://developer.android.com/studio)
2. Install Java JDK 17+

### Steps to Build APK

1. **Open Project**
   - Open Android Studio
   - File → Open → Select `d:\iptv\ProIPTV` folder

2. **Wait for Gradle Sync**
   - Android Studio will download dependencies
   - This may take 5-10 minutes first time

3. **Configure Server URL**
   - Open `app/src/main/java/.../utils/Constants.kt` or similar
   - Change the server URL to your panel's IP:
   ```kotlin
   const val BASE_URL = "http://YOUR_SERVER_IP:5000"
   ```

4. **Build Debug APK**
   - Menu: Build → Build Bundle(s) / APK(s) → Build APK(s)
   - APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

5. **Build Release APK (for distribution)**
   - Menu: Build → Generate Signed Bundle / APK
   - Choose APK
   - Create new keystore or use existing
   - Select release build type
   - APK will be at: `app/build/outputs/apk/release/app-release.apk`

### Install APK on Device
1. Copy APK to phone
2. Enable "Install from unknown sources" in settings
3. Open APK file to install

---

## For Production Deployment

### 1. Deploy Backend to a Server
```bash
# On your VPS/server
cd server
npm install
npm start

# Use PM2 for production:
npm install -g pm2
pm2 start src/index.js --name iptv-api
```

### 2. Get a Domain & SSL
- Point domain to your server IP
- Use nginx as reverse proxy
- Get SSL certificate (Let's Encrypt)

### 3. Update URLs
- Panel: Update API URL in client/src/services/api.js
- App: Update server URL in Android app

### Example Production URLs:
```
Panel: https://panel.yourdomain.com
API: https://api.yourdomain.com
App connects to: https://api.yourdomain.com
```

---

## Quick Reference

| Component | Local URL | Purpose |
|-----------|-----------|---------|
| Panel Frontend | http://localhost:3000 | Web UI for admin/reseller |
| Backend API | http://localhost:5000 | Database & validation |
| Android App | Install on device | End-user streaming app |

| User Type | Can Do |
|-----------|--------|
| Admin | Create resellers, add credits, view all |
| Reseller | Create users, manage subscriptions |
| IPTV User | Login to app, watch content |

---

## Common Issues

**Q: Panel shows blank page**
A: Check browser console for errors. Make sure both servers are running.

**Q: Login fails**
A: Check if backend is running on port 5000.

**Q: App can't connect**
A: Make sure server URL in app matches your backend. Use your computer's IP, not localhost.

**Q: Android Studio build fails**
A: Run "File → Invalidate Caches" and restart. Check Gradle sync.
