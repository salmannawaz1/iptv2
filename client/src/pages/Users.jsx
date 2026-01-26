import { useState, useEffect } from 'react'
import api from '../services/api'
import toast from 'react-hot-toast'
import { 
  Plus, 
  Search, 
  Edit2, 
  Trash2, 
  Copy, 
  Clock,
  MoreVertical,
  X
} from 'lucide-react'

export default function Users() {
  const [users, setUsers] = useState([])
  const [bouquets, setBouquets] = useState([])
  const [playlists, setPlaylists] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editingUser, setEditingUser] = useState(null)
  const [showPlaylistModal, setShowPlaylistModal] = useState(null)

  const [formData, setFormData] = useState({
    username: '',
    password: '',
    max_connections: 1,
    expiry_days: 30,
    notes: '',
    bouquet_ids: [],
    m3u_url: '',
    m3u_playlist_id: ''
  })

  useEffect(() => {
    fetchUsers()
    fetchBouquets()
    fetchPlaylists()
  }, [])

  const fetchUsers = async () => {
    try {
      const response = await api.get('/users')
      setUsers(response.data)
    } catch (error) {
      toast.error('Failed to fetch users')
    } finally {
      setLoading(false)
    }
  }

  const fetchBouquets = async () => {
    try {
      const response = await api.get('/playlists/bouquets')
      setBouquets(response.data)
    } catch (error) {
      console.error('Failed to fetch bouquets')
    }
  }

  const fetchPlaylists = async () => {
    try {
      const response = await api.get('/m3u')
      setPlaylists(response.data)
    } catch (error) {
      console.error('Failed to fetch playlists')
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      if (editingUser) {
        await api.put(`/users/${editingUser.id}`, formData)
        toast.success('User updated')
      } else {
        await api.post('/users', formData)
        toast.success('User created')
      }
      setShowModal(false)
      resetForm()
      fetchUsers()
    } catch (error) {
      toast.error(error.response?.data?.error || 'Operation failed')
    }
  }

  const handleDelete = async (user) => {
    if (!confirm(`Delete user "${user.username}"?`)) return
    try {
      await api.delete(`/users/${user.id}`)
      toast.success('User deleted')
      fetchUsers()
    } catch (error) {
      toast.error('Failed to delete user')
    }
  }

  const handleExtend = async (user) => {
    const days = prompt('Enter days to extend:', '30')
    if (!days) return
    try {
      await api.post(`/users/${user.id}/extend`, { days: parseInt(days) })
      toast.success('Subscription extended')
      fetchUsers()
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to extend')
    }
  }

  const handleToggleStatus = async (user) => {
    try {
      await api.put(`/users/${user.id}`, { is_active: !user.is_active })
      toast.success(`User ${user.is_active ? 'disabled' : 'enabled'}`)
      fetchUsers()
    } catch (error) {
      toast.error('Failed to update status')
    }
  }

  const openEdit = (user) => {
    setEditingUser(user)
    setFormData({
      username: user.username,
      password: '',
      max_connections: user.max_connections,
      expiry_days: 30,
      notes: user.notes || '',
      bouquet_ids: [],
      m3u_url: user.m3u_url || '',
      m3u_playlist_id: user.m3u_playlist_id || ''
    })
    setShowModal(true)
  }

  const resetForm = () => {
    setEditingUser(null)
    setFormData({
      username: '',
      password: '',
      max_connections: 1,
      expiry_days: 30,
      notes: '',
      bouquet_ids: [],
      m3u_url: '',
      m3u_playlist_id: ''
    })
  }

  const showPlaylist = async (user) => {
    try {
      const response = await api.get(`/playlists/m3u/${user.id}`)
      setShowPlaylistModal(response.data)
    } catch (error) {
      toast.error('Failed to get playlist info')
    }
  }

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text)
    toast.success('Copied to clipboard')
  }

  const filteredUsers = users.filter(user =>
    user.username.toLowerCase().includes(search.toLowerCase())
  )

  const getStatusBadge = (user) => {
    if (user.is_expired) return <span className="badge badge-danger">Expired</span>
    if (!user.is_active) return <span className="badge badge-warning">Disabled</span>
    return <span className="badge badge-success">Active</span>
  }

  return (
    <div>
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <h1 className="text-2xl font-bold">Users</h1>
        <button
          onClick={() => { resetForm(); setShowModal(true) }}
          className="btn btn-primary flex items-center gap-2"
        >
          <Plus className="w-4 h-4" /> Add User
        </button>
      </div>

      {/* Search */}
      <div className="relative mb-6">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
        <input
          type="text"
          placeholder="Search users..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="input pl-10"
        />
      </div>

      {/* Users Table */}
      <div className="card overflow-hidden p-0">
        <div className="overflow-x-auto">
          <table className="table">
            <thead>
              <tr>
                <th>Username</th>
                <th>Status</th>
                <th>Connections</th>
                <th>Expiry</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="6" className="text-center py-8">Loading...</td>
                </tr>
              ) : filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan="6" className="text-center py-8 text-gray-400">No users found</td>
                </tr>
              ) : (
                filteredUsers.map((user) => (
                  <tr key={user.id}>
                    <td className="font-medium">{user.username}</td>
                    <td>{getStatusBadge(user)}</td>
                    <td>{user.max_connections}</td>
                    <td>{new Date(user.expiry_date).toLocaleDateString()}</td>
                    <td className="text-gray-400">{new Date(user.created_at).toLocaleDateString()}</td>
                    <td>
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => showPlaylist(user)}
                          className="p-2 hover:bg-gray-700 rounded-lg"
                          title="Get Playlist"
                        >
                          <Copy className="w-4 h-4 text-primary-400" />
                        </button>
                        <button
                          onClick={() => handleExtend(user)}
                          className="p-2 hover:bg-gray-700 rounded-lg"
                          title="Extend"
                        >
                          <Clock className="w-4 h-4 text-green-400" />
                        </button>
                        <button
                          onClick={() => openEdit(user)}
                          className="p-2 hover:bg-gray-700 rounded-lg"
                          title="Edit"
                        >
                          <Edit2 className="w-4 h-4 text-yellow-400" />
                        </button>
                        <button
                          onClick={() => handleDelete(user)}
                          className="p-2 hover:bg-gray-700 rounded-lg"
                          title="Delete"
                        >
                          <Trash2 className="w-4 h-4 text-red-400" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create/Edit Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-gray-800 rounded-xl w-full max-w-md p-6">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-bold">
                {editingUser ? 'Edit User' : 'Create User'}
              </h2>
              <button onClick={() => setShowModal(false)} className="p-1 hover:bg-gray-700 rounded">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="label">Username</label>
                <input
                  type="text"
                  value={formData.username}
                  onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                  className="input"
                  required
                  disabled={!!editingUser}
                />
              </div>

              <div>
                <label className="label">Password {editingUser && '(leave blank to keep)'}</label>
                <input
                  type="text"
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  className="input"
                  required={!editingUser}
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Max Connections</label>
                  <input
                    type="number"
                    value={formData.max_connections}
                    onChange={(e) => setFormData({ ...formData, max_connections: parseInt(e.target.value) })}
                    className="input"
                    min="1"
                  />
                </div>

                {!editingUser && (
                  <div>
                    <label className="label">Expiry (days)</label>
                    <input
                      type="number"
                      value={formData.expiry_days}
                      onChange={(e) => setFormData({ ...formData, expiry_days: parseInt(e.target.value) })}
                      className="input"
                      min="1"
                    />
                  </div>
                )}
              </div>

              <div>
                <label className="label">Notes</label>
                <textarea
                  value={formData.notes}
                  onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                  className="input"
                  rows="2"
                />
              </div>

              <div>
                <label className="label">Assign Playlist</label>
                <select
                  value={formData.m3u_playlist_id}
                  onChange={(e) => setFormData({ ...formData, m3u_playlist_id: e.target.value, m3u_url: '' })}
                  className="input"
                >
                  <option value="">-- Select uploaded playlist --</option>
                  {playlists.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.channel_count} channels)
                    </option>
                  ))}
                </select>
              </div>

              <div className="text-center text-gray-400 text-sm">- OR -</div>

              <div>
                <label className="label">M3U Playlist URL (optional)</label>
                <input
                  type="url"
                  value={formData.m3u_url}
                  onChange={(e) => setFormData({ ...formData, m3u_url: e.target.value, m3u_playlist_id: '' })}
                  className="input"
                  placeholder="https://example.com/playlist.m3u"
                />
                <p className="text-xs text-gray-400 mt-1">Or paste a URL directly</p>
              </div>

              <div className="flex gap-3 pt-4">
                <button type="button" onClick={() => setShowModal(false)} className="btn btn-secondary flex-1">
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary flex-1">
                  {editingUser ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Playlist Modal */}
      {showPlaylistModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-gray-800 rounded-xl w-full max-w-lg p-6">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-bold">Playlist Info</h2>
              <button onClick={() => setShowPlaylistModal(null)} className="p-1 hover:bg-gray-700 rounded">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="label">M3U URL</label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={showPlaylistModal.m3u_url}
                    readOnly
                    className="input flex-1 text-sm"
                  />
                  <button
                    onClick={() => copyToClipboard(showPlaylistModal.m3u_url)}
                    className="btn btn-secondary"
                  >
                    <Copy className="w-4 h-4" />
                  </button>
                </div>
              </div>

              <div className="bg-gray-700/50 p-4 rounded-lg">
                <h3 className="font-medium mb-2">Xtream Codes API</h3>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-400">Server:</span>
                    <span className="font-mono">{showPlaylistModal.xtream?.server}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">Username:</span>
                    <span className="font-mono">{showPlaylistModal.user?.username}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400">Expiry:</span>
                    <span>{new Date(showPlaylistModal.user?.expiry).toLocaleDateString()}</span>
                  </div>
                </div>
              </div>

              <button onClick={() => setShowPlaylistModal(null)} className="btn btn-primary w-full">
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
