const initSqlJs = require('sql.js');
const fs = require('fs');
const path = require('path');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');

const dbPath = path.join(__dirname, '../../data/iptv.db');
const dbDir = path.dirname(dbPath);
let db = null;

// Ensure data directory exists
if (!fs.existsSync(dbDir)) {
  fs.mkdirSync(dbDir, { recursive: true });
}

// Wrapper to make sql.js API similar to better-sqlite3
function createDbWrapper(database) {
  return {
    prepare: (sql) => ({
      run: (...params) => {
        database.run(sql, params);
        saveDatabase();
      },
      get: (...params) => {
        const stmt = database.prepare(sql);
        stmt.bind(params);
        if (stmt.step()) {
          const row = stmt.getAsObject();
          stmt.free();
          return row;
        }
        stmt.free();
        return undefined;
      },
      all: (...params) => {
        const results = [];
        const stmt = database.prepare(sql);
        stmt.bind(params);
        while (stmt.step()) {
          results.push(stmt.getAsObject());
        }
        stmt.free();
        return results;
      }
    }),
    exec: (sql) => {
      database.run(sql);
      saveDatabase();
    },
    pragma: () => {}
  };
}

function saveDatabase() {
  if (db && db._db) {
    const data = db._db.export();
    const buffer = Buffer.from(data);
    fs.writeFileSync(dbPath, buffer);
  }
}

async function initializeDatabase() {
  const SQL = await initSqlJs();
  
  // Load existing database or create new one
  let database;
  if (fs.existsSync(dbPath)) {
    const fileBuffer = fs.readFileSync(dbPath);
    database = new SQL.Database(fileBuffer);
  } else {
    database = new SQL.Database();
  }
  
  db = createDbWrapper(database);
  db._db = database;

  // Create tables
  db.exec(`
    -- Admins table (main admin who manages resellers)
    CREATE TABLE IF NOT EXISTS admins (
      id TEXT PRIMARY KEY,
      username TEXT UNIQUE NOT NULL,
      password TEXT NOT NULL,
      email TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    -- Resellers table
    CREATE TABLE IF NOT EXISTS resellers (
      id TEXT PRIMARY KEY,
      username TEXT UNIQUE NOT NULL,
      password TEXT NOT NULL,
      email TEXT,
      credits INTEGER DEFAULT 0,
      max_users INTEGER DEFAULT 100,
      is_active INTEGER DEFAULT 1,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      created_by TEXT,
      FOREIGN KEY (created_by) REFERENCES admins(id)
    );

    -- Users table (IPTV end users created by resellers)
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      username TEXT UNIQUE NOT NULL,
      password TEXT NOT NULL,
      max_connections INTEGER DEFAULT 1,
      is_active INTEGER DEFAULT 1,
      expiry_date DATETIME NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      reseller_id TEXT NOT NULL,
      notes TEXT,
      m3u_url TEXT,
      FOREIGN KEY (reseller_id) REFERENCES resellers(id)
    );

    -- Playlists/Bouquets table
    CREATE TABLE IF NOT EXISTS bouquets (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      description TEXT,
      channels TEXT,
      m3u_url TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    -- User-Bouquet assignments
    CREATE TABLE IF NOT EXISTS user_bouquets (
      user_id TEXT NOT NULL,
      bouquet_id TEXT NOT NULL,
      PRIMARY KEY (user_id, bouquet_id),
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
      FOREIGN KEY (bouquet_id) REFERENCES bouquets(id) ON DELETE CASCADE
    );

    -- Activity logs
    CREATE TABLE IF NOT EXISTS activity_logs (
      id TEXT PRIMARY KEY,
      actor_type TEXT NOT NULL,
      actor_id TEXT NOT NULL,
      action TEXT NOT NULL,
      details TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    -- Credit transactions
    CREATE TABLE IF NOT EXISTS credit_transactions (
      id TEXT PRIMARY KEY,
      reseller_id TEXT NOT NULL,
      amount INTEGER NOT NULL,
      type TEXT NOT NULL,
      description TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (reseller_id) REFERENCES resellers(id)
    );
  `);

  // Create default admin if not exists
  const adminExists = db.prepare('SELECT id FROM admins WHERE username = ?').get('admin');
  let adminId;
  if (!adminExists) {
    adminId = uuidv4();
    const hashedPassword = bcrypt.hashSync('admin123', 10);
    db.prepare(`
      INSERT INTO admins (id, username, password, email)
      VALUES (?, ?, ?, ?)
    `).run(adminId, 'admin', hashedPassword, 'admin@iptv.local');
    console.log('✅ Default admin created (username: admin, password: admin123)');
  } else {
    adminId = adminExists.id;
  }

  // Create admin reseller if not exists (admin acts as a reseller too)
  const adminReseller = db.prepare('SELECT id FROM resellers WHERE username = ?').get('admin');
  let resellerId;
  if (!adminReseller) {
    resellerId = uuidv4();
    const hashedPassword = bcrypt.hashSync('admin123', 10);
    db.prepare(`
      INSERT INTO resellers (id, username, password, email, credits, max_users, is_active, created_by)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `).run(resellerId, 'admin', hashedPassword, 'admin@iptv.local', 1000, 1000, 1, adminId);
    console.log('✅ Admin reseller created');
  } else {
    resellerId = adminReseller.id;
  }

  // Create test user if not exists
  const testUserExists = db.prepare('SELECT id FROM users WHERE username = ?').get('testuser');
  if (!testUserExists) {
    const testUserId = uuidv4();
    const hashedPassword = bcrypt.hashSync('test123', 10);
    const expiryDate = new Date();
    expiryDate.setDate(expiryDate.getDate() + 365);
    db.prepare(`
      INSERT INTO users (id, username, password, max_connections, is_active, expiry_date, reseller_id, notes, m3u_url)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).run(testUserId, 'testuser', hashedPassword, 1, 1, expiryDate.toISOString(), resellerId, 'Test user for app', '');
    console.log('✅ Test user created (username: testuser, password: test123)');
  }

  // Create sample bouquets
  const bouquetExists = db.prepare('SELECT id FROM bouquets LIMIT 1').get();
  if (!bouquetExists) {
    const bouquets = [
      { name: 'Sports Package', description: 'All sports channels', channels: JSON.stringify(['ESPN', 'Sky Sports', 'beIN Sports']) },
      { name: 'Movies Package', description: 'Premium movie channels', channels: JSON.stringify(['HBO', 'Showtime', 'Starz']) },
      { name: 'Kids Package', description: 'Children channels', channels: JSON.stringify(['Cartoon Network', 'Nickelodeon', 'Disney']) },
      { name: 'News Package', description: 'News channels', channels: JSON.stringify(['CNN', 'BBC News', 'Al Jazeera']) },
      { name: 'Full Package', description: 'All channels included', channels: JSON.stringify(['All Channels']) }
    ];

    const insertBouquet = db.prepare(`
      INSERT INTO bouquets (id, name, description, channels)
      VALUES (?, ?, ?, ?)
    `);

    bouquets.forEach(b => {
      insertBouquet.run(uuidv4(), b.name, b.description, b.channels);
    });
    console.log('✅ Sample bouquets created');
  }

  console.log('✅ Database initialized');
}

function getDb() {
  return db;
}

module.exports = { getDb, initializeDatabase };
