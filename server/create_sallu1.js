// Create sallu1 user with M3U URL
const { getDb, initializeDatabase } = require('./src/db/database');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');

async function createUser() {
  await initializeDatabase();
  const db = getDb();
  
  // Check if sallu1 exists
  const existing = db.prepare('SELECT * FROM users WHERE username = ?').get('sallu1');
  
  if (existing) {
    console.log('User sallu1 already exists. Updating M3U URL and password...');
    const hashedPassword = bcrypt.hashSync('sallu1', 10);
    db.prepare('UPDATE users SET m3u_url = ?, password = ? WHERE username = ?')
      .run('https://iptv-org.github.io/iptv/index.m3u', hashedPassword, 'sallu1');
    console.log('✅ Updated sallu1 with M3U URL and password');
  } else {
    console.log('Creating new user sallu1...');
    
    // Get admin reseller
    const reseller = db.prepare('SELECT id FROM resellers WHERE username = ?').get('admin');
    if (!reseller) {
      console.error('❌ Admin reseller not found!');
      process.exit(1);
    }
    
    const userId = uuidv4();
    const hashedPassword = bcrypt.hashSync('sallu1', 10);
    const expiryDate = new Date();
    expiryDate.setFullYear(expiryDate.getFullYear() + 1); // 1 year
    
    db.prepare(`
      INSERT INTO users (id, username, password, max_connections, is_active, expiry_date, reseller_id, notes, m3u_url)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).run(
      userId,
      'sallu1',
      hashedPassword,
      1,
      1,
      expiryDate.toISOString(),
      reseller.id,
      'Test user with M3U',
      'https://iptv-org.github.io/iptv/index.m3u'
    );
    
    console.log('✅ Created user sallu1');
    console.log('   Username: sallu1');
    console.log('   Password: sallu1');
    console.log('   M3U URL: https://iptv-org.github.io/iptv/index.m3u');
  }
  
  // Verify
  const user = db.prepare('SELECT username, m3u_url, is_active FROM users WHERE username = ?').get('sallu1');
  console.log('\n=== VERIFICATION ===');
  console.log('Username:', user.username);
  console.log('M3U URL:', user.m3u_url);
  console.log('Active:', user.is_active);
  
  process.exit(0);
}

createUser().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
