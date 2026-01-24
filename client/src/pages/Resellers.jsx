import { useState, useEffect } from 'react'
import api from '../services/api'
import toast from 'react-hot-toast'
import { 
  Plus, 
  Search, 
  Edit2, 
  Trash2, 
  CreditCard,
  X,
  Users
} from 'lucide-react'

export default function Resellers() {
  const [resellers, setResellers] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editingReseller, setEditingReseller] = useState(null)
  const [showCreditsModal, setShowCreditsModal] = useState(null)
  const [creditsAmount, setCreditsAmount] = useState('')

  const [formData, setFormData] = useState({
    username: '',
    password: '',
    email: '',
    credits: 0,
    max_users: 100
  })

  useEffect(() => {
    fetchResellers()
  }, [])

  const fetchResellers = async () => {
    try {
      const response = await api.get('/resellers')
      setResellers(response.data)
    } catch (error) {
      toast.error('Failed to fetch resellers')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      if (editingReseller) {
        await api.put(`/resellers/${editingReseller.id}`, formData)
        toast.success('Reseller updated')
      } else {
        await api.post('/resellers', formData)
        toast.success('Reseller created')
      }
      setShowModal(false)
      resetForm()
      fetchResellers()
    } catch (error) {
      toast.error(error.response?.data?.error || 'Operation failed')
    }
  }

  const handleDelete = async (reseller) => {
    if (!confirm(`Delete reseller "${reseller.username}" and all their users?`)) return
    try {
      await api.delete(`/resellers/${reseller.id}`)
      toast.success('Reseller deleted')
      fetchResellers()
    } catch (error) {
      toast.error('Failed to delete reseller')
    }
  }

  const handleToggleStatus = async (reseller) => {
    try {
      await api.put(`/resellers/${reseller.id}`, { is_active: !reseller.is_active })
      toast.success(`Reseller ${reseller.is_active ? 'disabled' : 'enabled'}`)
      fetchResellers()
    } catch (error) {
      toast.error('Failed to update status')
    }
  }

  const handleAddCredits = async () => {
    if (!creditsAmount || parseInt(creditsAmount) <= 0) {
      toast.error('Enter valid amount')
      return
    }
    try {
      await api.post(`/resellers/${showCreditsModal.id}/credits`, {
        amount: parseInt(creditsAmount),
        description: 'Credits added by admin'
      })
      toast.success('Credits added')
      setShowCreditsModal(null)
      setCreditsAmount('')
      fetchResellers()
    } catch (error) {
      toast.error('Failed to add credits')
    }
  }

  const openEdit = (reseller) => {
    setEditingReseller(reseller)
    setFormData({
      username: reseller.username,
      password: '',
      email: reseller.email || '',
      credits: reseller.credits,
      max_users: reseller.max_users
    })
    setShowModal(true)
  }

  const resetForm = () => {
    setEditingReseller(null)
    setFormData({
      username: '',
      password: '',
      email: '',
      credits: 0,
      max_users: 100
    })
  }

  const filteredResellers = resellers.filter(r =>
    r.username.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div>
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <h1 className="text-2xl font-bold">Resellers</h1>
        <button
          onClick={() => { resetForm(); setShowModal(true) }}
          className="btn btn-primary flex items-center gap-2"
        >
          <Plus className="w-4 h-4" /> Add Reseller
        </button>
      </div>

      {/* Search */}
      <div className="relative mb-6">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
        <input
          type="text"
          placeholder="Search resellers..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="input pl-10"
        />
      </div>

      {/* Resellers Table */}
      <div className="card overflow-hidden p-0">
        <div className="overflow-x-auto">
          <table className="table">
            <thead>
              <tr>
                <th>Username</th>
                <th>Status</th>
                <th>Credits</th>
                <th>Users</th>
                <th>Max Users</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="7" className="text-center py-8">Loading...</td>
                </tr>
              ) : filteredResellers.length === 0 ? (
                <tr>
                  <td colSpan="7" className="text-center py-8 text-gray-400">No resellers found</td>
                </tr>
              ) : (
                filteredResellers.map((reseller) => (
                  <tr key={reseller.id}>
                    <td className="font-medium">{reseller.username}</td>
                    <td>
                      <button
                        onClick={() => handleToggleStatus(reseller)}
                        className={`badge ${reseller.is_active ? 'badge-success' : 'badge-danger'}`}
                      >
                        {reseller.is_active ? 'Active' : 'Disabled'}
                      </button>
                    </td>
                    <td>
                      <span className="text-yellow-400 font-medium">{reseller.credits}</span>
                    </td>
                    <td>
                      <span className="flex items-center gap-1">
                        <Users className="w-4 h-4 text-gray-400" />
                        {reseller.user_count}
                      </span>
                    </td>
                    <td>{reseller.max_users}</td>
                    <td className="text-gray-400">{new Date(reseller.created_at).toLocaleDateString()}</td>
                    <td>
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => setShowCreditsModal(reseller)}
                          className="p-2 hover:bg-gray-700 rounded-lg"
                          title="Add Credits"
                        >
                          <CreditCard className="w-4 h-4 text-yellow-400" />
                        </button>
                        <button
                          onClick={() => openEdit(reseller)}
                          className="p-2 hover:bg-gray-700 rounded-lg"
                          title="Edit"
                        >
                          <Edit2 className="w-4 h-4 text-blue-400" />
                        </button>
                        <button
                          onClick={() => handleDelete(reseller)}
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
                {editingReseller ? 'Edit Reseller' : 'Create Reseller'}
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
                  disabled={!!editingReseller}
                />
              </div>

              <div>
                <label className="label">Password {editingReseller && '(leave blank to keep)'}</label>
                <input
                  type="text"
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  className="input"
                  required={!editingReseller}
                />
              </div>

              <div>
                <label className="label">Email</label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  className="input"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Credits</label>
                  <input
                    type="number"
                    value={formData.credits}
                    onChange={(e) => setFormData({ ...formData, credits: parseInt(e.target.value) })}
                    className="input"
                    min="0"
                  />
                </div>

                <div>
                  <label className="label">Max Users</label>
                  <input
                    type="number"
                    value={formData.max_users}
                    onChange={(e) => setFormData({ ...formData, max_users: parseInt(e.target.value) })}
                    className="input"
                    min="1"
                  />
                </div>
              </div>

              <div className="flex gap-3 pt-4">
                <button type="button" onClick={() => setShowModal(false)} className="btn btn-secondary flex-1">
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary flex-1">
                  {editingReseller ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Credits Modal */}
      {showCreditsModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-gray-800 rounded-xl w-full max-w-sm p-6">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-bold">Add Credits</h2>
              <button onClick={() => setShowCreditsModal(null)} className="p-1 hover:bg-gray-700 rounded">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4">
              <p className="text-gray-400">
                Adding credits to <span className="text-white font-medium">{showCreditsModal.username}</span>
              </p>
              <p className="text-gray-400">
                Current balance: <span className="text-yellow-400 font-medium">{showCreditsModal.credits}</span>
              </p>

              <div>
                <label className="label">Amount to Add</label>
                <input
                  type="number"
                  value={creditsAmount}
                  onChange={(e) => setCreditsAmount(e.target.value)}
                  className="input"
                  min="1"
                  placeholder="Enter amount"
                />
              </div>

              <div className="flex gap-3">
                <button onClick={() => setShowCreditsModal(null)} className="btn btn-secondary flex-1">
                  Cancel
                </button>
                <button onClick={handleAddCredits} className="btn btn-primary flex-1">
                  Add Credits
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
