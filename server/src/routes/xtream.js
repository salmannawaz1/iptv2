const express = require('express');
const bcrypt = require('bcryptjs');
const axios = require('axios');
const router = express.Router();
const { getDb } = require('../db/database');

// Get M3U content from URL or stored playlist
async function getM3UContent(user) {
  const db = getDb();
  
  // First check if user has a stored playlist assigned
  if (user.m3u_playlist_id) {
    console.log(`[getM3UContent] User ${user.username} has playlist ID: ${user.m3u_playlist_id}`);
    const playlist = db.prepare('SELECT m3u_content, m3u_url FROM m3u_playlists WHERE id = ?').get(user.m3u_playlist_id);
    if (playlist && playlist.m3u_content) {
      console.log(`[getM3UContent] Using stored playlist content (${playlist.m3u_content.length} chars)`);
      return playlist.m3u_content;
    }
    if (playlist && playlist.m3u_url) {
      console.log(`[getM3UContent] Fetching from playlist URL: ${playlist.m3u_url}`);
      const response = await axios.get(playlist.m3u_url);
      return response.data;
    }
  }
  
  // Fall back to user's m3u_url
  if (user.m3u_url) {
    console.log(`[getM3UContent] Fetching from user URL: ${user.m3u_url}`);
    const response = await axios.get(user.m3u_url);
    return response.data;
  }
  
  return null;
}

// Parse M3U content
function parseM3UContent(content) {
  console.log(`[parseM3U] Parsing ${content.length} characters`);
  
  const lines = content.split('\n');
  const channels = [];
  const movies = [];
  const series = [];
  const categories = new Set();
  
  let currentChannel = null;
  let currentGroup = 'Uncategorized';
  let streamId = 1;
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    
    if (line.startsWith('#EXTINF:')) {
      // Parse channel info
      const nameMatch = line.match(/,(.+)$/);
      const groupMatch = line.match(/group-title="([^"]+)"/i);
      const logoMatch = line.match(/tvg-logo="([^"]+)"/i);
      const idMatch = line.match(/tvg-id="([^"]+)"/i);
      
      const name = nameMatch ? nameMatch[1] : `Channel ${streamId}`;
      currentGroup = groupMatch ? groupMatch[1] : 'Uncategorized';
      const logo = logoMatch ? logoMatch[1] : '';
      const tvgId = idMatch ? idMatch[1] : '';
      
      categories.add(currentGroup);
      
      currentChannel = {
        num: streamId,
        name: name,
        stream_type: 'live',
        stream_id: streamId,
        stream_icon: logo,
        epg_channel_id: tvgId,
        added: '',
        category_id: currentGroup,
        custom_sid: '',
        tv_archive: 0,
        direct_source: '',
        tv_archive_duration: 0
      };
      
      streamId++;
    } else if (line && !line.startsWith('#') && currentChannel) {
      // This is the stream URL
      currentChannel.direct_source = line;
      
      // Categorize based on group name
      const groupLower = currentGroup.toLowerCase();
      if (groupLower.includes('movie') || groupLower.includes('vod')) {
        movies.push({ ...currentChannel, stream_type: 'movie' });
      } else if (groupLower.includes('series')) {
        series.push({ ...currentChannel, stream_type: 'series' });
      } else {
        channels.push(currentChannel);
      }
      
      currentChannel = null;
    }
  }
  
  console.log(`[parseM3U] Parsed: ${categories.size} categories, ${channels.length} channels, ${movies.length} movies, ${series.length} series`);
  
  return {
    categories: Array.from(categories),
    channels,
    movies,
    series
  };
}

// Xtream Codes API - Player API endpoint
// GET /player_api.php?username=XXX&password=XXX&action=XXX
router.get('/player_api.php', async (req, res) => {
  try {
    const { username, password, action, category_id, vod_id, series_id, stream_id } = req.query;
    const db = getDb();

    // Authenticate user
    const user = db.prepare('SELECT * FROM users WHERE username = ?').get(username);
    
    if (!user) {
      return res.status(401).json({ user_info: { auth: 0, message: 'Invalid credentials' } });
    }

    // Check password using bcrypt
    const passwordValid = bcrypt.compareSync(password, user.password);
    if (!passwordValid) {
      return res.status(401).json({ user_info: { auth: 0, message: 'Invalid credentials' } });
    }

    // Check if user is active
    if (!user.is_active) {
      return res.status(403).json({ user_info: { auth: 0, message: 'Account disabled' } });
    }

    // Check expiry
    const now = new Date();
    const expiry = new Date(user.expiry_date);
    if (expiry < now) {
      return res.status(403).json({ user_info: { auth: 0, message: 'Subscription expired' } });
    }

    // Handle different actions
    switch (action) {
      case undefined:
      case 'get_user_info':
        return res.json(getUserInfo(user));

      case 'get_live_categories':
        return res.json(await getLiveCategories(user));

      case 'get_live_streams':
        return res.json(await getLiveStreams(user, category_id));

      case 'get_vod_categories':
        return res.json(await getVodCategories(user));

      case 'get_vod_streams':
        return res.json(await getVodStreams(user, category_id));

      case 'get_vod_info':
        return res.json(getVodInfo(vod_id));

      case 'get_series_categories':
        return res.json(await getSeriesCategories(user));

      case 'get_series':
        return res.json(await getSeries(user, category_id));

      case 'get_series_info':
        return res.json(getSeriesInfo(series_id));

      case 'get_short_epg':
      case 'get_simple_data_table':
        return res.json({ epg_listings: [] });

      default:
        return res.json({ error: 'Unknown action' });
    }
  } catch (err) {
    console.error('Xtream API error:', err);
    res.status(500).json({ error: 'Server error' });
  }
});

function getUserInfo(user) {
  const now = new Date();
  const expiry = new Date(user.expiry_date);
  
  return {
    user_info: {
      auth: 1,
      status: 'Active',
      exp_date: Math.floor(expiry.getTime() / 1000).toString(),
      is_trial: '0',
      active_cons: '0',
      created_at: Math.floor(new Date(user.created_at).getTime() / 1000).toString(),
      max_connections: user.max_connections.toString(),
      allowed_output_formats: ['m3u8', 'ts', 'rtmp'],
      username: user.username,
      password: user.password
    },
    server_info: {
      url: process.env.STREAM_SERVER || 'http://localhost:5000',
      port: '5000',
      https_port: '443',
      server_protocol: 'http',
      rtmp_port: '1935',
      timezone: 'UTC',
      timestamp_now: Math.floor(now.getTime() / 1000),
      time_now: now.toISOString()
    }
  };
}

// In-memory cache for M3U data - extended TTL for faster loading
const m3uCache = new Map();
const CACHE_TTL = 6 * 60 * 60 * 1000; // 6 hours - much longer for better performance
const parsedM3UCache = new Map(); // Cache parsed M3U data separately

async function getLiveCategories(user) {
  // Check if user has M3U content (from stored playlist or URL)
  const hasM3U = user.m3u_playlist_id || user.m3u_url;
  
  if (hasM3U) {
    console.log(`[getLiveCategories] User ${user.username} has M3U source`);
    
    // Check cache first
    const cacheKey = `categories_${user.m3u_playlist_id || user.m3u_url}`;
    const cached = m3uCache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
      console.log(`[getLiveCategories] Using cached data (${cached.categories.length} categories)`);
      return cached.categories;
    }
    
    try {
      console.log('[getLiveCategories] Getting M3U content...');
      const content = await getM3UContent(user);
      if (!content) {
        console.log('[getLiveCategories] No M3U content found');
        return getSampleCategories();
      }
      
      const m3uData = parseM3UContent(content);
      console.log(`[getLiveCategories] Parsed ${m3uData.categories.length} categories, ${m3uData.channels.length} channels`);
      
      const categories = m3uData.categories.map((cat, idx) => ({
        category_id: cat,
        category_name: cat,
        parent_id: 0
      }));
      
      // Cache the result
      m3uCache.set(cacheKey, {
        categories,
        timestamp: Date.now()
      });
      
      return categories;
    } catch (err) {
      console.error('[getLiveCategories] Failed to load M3U:', err.message);
    }
  } else {
    console.log(`[getLiveCategories] User ${user.username} has NO M3U source, returning sample data`);
  }
  
  // Return sample live categories
  return getSampleCategories();
}

function getSampleCategories() {
  return [
    { category_id: '1', category_name: 'General', parent_id: 0 },
    { category_id: '2', category_name: 'News', parent_id: 0 },
    { category_id: '3', category_name: 'Sports', parent_id: 0 },
    { category_id: '4', category_name: 'Entertainment', parent_id: 0 },
    { category_id: '5', category_name: 'Movies', parent_id: 0 }
  ];
}

async function getLiveStreams(user, categoryId) {
  // Check if user has M3U content (from stored playlist or URL)
  const hasM3U = user.m3u_playlist_id || user.m3u_url;
  
  if (hasM3U) {
    console.log(`[getLiveStreams] User ${user.username} has M3U source`);
    console.log(`[getLiveStreams] Category filter: ${categoryId || 'none'}`);
    
    // Check cache first
    const cacheKey = `streams_${user.m3u_playlist_id || user.m3u_url}_${categoryId || 'all'}`;
    const cached = m3uCache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
      console.log(`[getLiveStreams] Using cached data (${cached.streams.length} streams)`);
      return cached.streams;
    }
    
    try {
      console.log('[getLiveStreams] Getting M3U content...');
      const content = await getM3UContent(user);
      if (!content) {
        console.log('[getLiveStreams] No M3U content found');
        return getSampleStreams(categoryId);
      }
      
      const m3uData = parseM3UContent(content);
      console.log(`[getLiveStreams] Parsed ${m3uData.channels.length} channels`);
      let streams = m3uData.channels;
      
      if (categoryId) {
        streams = streams.filter(s => s.category_id === categoryId);
        console.log(`[getLiveStreams] After category filter: ${streams.length} channels`);
      }
      
      // Cache the result
      m3uCache.set(cacheKey, {
        streams,
        timestamp: Date.now()
      });
      
      console.log(`[getLiveStreams] Returning ${streams.length} streams`);
      return streams;
    } catch (err) {
      console.error('[getLiveStreams] Failed to load M3U:', err.message);
    }
  } else {
    console.log(`[getLiveStreams] User ${user.username} has NO M3U source, returning sample data`);
  }
  
  // Return sample live streams
  return getSampleStreams(categoryId);
}

function getSampleStreams(categoryId) {
  const streams = [
    { num: 1, name: 'Channel 1', stream_type: 'live', stream_id: 1, stream_icon: '', epg_channel_id: '', added: '', category_id: '1', custom_sid: '', tv_archive: 0, direct_source: 'http://sample-stream.com/live/1.m3u8', tv_archive_duration: 0 },
    { num: 2, name: 'Channel 2', stream_type: 'live', stream_id: 2, stream_icon: '', epg_channel_id: '', added: '', category_id: '1', custom_sid: '', tv_archive: 0, direct_source: 'http://sample-stream.com/live/2.m3u8', tv_archive_duration: 0 },
    { num: 3, name: 'News Live', stream_type: 'live', stream_id: 3, stream_icon: '', epg_channel_id: '', added: '', category_id: '2', custom_sid: '', tv_archive: 0, direct_source: 'http://sample-stream.com/live/news.m3u8', tv_archive_duration: 0 },
    { num: 4, name: 'Sports Channel', stream_type: 'live', stream_id: 4, stream_icon: '', epg_channel_id: '', added: '', category_id: '3', custom_sid: '', tv_archive: 0, direct_source: 'http://sample-stream.com/live/sports.m3u8', tv_archive_duration: 0 }
  ];

  if (categoryId) {
    return streams.filter(s => s.category_id === categoryId);
  }
  return streams;
}

async function getVodCategories(user) {
  const hasM3U = user.m3u_playlist_id || user.m3u_url;
  
  if (hasM3U) {
    console.log(`[getVodCategories] User ${user.username} has M3U source`);
    
    const cacheKey = `vod_categories_${user.m3u_playlist_id || user.m3u_url}`;
    const cached = m3uCache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
      console.log(`[getVodCategories] Using cached data (${cached.data.length} categories)`);
      return cached.data;
    }
    
    try {
      const content = await getM3UContent(user);
      if (content) {
        const m3uData = parseM3UContent(content);
        const movieCategories = new Set();
        m3uData.movies.forEach(m => movieCategories.add(m.category_id));
        const categories = Array.from(movieCategories).map(cat => ({
          category_id: cat,
          category_name: cat,
          parent_id: 0
        }));
        
        m3uCache.set(cacheKey, { data: categories, timestamp: Date.now() });
        console.log(`[getVodCategories] Parsed ${categories.length} movie categories`);
        return categories;
      }
    } catch (err) {
      console.error('[getVodCategories] Failed to load M3U:', err.message);
    }
  }
  
  return [
    { category_id: '10', category_name: 'Action', parent_id: 0 },
    { category_id: '11', category_name: 'Comedy', parent_id: 0 },
    { category_id: '12', category_name: 'Drama', parent_id: 0 },
    { category_id: '13', category_name: 'Horror', parent_id: 0 }
  ];
}

async function getVodStreams(user, categoryId) {
  const hasM3U = user.m3u_playlist_id || user.m3u_url;
  
  if (hasM3U) {
    console.log(`[getVodStreams] User ${user.username} has M3U source`);
    
    const cacheKey = `vod_streams_${user.m3u_playlist_id || user.m3u_url}_${categoryId || 'all'}`;
    const cached = m3uCache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
      console.log(`[getVodStreams] Using cached data (${cached.data.length} movies)`);
      return cached.data;
    }
    
    try {
      const content = await getM3UContent(user);
      if (content) {
        const m3uData = parseM3UContent(content);
        let movies = m3uData.movies.map(m => ({
          ...m,
          rating: '0',
          rating_5based: 0,
          container_extension: 'mp4',
          stream_type: 'movie'
        }));
        
        if (categoryId) {
          movies = movies.filter(m => m.category_id === categoryId);
        }
        
        m3uCache.set(cacheKey, { data: movies, timestamp: Date.now() });
        console.log(`[getVodStreams] Parsed ${movies.length} movies`);
        return movies;
      }
    } catch (err) {
      console.error('[getVodStreams] Failed to load M3U:', err.message);
    }
  }
  
  const movies = [
    { num: 1, name: 'Sample Movie 1', stream_type: 'movie', stream_id: 101, stream_icon: '', rating: '7.5', rating_5based: 3.75, added: '', category_id: '10', container_extension: 'mp4', custom_sid: '', direct_source: 'http://sample-stream.com/movie/1.mp4' },
    { num: 2, name: 'Sample Movie 2', stream_type: 'movie', stream_id: 102, stream_icon: '', rating: '8.0', rating_5based: 4.0, added: '', category_id: '11', container_extension: 'mp4', custom_sid: '', direct_source: 'http://sample-stream.com/movie/2.mp4' }
  ];

  if (categoryId) {
    return movies.filter(m => m.category_id === categoryId);
  }
  return movies;
}

function getVodInfo(vodId) {
  return {
    info: {
      movie_image: '',
      tmdb_id: '',
      name: 'Sample Movie',
      o_name: 'Sample Movie',
      plot: 'This is a sample movie description.',
      cast: 'Actor 1, Actor 2',
      director: 'Director Name',
      genre: 'Action',
      release_date: '2024-01-01',
      duration: '120 min',
      rating: '7.5'
    },
    movie_data: {
      stream_id: vodId,
      container_extension: 'mp4',
      direct_source: 'http://sample-stream.com/movie/' + vodId + '.mp4'
    }
  };
}

async function getSeriesCategories(user) {
  const hasM3U = user.m3u_playlist_id || user.m3u_url;
  
  if (hasM3U) {
    console.log(`[getSeriesCategories] User ${user.username} has M3U source`);
    
    const cacheKey = `series_categories_${user.m3u_playlist_id || user.m3u_url}`;
    const cached = m3uCache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
      console.log(`[getSeriesCategories] Using cached data (${cached.data.length} categories)`);
      return cached.data;
    }
    
    try {
      const content = await getM3UContent(user);
      if (content) {
        const m3uData = parseM3UContent(content);
        const seriesCategories = new Set();
        m3uData.series.forEach(s => seriesCategories.add(s.category_id));
        const categories = Array.from(seriesCategories).map(cat => ({
          category_id: cat,
          category_name: cat,
          parent_id: 0
        }));
        
        m3uCache.set(cacheKey, { data: categories, timestamp: Date.now() });
        console.log(`[getSeriesCategories] Parsed ${categories.length} series categories`);
        return categories;
      }
    } catch (err) {
      console.error('[getSeriesCategories] Failed to load M3U:', err.message);
    }
  }
  
  return [
    { category_id: '20', category_name: 'Drama Series', parent_id: 0 },
    { category_id: '21', category_name: 'Comedy Series', parent_id: 0 },
    { category_id: '22', category_name: 'Action Series', parent_id: 0 }
  ];
}

async function getSeries(user, categoryId) {
  const hasM3U = user.m3u_playlist_id || user.m3u_url;
  
  if (hasM3U) {
    console.log(`[getSeries] User ${user.username} has M3U source`);
    
    const cacheKey = `series_${user.m3u_playlist_id || user.m3u_url}_${categoryId || 'all'}`;
    const cached = m3uCache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
      console.log(`[getSeries] Using cached data (${cached.data.length} series)`);
      return cached.data;
    }
    
    try {
      const content = await getM3UContent(user);
      if (content) {
        const m3uData = parseM3UContent(content);
        let series = m3uData.series.map((s, idx) => ({
          num: idx + 1,
          name: s.name,
          series_id: s.stream_id,
          cover: s.stream_icon,
          plot: '',
          cast: '',
          director: '',
          genre: s.category_id,
          release_date: '',
          rating: '0',
          rating_5based: 0,
          youtube_trailer: '',
          category_id: s.category_id,
          backdrop_path: []
        }));
        
        if (categoryId) {
          series = series.filter(s => s.category_id === categoryId);
        }
        
        m3uCache.set(cacheKey, { data: series, timestamp: Date.now() });
        console.log(`[getSeries] Parsed ${series.length} series`);
        return series;
      }
    } catch (err) {
      console.error('[getSeries] Failed to load M3U:', err.message);
    }
  }
  
  const series = [
    { num: 1, name: 'Sample Series 1', series_id: 201, cover: '', plot: 'A great series', cast: 'Actor 1', director: 'Director', genre: 'Drama', release_date: '2024-01-01', rating: '8.5', rating_5based: 4.25, youtube_trailer: '', category_id: '20', backdrop_path: [] },
    { num: 2, name: 'Sample Series 2', series_id: 202, cover: '', plot: 'Another great series', cast: 'Actor 2', director: 'Director 2', genre: 'Comedy', release_date: '2024-01-01', rating: '7.8', rating_5based: 3.9, youtube_trailer: '', category_id: '21', backdrop_path: [] }
  ];

  if (categoryId) {
    return series.filter(s => s.category_id === categoryId);
  }
  return series;
}

function getSeriesInfo(seriesId) {
  return {
    seasons: [{ season_number: 1, name: 'Season 1' }],
    info: {
      name: 'Sample Series',
      cover: '',
      plot: 'Series description',
      cast: 'Actor 1, Actor 2',
      director: 'Director',
      genre: 'Drama',
      release_date: '2024-01-01',
      rating: '8.0'
    },
    episodes: {
      '1': [
        { id: '1', episode_num: 1, title: 'Episode 1', container_extension: 'mp4', info: { plot: 'Episode 1 plot' }, custom_sid: '', added: '', season: 1, direct_source: 'http://sample-stream.com/series/201/s1e1.mp4' },
        { id: '2', episode_num: 2, title: 'Episode 2', container_extension: 'mp4', info: { plot: 'Episode 2 plot' }, custom_sid: '', added: '', season: 1, direct_source: 'http://sample-stream.com/series/201/s1e2.mp4' }
      ]
    }
  };
}

// Live stream endpoint
router.get('/live/:username/:password/:streamId.ts', (req, res) => {
  const { username, password, streamId } = req.params;
  // In production, validate user and redirect to actual stream
  res.redirect(`http://sample-stream.com/live/${streamId}.m3u8`);
});

// VOD stream endpoint
router.get('/movie/:username/:password/:streamId.:ext', (req, res) => {
  const { username, password, streamId, ext } = req.params;
  // In production, validate user and redirect to actual stream
  res.redirect(`http://sample-stream.com/movie/${streamId}.${ext}`);
});

// Series stream endpoint
router.get('/series/:username/:password/:streamId.:ext', (req, res) => {
  const { username, password, streamId, ext } = req.params;
  // In production, validate user and redirect to actual stream
  res.redirect(`http://sample-stream.com/series/${streamId}.${ext}`);
});

module.exports = router;
