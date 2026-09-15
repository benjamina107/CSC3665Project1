import { NavLink, useNavigate } from 'react-router-dom'
import { CalendarDays, ClipboardList, Users, LogOut, PawPrint } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

function NavItem({ to, icon: Icon, label }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all duration-150 ${
          isActive
            ? 'bg-teal-700 text-white shadow-sm'
            : 'text-teal-100 hover:bg-teal-800 hover:text-white'
        }`
      }
    >
      <Icon size={18} />
      {label}
    </NavLink>
  )
}

function Avatar({ name }) {
  const initials = name
    .split(' ')
    .filter(w => /^[A-Z]/.test(w))
    .slice(0, 2)
    .map(w => w[0])
    .join('')
  return (
    <div className="w-9 h-9 rounded-full bg-teal-600 flex items-center justify-center text-white text-sm font-semibold flex-shrink-0">
      {initials}
    </div>
  )
}

export default function Layout({ children }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const isAdmin = user?.role === 'admin'
  const roleLabel =
    user?.role === 'doctor' ? 'Doctor' :
    user?.role === 'technician' ? 'Technician' : 'Administrator'

  return (
    <div className="flex h-screen bg-gray-50 overflow-hidden">
      {/* Sidebar */}
      <aside className="w-64 bg-teal-900 flex flex-col flex-shrink-0">
        {/* Brand */}
        <div className="flex items-center gap-3 px-5 py-6 border-b border-teal-800">
          <div className="w-9 h-9 rounded-xl bg-teal-600 flex items-center justify-center">
            <PawPrint size={20} className="text-white" />
          </div>
          <div>
            <p className="text-white font-bold text-sm leading-tight">ACME Vet Clinic</p>
            <p className="text-teal-400 text-xs">Scheduling System</p>
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-5 space-y-1 overflow-y-auto scrollbar-hide">
          {isAdmin ? (
            <NavItem to="/admin" icon={Users} label="Staff Management" />
          ) : (
            <>
              <NavItem to="/schedule" icon={CalendarDays} label="My Schedule" />
              <NavItem to="/requests" icon={ClipboardList} label="Time Off Requests" />
            </>
          )}
        </nav>

        {/* User footer */}
        <div className="px-3 py-4 border-t border-teal-800">
          <div className="flex items-center gap-3 px-2 py-2 mb-2">
            <Avatar name={user?.name || 'User'} />
            <div className="min-w-0">
              <p className="text-white text-sm font-medium truncate">{user?.name}</p>
              <p className="text-teal-400 text-xs">{roleLabel}</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="flex items-center gap-2 w-full px-4 py-2.5 rounded-xl text-teal-300 hover:bg-teal-800 hover:text-white text-sm font-medium transition-colors"
          >
            <LogOut size={16} />
            Sign Out
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto">
        {children}
      </main>
    </div>
  )
}
