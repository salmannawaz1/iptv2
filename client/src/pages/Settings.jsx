import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'
import toast from 'react-hot-toast'
import { Key, User, Shield } from 'lucide-react'

export default function Settings() {
  const { user } = useAuth()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChangePassword = async (e) => {
    e.preventDefault()
    
    if (newPassword !== confirmPassword) {
      toast.error('Passwords do not match')
      return
    }

    if (newPassword.length < 6) {
      toast.error('Password must be at least 6 characters')
      return
    }

    setLoading(true)
    try {
      await api.post('/auth/change-password', {
        currentPassword,
        newPassword
      })
      toast.success('Password changed successfully')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (error) {
      toast.error(error.response?.data?.error || 'Failed to change password')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Settings</h1>

      <div className="grid gap-6 max-w-2xl">
        {/* Account Info */}
        <div className="card">
          <div className="flex items-center gap-3 mb-4">
            <User className="w-5 h-5 text-primary-400" />
            <h2 className="text-lg font-semibold">Account Information</h2>
          </div>
          
          <div className="space-y-3">
            <div className="flex justify-between py-2 border-b border-gray-700">
              <span className="text-gray-400">Username</span>
              <span className="font-medium">{user?.username}</span>
            </div>
            <div className="flex justify-between py-2 border-b border-gray-700">
              <span className="text-gray-400">Role</span>
              <span className="badge badge-info capitalize">{user?.role}</span>
            </div>
            {user?.role === 'reseller' && (
              <>
                <div className="flex justify-between py-2 border-b border-gray-700">
                  <span className="text-gray-400">Credits</span>
                  <span className="text-yellow-400 font-bold">{user?.credits || 0}</span>
                </div>
                <div className="flex justify-between py-2 border-b border-gray-700">
                  <span className="text-gray-400">Max Users</span>
                  <span>{user?.max_users || 0}</span>
                </div>
              </>
            )}
            <div className="flex justify-between py-2">
              <span className="text-gray-400">Email</span>
              <span>{user?.email || 'Not set'}</span>
            </div>
          </div>
        </div>

        {/* Change Password */}
        <div className="card">
          <div className="flex items-center gap-3 mb-4">
            <Key className="w-5 h-5 text-primary-400" />
            <h2 className="text-lg font-semibold">Change Password</h2>
          </div>

          <form onSubmit={handleChangePassword} className="space-y-4">
            <div>
              <label className="label">Current Password</label>
              <input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                className="input"
                required
              />
            </div>

            <div>
              <label className="label">New Password</label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="input"
                required
                minLength={6}
              />
            </div>

            <div>
              <label className="label">Confirm New Password</label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="input"
                required
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary"
            >
              {loading ? 'Changing...' : 'Change Password'}
            </button>
          </form>
        </div>

        {/* API Info for Resellers */}
        {user?.role === 'reseller' && (
          <div className="card">
            <div className="flex items-center gap-3 mb-4">
              <Shield className="w-5 h-5 text-primary-400" />
              <h2 className="text-lg font-semibold">API Information</h2>
            </div>

            <div className="bg-gray-700/50 p-4 rounded-lg">
              <p className="text-sm text-gray-400 mb-3">
                Use this endpoint to validate user credentials from your IPTV app:
              </p>
              <code className="block bg-gray-900 p-3 rounded text-sm text-green-400 break-all">
                POST /api/playlists/validate
              </code>
              <p className="text-xs text-gray-500 mt-2">
                Send username and password in request body to validate IPTV users.
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
