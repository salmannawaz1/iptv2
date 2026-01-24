// Check sallu1 user in database
const { getDb } = require('./src/db/database');

async function checkUser() {
  const db = getDb();
  const user = db.prepare('SELECT * FROM users WHERE username = ?').get('sallu1');
  
  if (user) {
    console.log('User found:');
    console.log('Username:', user.username);
    console.log('M3U URL:', user.m3u_url);
    console.log('Expiry:', new Date(user.expiry_date * 1000).toISOString());
    console.log('Is Reseller:', user.is_reseller);
  } else {
    console.log('User "sallu1" not found');
    console.log('\nAll users:');
    const allUsers = db.prepare('SELECT username, m3u_url FROM users').all();
    allUsers.forEach(u => console.log('-', u.username, ':', u.m3u_url || '(no M3U)'));
  }
}

checkUser();
