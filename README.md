# IPTV Reseller Panel

A complete web-based IPTV Reseller Panel for managing users, subscriptions, and generating M3U playlists.

## Features

### Admin Features
- **Reseller Management** - Create, edit, delete resellers
- **Credit System** - Add credits to reseller accounts
- **User Overview** - View all users across all resellers
- **Statistics** - Dashboard with total users, active/expired counts
- **Activity Logs** - Track all actions

### Reseller Features  
- **User Management** - Create, edit, delete IPTV users
- **Subscription Control** - Set expiry dates, extend subscriptions
- **Connection Limits** - Set max concurrent connections per user
- **Playlist Generation** - Generate M3U URLs for clients
- **Credit Usage** - Each user creation costs 1 credit
- **Dashboard** - View stats, expiring users

### Technical Features
- **JWT Authentication** - Secure token-based auth
- **SQLite Database** - No external DB required
- **Xtream Codes API Compatible** - Validates user credentials
- **Modern UI** - React + Tailwind CSS

## Project Structure

```
iptv/
├── server/                 # Backend API
│   ├── src/
│   │   ├── db/            # Database setup
│   │   ├── middleware/    # Auth middleware
│   │   ├── routes/        # API routes
│   │   └── index.js       # Entry point
│   └── data/              # SQLite database
│
├── client/                 # Frontend React app
│   ├── src/
│   │   ├── components/    # Reusable components
│   │   ├── context/       # Auth context
│   │   ├── pages/         # Page components
│   │   └── services/      # API service
│   └── index.html
│
└── ProIPTV/               # Android IPTV Player App
```

## Installation

### Prerequisites
- Node.js 18+
- npm or yarn

### Setup

1. **Install all dependencies:**
```bash
npm run install:all
```

2. **Configure environment:**
```bash
# Edit server/.env
PORT=5000
JWT_SECRET=your-secret-key-change-this
STREAM_SERVER_URL=http://your-iptv-server:8080
```

3. **Start development servers:**
```bash
npm run dev
```

This starts:
- Backend API at `http://localhost:5000`
- Frontend at `http://localhost:3000`

## Default Credentials

| Role | Username | Password |
|------|----------|----------|
| Admin | admin | admin123 |

**Change the admin password after first login!**

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login
- `GET /api/auth/me` - Get current user
- `POST /api/auth/change-password` - Change password

### Users (Reseller/Admin)
- `GET /api/users` - List users
- `POST /api/users` - Create user
- `PUT /api/users/:id` - Update user
- `DELETE /api/users/:id` - Delete user
- `POST /api/users/:id/extend` - Extend subscription

### Resellers (Admin only)
- `GET /api/resellers` - List resellers
- `POST /api/resellers` - Create reseller
- `PUT /api/resellers/:id` - Update reseller
- `DELETE /api/resellers/:id` - Delete reseller
- `POST /api/resellers/:id/credits` - Add credits

### Playlists
- `GET /api/playlists/m3u/:userId` - Get M3U URL for user
- `POST /api/playlists/validate` - Validate user credentials (for IPTV apps)
- `GET /api/playlists/bouquets` - List bouquets

### Statistics
- `GET /api/stats/dashboard` - Dashboard stats
- `GET /api/stats/expiring` - Users expiring soon

## How It Works

### Flow
1. **Admin** creates **Reseller** accounts with credits
2. **Reseller** logs in and creates **IPTV Users** (costs 1 credit each)
3. **Reseller** gets M3U URL or Xtream credentials for each user
4. **Reseller** gives credentials to their **Client**
5. **Client** enters credentials in IPTV app (like ProIPTV)
6. IPTV app validates credentials via `/api/playlists/validate`

### Credit System
- Admin adds credits to reseller
- Creating a user = 1 credit
- Extending subscription = 1 credit
- Resellers can't exceed their credit balance

## Android IPTV App

The `ProIPTV` folder contains a Kotlin Android app that works with this panel:
- Login with Xtream Codes credentials
- Live TV, Movies, Series support
- ExoPlayer for playback
- Favorites system

See `ProIPTV/README.md` for details.

## Production Deployment

### Backend
```bash
cd server
npm install --production
npm start
```

### Frontend
```bash
cd client
npm run build
# Serve the dist/ folder with nginx or similar
```

### Environment Variables
```env
PORT=5000
JWT_SECRET=change-this-to-a-secure-random-string
JWT_EXPIRES_IN=7d
STREAM_SERVER_URL=http://your-actual-stream-server.com:8080
```

## License

For educational purposes only.

## Disclaimer

- This is a panel management system only
- Users must have rights to the content they stream
- The developer assumes no legal responsibility
