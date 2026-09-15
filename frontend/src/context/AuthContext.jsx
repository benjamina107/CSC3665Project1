import { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

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
    // TODO: replace with → POST /api/auth/login  { username, password }
    //       expect: { id, name, role, isFirstLogin, token }
    const found = MOCK_USERS.find(u => u.username === username && u.password === password)
    if (!found) throw new Error('Invalid username or password.')
    setUser({ ...found })
    return found
  }

  const logout = () => {
    // TODO: POST /api/auth/logout
    setUser(null)
  }

  const changePassword = async (newPassword) => {
    // TODO: POST /api/auth/change-password  { newPassword }
    setUser(prev => ({ ...prev, isFirstLogin: false }))
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, changePassword }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
