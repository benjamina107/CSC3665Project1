import { useState } from 'react'
import { Plus, Trash2, KeyRound, Users, Stethoscope, X, AlertCircle, CheckCircle2 } from 'lucide-react'

// ─── Mock data (remove when backend is ready) ─────────────────────────────────
const INITIAL_DOCTORS = [
  { id: 1,  name: 'Dr. Jane Smith',       username: 'dr.smith',    lastLogin: 'Sep 15, 2026' },
  { id: 2,  name: 'Dr. Michael Jones',    username: 'dr.jones',    lastLogin: 'Sep 14, 2026' },
  { id: 3,  name: 'Dr. Emily Chen',       username: 'dr.chen',     lastLogin: 'Sep 15, 2026' },
  { id: 4,  name: 'Dr. Robert Davis',     username: 'dr.davis',    lastLogin: 'Sep 13, 2026' },
  { id: 5,  name: 'Dr. Lisa Martinez',    username: 'dr.martinez', lastLogin: 'Sep 12, 2026' },
  { id: 6,  name: 'Dr. James Wilson',     username: 'dr.wilson',   lastLogin: 'Sep 15, 2026' },
  { id: 7,  name: 'Dr. Sarah Taylor',     username: 'dr.taylor',   lastLogin: 'Sep 11, 2026' },
  { id: 8,  name: 'Dr. David Anderson',   username: 'dr.anderson', lastLogin: 'Sep 14, 2026' },
  { id: 9,  name: 'Dr. Jennifer Thomas',  username: 'dr.thomas',   lastLogin: 'Sep 10, 2026' },
  { id: 10, name: 'Dr. Christopher Lee',  username: 'dr.lee',      lastLogin: 'Sep 15, 2026' },
]

const INITIAL_TECHNICIANS = [
  { id: 11, name: 'Alex Johnson',    username: 'tech.johnson',  lastLogin: 'Sep 15, 2026' },
  { id: 12, name: 'Sam Williams',    username: 'tech.williams', lastLogin: 'Sep 15, 2026' },
  { id: 13, name: 'Casey Brown',     username: 'tech.brown',    lastLogin: 'Sep 14, 2026' },
  { id: 14, name: 'Morgan Lee',      username: 'tech.lee',      lastLogin: 'Sep 14, 2026' },
  { id: 15, name: 'Jordan Garcia',   username: 'tech.garcia',   lastLogin: 'Sep 13, 2026' },
  { id: 16, name: 'Riley Harris',    username: 'tech.harris',   lastLogin: 'Sep 13, 2026' },
  { id: 17, name: 'Avery Clark',     username: 'tech.clark',    lastLogin: 'Sep 12, 2026' },
  { id: 18, name: 'Drew Lewis',      username: 'tech.lewis',    lastLogin: 'Sep 12, 2026' },
  { id: 19, name: 'Blake Robinson',  username: 'tech.robinson', lastLogin: 'Sep 11, 2026' },
  { id: 20, name: 'Quinn Walker',    username: 'tech.walker',   lastLogin: 'Sep 11, 2026' },
  { id: 21, name: 'Cameron Hall',    username: 'tech.hall',     lastLogin: 'Sep 10, 2026' },
  { id: 22, name: 'Reese Allen',     username: 'tech.allen',    lastLogin: 'Sep 10, 2026' },
  { id: 23, name: 'Skyler Young',    username: 'tech.young',    lastLogin: 'Sep 9, 2026'  },
  { id: 24, name: 'Dakota King',     username: 'tech.king',     lastLogin: 'Sep 9, 2026'  },
  { id: 25, name: 'Finley Wright',   username: 'tech.wright',   lastLogin: 'Sep 8, 2026'  },
  { id: 26, name: 'Harley Scott',    username: 'tech.scott',    lastLogin: 'Sep 8, 2026'  },
  { id: 27, name: 'Jamie Torres',    username: 'tech.torres',   lastLogin: 'Sep 7, 2026'  },
  { id: 28, name: 'Kerry Nguyen',    username: 'tech.nguyen',   lastLogin: 'Sep 7, 2026'  },
  { id: 29, name: 'Logan Hill',      username: 'tech.hill',     lastLogin: 'Sep 6, 2026'  },
  { id: 30, name: 'Peyton Adams',    username: 'tech.adams',    lastLogin: 'Sep 6, 2026'  },
]
// ─────────────────────────────────────────────────────────────────────────────

function StatCard({ icon: Icon, value, label, color }) {
  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 flex items-center gap-5">
      <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${color}`}>
        <Icon size={24} className="text-white" />
      </div>
      <div>
        <p className="text-3xl font-bold text-gray-900">{value}</p>
        <p className="text-sm text-gray-500">{label}</p>
      </div>
    </div>
  )
}

function StaffTable({ rows, onRemove, onResetPassword }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-gray-50 border-b border-gray-100">
            <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Name</th>
            <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Username</th>
            <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Last Login</th>
            <th className="px-4 py-3" />
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-50">
          {rows.map(person => (
            <tr key={person.id} className="hover:bg-gray-50 transition-colors group">
              <td className="px-4 py-3.5">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-full bg-teal-100 flex items-center justify-center text-teal-700 text-xs font-semibold flex-shrink-0">
                    {person.name.split(' ').filter(w => /^[A-Z]/.test(w)).slice(0, 2).map(w => w[0]).join('')}
                  </div>
                  <span className="font-medium text-gray-900">{person.name}</span>
                </div>
              </td>
              <td className="px-4 py-3.5 text-gray-500 font-mono text-xs">{person.username}</td>
              <td className="px-4 py-3.5 text-gray-400">{person.lastLogin}</td>
              <td className="px-4 py-3.5">
                <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button
                    onClick={() => onResetPassword(person)}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-blue-600 hover:bg-blue-50 transition-colors"
                  >
                    <KeyRound size={13} />
                    Reset Password
                  </button>
                  <button
                    onClick={() => onRemove(person)}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-red-500 hover:bg-red-50 transition-colors"
                  >
                    <Trash2 size={13} />
                    Remove
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function AddStaffPanel({ role, onClose, onAdd }) {
  const [name, setName] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)

  const handleSubmit = (e) => {
    e.preventDefault()
    setError('')
    if (!name || !username || !password) { setError('All fields are required.'); return }
    if (password.length < 6) { setError('Password must be at least 6 characters.'); return }
    // TODO: POST /api/admin/staff  { name, username, password, role }
    onAdd({ id: Date.now(), name, username, lastLogin: 'Never' })
    setSuccess(true)
    setTimeout(() => { setSuccess(false); setName(''); setUsername(''); setPassword('') }, 2000)
  }

  return (
    <div className="fixed inset-0 z-50 flex">
      <div className="flex-1 bg-black/30 backdrop-blur-sm" onClick={onClose} />
      <div className="w-full max-w-md bg-white h-full shadow-2xl flex flex-col animate-in slide-in-from-right">
        <div className="flex items-center justify-between px-6 py-5 border-b border-gray-100">
          <h2 className="font-semibold text-gray-900">Add New {role === 'doctor' ? 'Doctor' : 'Technician'}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition-colors">
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex-1 px-6 py-6 space-y-5 overflow-y-auto">
          {error && (
            <div className="flex items-center gap-2 bg-red-50 border border-red-200 text-red-700 rounded-xl px-4 py-3 text-sm">
              <AlertCircle size={15} />
              {error}
            </div>
          )}
          {success && (
            <div className="flex items-center gap-2 bg-green-50 border border-green-200 text-green-700 rounded-xl px-4 py-3 text-sm">
              <CheckCircle2 size={15} />
              Staff member added successfully!
            </div>
          )}

          {[
            { label: 'Full Name', value: name, setter: setName, placeholder: role === 'doctor' ? 'Dr. First Last' : 'First Last', type: 'text' },
            { label: 'Username', value: username, setter: setUsername, placeholder: role === 'doctor' ? 'dr.lastname' : 'tech.lastname', type: 'text' },
            { label: 'Temporary Password', value: password, setter: setPassword, placeholder: 'Min. 6 characters', type: 'password' },
          ].map(({ label, value, setter, placeholder, type }) => (
            <div key={label}>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">{label}</label>
              <input
                type={type}
                value={value}
                onChange={e => setter(e.target.value)}
                placeholder={placeholder}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent transition"
              />
            </div>
          ))}

          <p className="text-xs text-gray-400">
            The staff member will be asked to change their password on first login.
          </p>
        </form>

        <div className="px-6 py-4 border-t border-gray-100 flex gap-3">
          <button onClick={onClose} className="flex-1 px-4 py-2.5 rounded-xl border border-gray-200 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors">
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            className="flex-1 px-4 py-2.5 rounded-xl bg-teal-700 text-white text-sm font-medium hover:bg-teal-800 transition-colors shadow-sm"
          >
            Add Staff Member
          </button>
        </div>
      </div>
    </div>
  )
}

function ConfirmModal({ person, action, onConfirm, onCancel }) {
  const isRemove = action === 'remove'
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onCancel} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-sm p-6">
        <div className={`w-12 h-12 rounded-2xl flex items-center justify-center mb-4 ${isRemove ? 'bg-red-100' : 'bg-blue-100'}`}>
          {isRemove ? <Trash2 size={22} className="text-red-600" /> : <KeyRound size={22} className="text-blue-600" />}
        </div>
        <h3 className="text-lg font-semibold text-gray-900 mb-1">
          {isRemove ? 'Remove Staff Member' : 'Reset Password'}
        </h3>
        <p className="text-sm text-gray-500 mb-6">
          {isRemove
            ? `Are you sure you want to remove ${person.name}? This action cannot be undone.`
            : `Reset password for ${person.name}? They will be required to set a new password on next login.`}
        </p>
        <div className="flex gap-3">
          <button onClick={onCancel} className="flex-1 px-4 py-2.5 rounded-xl border border-gray-200 text-sm font-medium text-gray-600 hover:bg-gray-50">
            Cancel
          </button>
          <button
            onClick={onConfirm}
            className={`flex-1 px-4 py-2.5 rounded-xl text-white text-sm font-medium shadow-sm ${isRemove ? 'bg-red-500 hover:bg-red-600' : 'bg-blue-600 hover:bg-blue-700'}`}
          >
            {isRemove ? 'Remove' : 'Reset'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('doctors')
  const [doctors, setDoctors] = useState(INITIAL_DOCTORS)
  const [technicians, setTechnicians] = useState(INITIAL_TECHNICIANS)
  const [showAddPanel, setShowAddPanel] = useState(false)
  const [confirmAction, setConfirmAction] = useState(null)

  const currentList = activeTab === 'doctors' ? doctors : technicians
  const setCurrentList = activeTab === 'doctors' ? setDoctors : setTechnicians

  const handleAdd = (person) => {
    setCurrentList(prev => [...prev, person])
  }

  const handleRemove = (person) => {
    // TODO: DELETE /api/admin/staff/:id
    setCurrentList(prev => prev.filter(p => p.id !== person.id))
    setConfirmAction(null)
  }

  const handleResetPassword = () => {
    // TODO: POST /api/admin/staff/:id/reset-password
    setConfirmAction(null)
  }

  return (
    <div className="p-8">
      {/* Page header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Staff Management</h1>
        <p className="text-gray-500 text-sm mt-1">Manage clinic doctors and technicians</p>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
        <StatCard icon={Stethoscope} value={doctors.length} label="Doctors" color="bg-teal-600" />
        <StatCard icon={Users} value={technicians.length} label="Technicians" color="bg-indigo-500" />
      </div>

      {/* Table card */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        {/* Card header: tabs + add button */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <div className="flex gap-1 bg-gray-100 p-1 rounded-xl">
            {['doctors', 'technicians'].map(tab => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-all ${
                  activeTab === tab
                    ? 'bg-white text-gray-900 shadow-sm'
                    : 'text-gray-500 hover:text-gray-700'
                }`}
              >
                {tab === 'doctors' ? `Doctors (${doctors.length})` : `Technicians (${technicians.length})`}
              </button>
            ))}
          </div>

          <button
            onClick={() => setShowAddPanel(true)}
            className="flex items-center gap-2 px-4 py-2 bg-teal-700 hover:bg-teal-800 text-white text-sm font-medium rounded-xl transition-colors shadow-sm"
          >
            <Plus size={16} />
            Add {activeTab === 'doctors' ? 'Doctor' : 'Technician'}
          </button>
        </div>

        <StaffTable
          rows={currentList}
          onRemove={p => setConfirmAction({ person: p, type: 'remove' })}
          onResetPassword={p => setConfirmAction({ person: p, type: 'reset' })}
        />
      </div>

      {/* Add staff slide-over */}
      {showAddPanel && (
        <AddStaffPanel
          role={activeTab === 'doctors' ? 'doctor' : 'technician'}
          onClose={() => setShowAddPanel(false)}
          onAdd={handleAdd}
        />
      )}

      {/* Confirm modal */}
      {confirmAction && (
        <ConfirmModal
          person={confirmAction.person}
          action={confirmAction.type}
          onConfirm={confirmAction.type === 'remove' ? () => handleRemove(confirmAction.person) : handleResetPassword}
          onCancel={() => setConfirmAction(null)}
        />
      )}
    </div>
  )
}
