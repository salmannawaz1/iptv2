const express = require('express');
const { getDb } = require('../db/database');
const { authenticateToken, isReseller, isActiveReseller } = require('../middleware/auth');

const router = express.Router();

// Get dashboard statistics
router.get('/dashboard', authenticateToken, isReseller, isActiveReseller, async (req, res) => {
  try {
    const db = getDb();
    const now = new Date().toISOString();
    let stats;

    if (req.user.role === 'admin') {
      // Admin sees everything
      const totalUsers = (await db.prepare('SELECT COUNT(*) as count FROM users').get()).count;
      const activeUsers = (await db.prepare('SELECT COUNT(*) as count FROM users WHERE is_active = 1 AND expiry_date > ?').get(now)).count;
      const expiredUsers = (await db.prepare('SELECT COUNT(*) as count FROM users WHERE expiry_date <= ?').get(now)).count;
      const totalResellers = (await db.prepare('SELECT COUNT(*) as count FROM resellers').get()).count;
      const activeResellers = (await db.prepare('SELECT COUNT(*) as count FROM resellers WHERE is_active = 1').get()).count;

      // Users created today
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const usersToday = (await db.prepare('SELECT COUNT(*) as count FROM users WHERE created_at >= ?').get(today.toISOString())).count;

      // Users created this month
      const monthStart = new Date(today.getFullYear(), today.getMonth(), 1);
      const usersThisMonth = (await db.prepare('SELECT COUNT(*) as count FROM users WHERE created_at >= ?').get(monthStart.toISOString())).count;

      stats = {
        total_users: totalUsers,
        active_users: activeUsers,
        expired_users: expiredUsers,
        disabled_users: totalUsers - activeUsers - expiredUsers,
        total_resellers: totalResellers,
        active_resellers: activeResellers,
        users_today: usersToday,
        users_this_month: usersThisMonth
      };
    } else {
      // Reseller sees only their stats
      const reseller = (await db.prepare('SELECT credits, max_users FROM resellers WHERE id = ?').get(req.user.id));
      const totalUsers = (await db.prepare('SELECT COUNT(*) as count FROM users WHERE reseller_id = ?').get(req.user.id)).count;
      const activeUsers = (await db.prepare('SELECT COUNT(*) as count FROM users WHERE reseller_id = ? AND is_active = 1 AND expiry_date > ?').get(req.user.id, now)).count;
      const expiredUsers = (await db.prepare('SELECT COUNT(*) as count FROM users WHERE reseller_id = ? AND expiry_date <= ?').get(req.user.id, now)).count;

      // Users expiring soon (within 7 days)
      const weekFromNow = new Date();
      weekFromNow.setDate(weekFromNow.getDate() + 7);
      const expiringSoon = (await db.prepare(`
        SELECT COUNT(*) as count FROM users 
        WHERE reseller_id = ? AND expiry_date > ? AND expiry_date <= ?
      `).get(req.user.id, now, weekFromNow.toISOString())).count;

      stats = {
        credits: reseller.credits,
        max_users: reseller.max_users,
        total_users: totalUsers,
        active_users: activeUsers,
        expired_users: expiredUsers,
        expiring_soon: expiringSoon,
        available_slots: reseller.max_users - totalUsers
      };
    }

    res.json(stats);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to fetch statistics' });
  }
});

// Get recent activity
router.get('/activity', authenticateToken, isReseller, isActiveReseller, async (req, res) => {
  try {
    const db = getDb();
    const limit = parseInt(req.query.limit) || 50;
    let logs;

    if (req.user.role === 'admin') {
      logs = await db.prepare(`
        SELECT * FROM activity_logs 
        ORDER BY created_at DESC 
        LIMIT ?
      `).all(limit);
    } else {
      logs = await db.prepare(`
        SELECT * FROM activity_logs 
        WHERE actor_id = ? OR actor_id IN (
          SELECT id FROM users WHERE reseller_id = ?
        )
        ORDER BY created_at DESC 
        LIMIT ?
      `).all(req.user.id, req.user.id, limit);
    }

    res.json(logs.map(log => ({
      ...log,
      details: JSON.parse(log.details || '{}')
    })));
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to fetch activity' });
  }
});

// Get users expiring soon
router.get('/expiring', authenticateToken, isReseller, isActiveReseller, async (req, res) => {
  try {
    const db = getDb();
    const days = parseInt(req.query.days) || 7;
    const now = new Date().toISOString();
    const future = new Date();
    future.setDate(future.getDate() + days);

    let users;

    if (req.user.role === 'admin') {
      users = await db.prepare(`
        SELECT u.*, r.username as reseller_name 
        FROM users u 
        LEFT JOIN resellers r ON u.reseller_id = r.id
        WHERE u.expiry_date > ? AND u.expiry_date <= ?
        ORDER BY u.expiry_date ASC
      `).all(now, future.toISOString());
    } else {
      users = await db.prepare(`
        SELECT * FROM users 
        WHERE reseller_id = ? AND expiry_date > ? AND expiry_date <= ?
        ORDER BY expiry_date ASC
      `).all(req.user.id, now, future.toISOString());
    }

    res.json(users.map(u => {
      const { password, ...user } = u;
      return user;
    }));
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to fetch expiring users' });
  }
});

module.exports = router;
