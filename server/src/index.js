require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');

const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');
const resellerRoutes = require('./routes/resellers');
const playlistRoutes = require('./routes/playlists');
const statsRoutes = require('./routes/stats');
const xtreamRoutes = require('./routes/xtream');

const { initializeDatabase } = require('./db/database');

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(express.json());

// Start server after database initialization
async function startServer() {
  await initializeDatabase();

  // Routes
  app.use('/api/auth', authRoutes);
  app.use('/api/users', userRoutes);
  app.use('/api/resellers', resellerRoutes);
  app.use('/api/playlists', playlistRoutes);
  app.use('/api/stats', statsRoutes);
  
  // Root welcome page
  app.get('/', (req, res) => {
    res.json({
      name: 'IPTV Reseller API',
      status: 'online',
      version: '1.0.0',
      endpoints: {
        xtream_api: '/player_api.php',
        health: '/api/health',
        login: '/api/auth/login'
      },
      documentation: 'Use /player_api.php for Xtream Codes API',
      timestamp: new Date().toISOString()
    });
  });

  // Xtream Codes API compatibility (for IPTV apps)
  app.use('/', xtreamRoutes);

  // Health check
  app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
  });

  // Error handling middleware
  app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Something went wrong!' });
  });

  app.listen(PORT, () => {
    console.log(`🚀 IPTV Reseller API running on port ${PORT}`);
  });
}

startServer().catch(console.error);
