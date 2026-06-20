const express = require('express');
const cors = require('cors');
const Database = require('better-sqlite3');
const path = require('path');
const admin = require('firebase-admin');

const app = express();
const PORT = process.env.PORT || 4242;

app.use(cors());
app.use(express.json());

try {
  const serviceAccount = require('./serviceAccountKey.json');
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
  console.log('Firebase Admin initialized');
} catch (e) {
  console.log('No serviceAccountKey.json found - push notifications disabled');
}

const db = new Database(path.join(__dirname, 'kinetic.db'));
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL DEFAULT '',
    photoUri TEXT NOT NULL DEFAULT '',
    fcmToken TEXT NOT NULL DEFAULT '',
    totalVolume REAL NOT NULL DEFAULT 0,
    workoutCount INTEGER NOT NULL DEFAULT 0,
    lastSeen INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL DEFAULT 0
  );

  CREATE TABLE IF NOT EXISTS friendships (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    userId TEXT NOT NULL,
    friendId TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    createdAt INTEGER NOT NULL DEFAULT 0,
    UNIQUE(userId, friendId)
  );
  CREATE INDEX IF NOT EXISTS idx_friends_user ON friendships(userId);
  CREATE INDEX IF NOT EXISTS idx_friends_friend ON friendships(friendId);

  CREATE TABLE IF NOT EXISTS feed_posts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    authorId TEXT NOT NULL,
    content TEXT NOT NULL DEFAULT '',
    activityType TEXT NOT NULL DEFAULT 'post',
    createdAt INTEGER NOT NULL DEFAULT 0
  );
  CREATE INDEX IF NOT EXISTS idx_posts_author ON feed_posts(authorId);

  CREATE TABLE IF NOT EXISTS comments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    postId INTEGER NOT NULL,
    authorId TEXT NOT NULL,
    content TEXT NOT NULL DEFAULT '',
    createdAt INTEGER NOT NULL DEFAULT 0
  );
  CREATE INDEX IF NOT EXISTS idx_comments_post ON comments(postId);

  CREATE TABLE IF NOT EXISTS likes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    postId INTEGER NOT NULL,
    userId TEXT NOT NULL,
    createdAt INTEGER NOT NULL DEFAULT 0,
    UNIQUE(postId, userId)
  );
  CREATE INDEX IF NOT EXISTS idx_likes_post ON likes(postId);

  CREATE TABLE IF NOT EXISTS leaderboard_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    userId TEXT NOT NULL,
    metric TEXT NOT NULL,
    value REAL NOT NULL DEFAULT 0,
    periodStart INTEGER NOT NULL DEFAULT 0,
    periodEnd INTEGER NOT NULL DEFAULT 0,
    UNIQUE(userId, metric, periodStart)
  );
  CREATE INDEX IF NOT EXISTS idx_lb_metric ON leaderboard_entries(metric, value DESC);

  CREATE TABLE IF NOT EXISTS badges (
    key TEXT PRIMARY KEY,
    title TEXT NOT NULL DEFAULT '',
    description TEXT NOT NULL DEFAULT '',
    icon TEXT NOT NULL DEFAULT ''
  );

  CREATE TABLE IF NOT EXISTS user_badges (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    userId TEXT NOT NULL,
    badgeKey TEXT NOT NULL,
    awardedAt INTEGER NOT NULL DEFAULT 0,
    UNIQUE(userId, badgeKey)
  );
  CREATE INDEX IF NOT EXISTS idx_ub_user ON user_badges(userId);

  CREATE TABLE IF NOT EXISTS streaks (
    userId TEXT PRIMARY KEY,
    currentStreak INTEGER NOT NULL DEFAULT 0,
    bestStreak INTEGER NOT NULL DEFAULT 0,
    lastDate INTEGER NOT NULL DEFAULT 0
  );
`);

const SEED_BADGES = [
  { key: 'first_workout', title: 'First Workout', description: 'Completed your first workout', icon: '🏋️' },
  { key: '7day_streak', title: '7-Day Streak', description: 'Trained 7 days in a row', icon: '🔥' },
  { key: '30day_streak', title: '30-Day Streak', description: 'Trained 30 days in a row', icon: '🔥' },
  { key: 'pr_machine', title: 'PR Machine', description: 'Set 10 personal records', icon: '🏆' },
  { key: 'century_club', title: 'Century Club', description: 'Logged 100 workouts', icon: '💯' },
  { key: 'social_butterfly', title: 'Social Butterfly', description: 'Added 10 friends', icon: '🦋' },
  { key: 'helping_hand', title: 'Helping Hand', description: 'Commented on 10 posts', icon: '🤝' },
  { key: '1000kg_club', title: '1000kg Club', description: 'Lifted 1000kg total in one session', icon: '💪' },
];
const insertBadge = db.prepare('INSERT OR IGNORE INTO badges (key, title, description, icon) VALUES (?, ?, ?, ?)');
for (const b of SEED_BADGES) {
  insertBadge.run(b.key, b.title, b.description, b.icon);
}

// =============================================
// USERS
// =============================================

app.post('/users', (req, res) => {
  console.log('Received registration request:', req.body);
  const { id, name, photoUri, fcmToken, totalVolume, workoutCount } = req.body;
  if (!id) return res.status(400).json({ error: 'id required' });
  const now = Date.now();
  db.prepare(`INSERT INTO users (id, name, photoUri, fcmToken, totalVolume, workoutCount, lastSeen, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET name=excluded.name, photoUri=excluded.photoUri, fcmToken=CASE WHEN excluded.fcmToken != '' THEN excluded.fcmToken ELSE users.fcmToken END, totalVolume=CASE WHEN excluded.totalVolume > 0 THEN excluded.totalVolume ELSE users.totalVolume END, workoutCount=CASE WHEN excluded.workoutCount > 0 THEN excluded.workoutCount ELSE users.workoutCount END, lastSeen=excluded.lastSeen`)
    .run(id, name || '', photoUri || '', fcmToken || '', totalVolume || 0, workoutCount || 0, now, now);
  res.json({ id, name, photoUri });
});

app.get('/users/search', (req, res) => {
  const q = (req.query.q || '').toLowerCase().trim();
  if (!q) return res.json([]);
  const normalize = (s) => s.toLowerCase()
    .replace(/ș/g,'s').replace(/ț/g,'t').replace(/ă/g,'a').replace(/â/g,'a').replace(/î/g,'i')
    .replace(/ş/g,'s').replace(/ţ/g,'t');
  const rows = db.prepare('SELECT * FROM users LIMIT 50').all();
  const matches = rows.filter(u => normalize(u.id).includes(q) || normalize(u.name).includes(q));
  res.json(matches.slice(0, 20));
});

app.get('/users/:id', (req, res) => {
  const user = db.prepare('SELECT * FROM users WHERE id = ?').get(req.params.id);
  if (!user) return res.status(404).json({ error: 'not found' });
  res.json(user);
});

app.delete('/users/:id', (req, res) => {
  db.prepare('DELETE FROM users WHERE id = ?').run(req.params.id);
  res.json({ success: true });
});

// =============================================
// FRIENDSHIPS
// =============================================

app.post('/friends/request', (req, res) => {
  const { fromUserId, toUserId } = req.body;
  if (!fromUserId || !toUserId) return res.status(400).json({ error: 'fromUserId and toUserId required' });
  const now = Date.now();
  db.prepare('INSERT OR IGNORE INTO friendships (userId, friendId, status, createdAt) VALUES (?, ?, ?, ?)')
    .run(fromUserId, toUserId, 'pending', now);

  const sender = db.prepare('SELECT name FROM users WHERE id = ?').get(fromUserId);
  const recipient = db.prepare('SELECT fcmToken FROM users WHERE id = ?').get(toUserId);
  if (recipient && recipient.fcmToken && admin.apps.length > 0) {
    admin.messaging().send({
      token: recipient.fcmToken,
      notification: {
        title: 'Friend Request',
        body: `${sender?.name || 'Someone'} sent you a friend request!`,
      },
      data: { type: 'friend_request', fromUserId },
    }).catch(e => console.log('FCM error:', e.message));
  }

  res.json({ success: true });
});

app.get('/friends/incoming/:userId', (req, res) => {
  const rows = db.prepare('SELECT * FROM friendships WHERE friendId = ? AND status = ? ORDER BY createdAt DESC')
    .all(req.params.userId, 'pending');
  res.json(rows);
});

app.post('/friends/accept', (req, res) => {
  const { userId, friendId } = req.body;
  if (!userId || !friendId) return res.status(400).json({ error: 'userId and friendId required' });
  db.prepare('UPDATE friendships SET status = ? WHERE userId = ? AND friendId = ?').run('accepted', friendId, userId);
  db.prepare('INSERT OR IGNORE INTO friendships (userId, friendId, status, createdAt) VALUES (?, ?, ?, ?)').run(userId, friendId, 'accepted', Date.now());
  res.json({ success: true });
});

app.post('/friends/reject', (req, res) => {
  const { userId, friendId } = req.body;
  if (!userId || !friendId) return res.status(400).json({ error: 'userId and friendId required' });
  db.prepare('DELETE FROM friendships WHERE userId = ? AND friendId = ?').run(userId, friendId);
  db.prepare('DELETE FROM friendships WHERE userId = ? AND friendId = ?').run(friendId, userId);
  res.json({ success: true });
});

app.post('/friends/remove', (req, res) => {
  const { userId, friendId } = req.body;
  if (!userId || !friendId) return res.status(400).json({ error: 'userId and friendId required' });
  db.prepare('DELETE FROM friendships WHERE userId = ? AND friendId = ? AND status = ?').run(userId, friendId, 'accepted');
  db.prepare('DELETE FROM friendships WHERE userId = ? AND friendId = ? AND status = ?').run(friendId, userId, 'accepted');
  res.json({ success: true });
});

app.get('/friends/:userId', (req, res) => {
  const rows = db.prepare('SELECT * FROM friendships WHERE (userId = ? OR friendId = ?) AND status = ? ORDER BY createdAt DESC')
    .all(req.params.userId, req.params.userId, 'accepted');
  res.json(rows);
});

// =============================================
// FEED & POSTS
// =============================================

app.post('/posts', (req, res) => {
  const { authorId, content, activityType } = req.body;
  if (!authorId || !content) return res.status(400).json({ error: 'authorId and content required' });
  const now = Date.now();
  const result = db.prepare('INSERT INTO feed_posts (authorId, content, activityType, createdAt) VALUES (?, ?, ?, ?)')
    .run(authorId, content, activityType || 'post', now);
  res.json({ postId: result.lastInsertRowid });
});

app.get('/feed', (req, res) => {
  const limit = parseInt(req.query.limit) || 50;
  const rows = db.prepare('SELECT * FROM feed_posts ORDER BY createdAt DESC LIMIT ?').all(limit);
  res.json(rows);
});

app.get('/posts/author/:authorId', (req, res) => {
  const rows = db.prepare('SELECT * FROM feed_posts WHERE authorId = ? ORDER BY createdAt DESC').all(req.params.authorId);
  res.json(rows);
});

// =============================================
// COMMENTS & LIKES
// =============================================

app.post('/comments', (req, res) => {
  const { postId, authorId, content } = req.body;
  if (!postId || !authorId || !content) return res.status(400).json({ error: 'postId, authorId and content required' });
  const now = Date.now();
  const result = db.prepare('INSERT INTO comments (postId, authorId, content, createdAt) VALUES (?, ?, ?, ?)')
    .run(parseInt(postId), authorId, content, now);
  res.json({ commentId: result.lastInsertRowid });
});

app.get('/comments/:postId', (req, res) => {
  const rows = db.prepare('SELECT * FROM comments WHERE postId = ? ORDER BY createdAt ASC')
    .all(parseInt(req.params.postId));
  res.json(rows);
});

app.post('/posts/:postId/like', (req, res) => {
  const postId = parseInt(req.params.postId);
  const { userId } = req.body;
  if (!userId) return res.status(400).json({ error: 'userId required' });
  const now = Date.now();
  db.prepare('INSERT OR IGNORE INTO likes (postId, userId, createdAt) VALUES (?, ?, ?)').run(postId, userId, now);
  res.json({ success: true });
});

app.delete('/posts/:postId/like', (req, res) => {
  const postId = parseInt(req.params.postId);
  const userId = req.query.userId;
  if (!userId) return res.status(400).json({ error: 'userId required' });
  db.prepare('DELETE FROM likes WHERE postId = ? AND userId = ?').run(postId, userId);
  res.json({ success: true });
});

app.get('/posts/:postId/likes/count', (req, res) => {
  const postId = parseInt(req.params.postId);
  const row = db.prepare('SELECT COUNT(*) as count FROM likes WHERE postId = ?').get(postId);
  res.json({ count: row.count });
});

// =============================================
// LEADERBOARD
// =============================================

app.post('/leaderboard', (req, res) => {
  const { userId, metric, value, periodStart, periodEnd } = req.body;
  if (!userId || !metric) return res.status(400).json({ error: 'userId and metric required' });
  const now = Date.now();
  db.prepare('INSERT INTO leaderboard_entries (userId, metric, value, periodStart, periodEnd) VALUES (?, ?, ?, ?, ?) ON CONFLICT(userId, metric, periodStart) DO UPDATE SET value=excluded.value')
    .run(userId, metric, value || 0, periodStart || now, periodEnd || now);
  res.json({ success: true });
});

app.get('/leaderboard', (req, res) => {
  const metric = req.query.metric || 'workouts';
  const limit = parseInt(req.query.limit) || 50;
  if (metric === 'volume') {
    const rows = db.prepare('SELECT id as userId, name, photoUri, totalVolume as value, workoutCount FROM users WHERE totalVolume > 0 ORDER BY totalVolume DESC LIMIT ?')
      .all(limit);
    res.json(rows);
  } else {
    const rows = db.prepare('SELECT * FROM leaderboard_entries WHERE metric = ? ORDER BY value DESC LIMIT ?')
      .all(metric, limit);
    res.json(rows);
  }
});

// =============================================
// WORKOUTS / STREAKS / BADGES
// =============================================

app.post('/workouts/log', (req, res) => {
  const { userId } = req.body;
  if (!userId) return res.status(400).json({ error: 'userId required' });
  const now = Date.now();

  const streak = db.prepare('SELECT * FROM streaks WHERE userId = ?').get(userId);
  let currentStreak = 1;
  let bestStreak = 1;
  let lastDate = now;

  if (streak) {
    const last = streak.lastDate;
    const diffHours = (now - last) / (1000 * 60 * 60);
    if (diffHours >= 24 && diffHours <= 48) {
      currentStreak = streak.currentStreak + 1;
    } else if (diffHours > 48) {
      currentStreak = 1;
    } else {
      currentStreak = streak.currentStreak;
    }
    bestStreak = Math.max(currentStreak, streak.bestStreak);
    lastDate = now;
  }

  db.prepare('INSERT INTO streaks (userId, currentStreak, bestStreak, lastDate) VALUES (?, ?, ?, ?) ON CONFLICT(userId) DO UPDATE SET currentStreak=excluded.currentStreak, bestStreak=excluded.bestStreak, lastDate=excluded.lastDate')
    .run(userId, currentStreak, bestStreak, lastDate);

  const workoutCount = db.prepare("SELECT COUNT(*) as c FROM feed_posts WHERE authorId = ? AND activityType = 'workout'").get(userId).c;
  const commentCount = db.prepare('SELECT COUNT(*) as c FROM comments WHERE authorId = ?').get(userId).c;
  const friendCount = db.prepare("SELECT COUNT(*) as c FROM friendships WHERE userId = ? AND status = 'accepted'").get(userId).c;

  const newlyAwardedBadges = [];
  const hasBadge = (key) => db.prepare('SELECT 1 FROM user_badges WHERE userId = ? AND badgeKey = ?').get(userId, key);
  const awardBadge = (key) => { db.prepare('INSERT OR IGNORE INTO user_badges (userId, badgeKey, awardedAt) VALUES (?, ?, ?)').run(userId, key, now); newlyAwardedBadges.push(key); };

  if (workoutCount >= 1 && !hasBadge('first_workout')) awardBadge('first_workout');
  if (currentStreak >= 7 && !hasBadge('7day_streak')) awardBadge('7day_streak');
  if (currentStreak >= 30 && !hasBadge('30day_streak')) awardBadge('30day_streak');
  if (workoutCount >= 100 && !hasBadge('century_club')) awardBadge('century_club');
  if (friendCount >= 10 && !hasBadge('social_butterfly')) awardBadge('social_butterfly');
  if (commentCount >= 10 && !hasBadge('helping_hand')) awardBadge('helping_hand');

  const stats = {
    workoutCount,
    commentCount,
    friendCount,
  };

  res.json({
    success: true,
    stats,
    streak: { currentStreak, bestStreak, lastDate },
    newlyAwardedBadges,
  });
});

app.get('/streaks/:userId', (req, res) => {
  const streak = db.prepare('SELECT * FROM streaks WHERE userId = ?').get(req.params.userId);
  res.json(streak || { userId: req.params.userId, currentStreak: 0, bestStreak: 0, lastDate: 0 });
});

app.get('/badges', (_req, res) => {
  const rows = db.prepare('SELECT * FROM badges').all();
  res.json(rows);
});

app.get('/badges/user/:userId', (req, res) => {
  const rows = db.prepare('SELECT * FROM user_badges WHERE userId = ? ORDER BY awardedAt DESC').all(req.params.userId);
  res.json(rows);
});

app.post('/badges/award', (req, res) => {
  const { userId, badgeKey } = req.body;
  if (!userId || !badgeKey) return res.status(400).json({ error: 'userId and badgeKey required' });
  const existing = db.prepare('SELECT 1 FROM user_badges WHERE userId = ? AND badgeKey = ?').get(userId, badgeKey);
  if (existing) return res.json({ success: false, alreadyAwarded: true });
  db.prepare('INSERT INTO user_badges (userId, badgeKey, awardedAt) VALUES (?, ?, ?)').run(userId, badgeKey, Date.now());
  res.json({ success: true });
});

// =============================================
// START
// =============================================

app.get('/', (_req, res) => {
  const userCount = db.prepare('SELECT COUNT(*) as c FROM users').get().c;
  const postCount = db.prepare('SELECT COUNT(*) as c FROM feed_posts').get().c;
  const friendCount = db.prepare('SELECT COUNT(*) as c FROM friendships WHERE status = ?').get('accepted').c;
  res.json({
    name: 'Kinetic API',
    version: '1.0.0',
    status: 'running',
    stats: { users: userCount, posts: postCount, friendships: friendCount },
    endpoints: [
      'POST /users', 'GET /users/:id', 'GET /users/search?q=',
      'POST /friends/request', 'GET /friends/incoming/:userId', 'POST /friends/accept', 'POST /friends/reject', 'GET /friends/:userId',
      'POST /posts', 'GET /feed?limit=', 'GET /posts/author/:authorId',
      'POST /comments', 'GET /comments/:postId', 'POST /posts/:postId/like', 'DELETE /posts/:postId/like', 'GET /posts/:postId/likes/count',
      'POST /leaderboard', 'GET /leaderboard?metric=&limit=',
      'POST /workouts/log', 'GET /streaks/:userId', 'GET /badges', 'GET /badges/user/:userId', 'POST /badges/award'
    ]
  });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Kinetic backend running on http://0.0.0.0:${PORT}`);
});
