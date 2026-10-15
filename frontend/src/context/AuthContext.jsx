import { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

// Implementation note: match UI roles to backend admin/doctor/technician values.
// Demo login state is stored in user; logout already clears it with setUser(null).

// ─── Mock users (remove when backend is ready) ───────────────────────────────
const MOCK_USERS = [
  { id: 1,  username: 'admin',          password: 'admin',       role: 'admin',      name: 'Administrator',      isFirstLogin: true  },
  { id: 2,  username: 'dr.smith',       password: 'password123', role: 'doctor',     name: 'Dr. Jane Smith',     isFirstLogin: false },
  { id: 3,  username: 'dr.jones',       password: 'password123', role: 'doctor',     name: 'Dr. Michael Jones',  isFirstLogin: false },
  { id: 4,  username: 'dr.chen',        password: 'password123', role: 'doctor',     name: 'Dr. Emily Chen',     isFirstLogin: false },
  { id: 5,  username: 'dr.davis',       password: 'password123', role: 'doctor',     name: 'Dr. Robert Davis',   isFirstLogin: false },
  { id: 6,  username: 'dr.martinez',    password: 'password123', role: 'doctor',     name: 'Dr. Lisa Martinez',  isFirstLogin: false },
  { id: 7,  username: 'tech.johnson',   password: 'password123', role: 'technician', name: 'Alex Johnson',       isFirstLogin: false },
  { id: 8,  username: 'tech.williams',  password: 'password123', role: 'technician', name: 'Sam Williams',       isFirstLogin: false },
  { id: 9,  username: 'tech.brown',     password: 'password123', role: 'technician', name: 'Casey Brown',        isFirstLogin: false },
  { id: 10, username: 'tech.lee',       password: 'password123', role: 'technician', name: 'Morgan Lee',         isFirstLogin: false },
]
// ─────────────────────────────────────────────────────────────────────────────

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)

  const login = async (username, password) => {
    const params = new URLSearchParams({
      user: username,
      pass: password,
    })

    const response = await fetch(`/api/auth/login?${params}`)

    if (!response.ok) {
      throw new Error('Invalid username or password.')
    }

    const employee = await response.json()
    setUser(employee)
    return employee
  }

  const logout = () => {
    setUser(null)
  }

  const changePassword = async (newPassword) => {
    const response = await fetch('/api/auth/changePassword', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        eid: user.id,
        newPassword: newPassword,
      }),
    })

    if (!response.ok) {
      throw new Error('Failed to change password.')
    }
    setUser(prev => ({ ...prev, isFirstLogin: false }))
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, changePassword }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
