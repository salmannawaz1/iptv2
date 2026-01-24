// Test sallu1 user and M3U parsing
const { getDb, initializeDatabase } = require('./src/db/database');
const axios = require('axios');

async function testUser() {
  // Initialize database first
  await initializeDatabase();
  
  console.log('=== Checking sallu1 user ===');
  const db = getDb();
  const user = db.prepare('SELECT * FROM users WHERE username = ?').get('sallu1');
  
  if (!user) {
    console.log('❌ User "sallu1" NOT found in database');
    console.log('\nAll users in database:');
    const allUsers = db.prepare('SELECT username, m3u_url FROM users').all();
    allUsers.forEach(u => {
      console.log(`  - ${u.username}: ${u.m3u_url || '(no M3U URL)'}`);
    });
    return;
  }
  
  console.log('✅ User found!');
  console.log('Username:', user.username);
  console.log('M3U URL:', user.m3u_url || '(EMPTY)');
  console.log('Expiry:', new Date(user.expiry_date).toISOString());
  console.log('Active:', user.is_active);
  
  if (!user.m3u_url) {
    console.log('\n❌ M3U URL is EMPTY for this user!');
    return;
  }
  
  // Test authentication API
  console.log('\n=== Testing Authentication API ===');
  try {
    const authRes = await axios.get('http://localhost:5000/player_api.php', {
      params: {
        username: 'sallu1',
        password: user.password  // This is hashed, won't work
      }
    });
    console.log('Auth response:', authRes.data);
  } catch (err) {
    console.log('Auth failed (expected - password is hashed)');
  }
  
  // Test live categories
  console.log('\n=== Testing Live Categories API ===');
  try {
    const catRes = await axios.get('http://localhost:5000/player_api.php', {
      params: {
        username: 'sallu1',
        password: 'test',  // We need the plain password
        action: 'get_live_categories'
      }
    });
    console.log('Categories count:', catRes.data.length);
    console.log('First 3 categories:', JSON.stringify(catRes.data.slice(0, 3), null, 2));
  } catch (err) {
    console.log('Failed to get categories:', err.message);
  }
  
  // Test live streams
  console.log('\n=== Testing Live Streams API ===');
  try {
    const streamRes = await axios.get('http://localhost:5000/player_api.php', {
      params: {
        username: 'sallu1',
        password: 'test',
        action: 'get_live_streams'
      }
    });
    console.log('Streams count:', streamRes.data.length);
    if (streamRes.data.length > 0) {
      console.log('First stream:', JSON.stringify(streamRes.data[0], null, 2));
    }
  } catch (err) {
    console.log('Failed to get streams:', err.message);
  }
}

testUser().catch(console.error);
