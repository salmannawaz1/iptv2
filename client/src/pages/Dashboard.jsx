import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'
import { 
  Users, 
  UserCheck, 
  UserX, 
  Clock,
  CreditCard,
  TrendingUp,
  AlertTriangle
} from 'lucide-react'

export default function Dashboard() {
  const { user } = useAuth()
  const [stats, setStats] = useState(null)
  const [expiring, setExpiring] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    try {
      const [statsRes, expiringRes] = await Promise.all([
        api.get('/stats/dashboard'),
        api.get('/stats/expiring?days=7')
      ])
      setStats(statsRes.data)
      setExpiring(expiringRes.data)
    } catch (error) {
      console.error('Failed to fetch dashboard data', error)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-500"></div>
      </div>
    )
  }

  const isAdmin = user?.role === 'admin'

  const statCards = isAdmin ? [
    { label: 'Total Users', value: stats?.total_users || 0, icon: Users, color: 'blue' },
    { label: 'Active Users', value: stats?.active_users || 0, icon: UserCheck, color: 'green' },
    { label: 'Expired Users', value: stats?.expired_users || 0, icon: UserX, color: 'red' },
    { label: 'Total Resellers', value: stats?.total_resellers || 0, icon: TrendingUp, color: 'purple' },
  ] : [
    { label: 'Credits', value: stats?.credits || 0, icon: CreditCard, color: 'yellow' },
    { label: 'Total Users', value: stats?.total_users || 0, icon: Users, color: 'blue' },
    { label: 'Active Users', value: stats?.active_users || 0, icon: UserCheck, color: 'green' },
    { label: 'Expiring Soon', value: stats?.expiring_soon || 0, icon: Clock, color: 'orange' },
  ]

  const colorClasses = {
    blue: 'bg-blue-900/30 text-blue-400',
    green: 'bg-green-900/30 text-green-400',
    red: 'bg-red-900/30 text-red-400',
    purple: 'bg-purple-900/30 text-purple-400',
    yellow: 'bg-yellow-900/30 text-yellow-400',
    orange: 'bg-orange-900/30 text-orange-400',
  }

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Dashboard</h1>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {statCards.map((stat) => (
          <div key={stat.label} className="card">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-400 text-sm">{stat.label}</p>
                <p className="text-3xl font-bold mt-1">{stat.value}</p>
              </div>
              <div className={`p-3 rounded-xl ${colorClasses[stat.color]}`}>
                <stat.icon className="w-6 h-6" />
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Expiring Soon */}
      {expiring.length > 0 && (
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <AlertTriangle className="w-5 h-5 text-yellow-500" />
            <h2 className="text-lg font-semibold">Expiring Within 7 Days</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="table">
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Expiry Date</th>
                  <th>Connections</th>
                  {isAdmin && <th>Reseller</th>}
                </tr>
              </thead>
              <tbody>
                {expiring.map((user) => (
                  <tr key={user.id}>
                    <td className="font-medium">{user.username}</td>
                    <td>
                      <span className="badge badge-warning">
                        {new Date(user.expiry_date).toLocaleDateString()}
                      </span>
                    </td>
                    <td>{user.max_connections}</td>
                    {isAdmin && <td className="text-gray-400">{user.reseller_name}</td>}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Quick Info for Resellers */}
      {!isAdmin && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div className="card">
            <h2 className="text-lg font-semibold mb-4">Account Info</h2>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-400">Max Users</span>
                <span>{stats?.max_users || 0}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400">Used Slots</span>
                <span>{stats?.total_users || 0}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400">Available Slots</span>
                <span className="text-green-400">{stats?.available_slots || 0}</span>
              </div>
            </div>
          </div>

          <div className="card">
            <h2 className="text-lg font-semibold mb-4">Quick Actions</h2>
            <div className="space-y-2">
              <a href="/users" className="block btn btn-primary text-center">
                Create New User
              </a>
              <a href="/settings" className="block btn btn-secondary text-center">
                View Credit History
              </a>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
