// Quick database check
const { getDb, initializeDatabase } = require('./src/db/database');

async function check() {
  await initializeDatabase();
  const db = getDb();
  
  console.log('\n=== ALL USERS IN DATABASE ===');
  const users = db.prepare('SELECT username, m3u_url, is_active, expiry_date FROM users').all();
  
  if (users.length === 0) {
    console.log('No users found!');
  } else {
    users.forEach(u => {
      console.log(`\n- Username: ${u.username}`);
      console.log(`  M3U URL: ${u.m3u_url || '(EMPTY)'}`);
      console.log(`  Active: ${u.is_active}`);
      console.log(`  Expiry: ${new Date(u.expiry_date).toISOString()}`);
    });
  }
  
  process.exit(0);
}

check();
