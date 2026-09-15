import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import LoginPage from './pages/LoginPage'
import ChangePasswordPage from './pages/admin/ChangePasswordPage'
import AdminDashboard from './pages/admin/AdminDashboard'
import SchedulePage from './pages/staff/SchedulePage'
import RequestsPage from './pages/staff/RequestsPage'
import Layout from './components/Layout'

function RequireAuth({ children, roles }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return children
}

function RootRedirect() {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (user.role === 'admin') {
    return user.isFirstLogin
      ? <Navigate to="/admin/change-password" replace />
      : <Navigate to="/admin" replace />
  }
  return <Navigate to="/schedule" replace />
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<RootRedirect />} />
          <Route path="/login" element={<LoginPage />} />

          <Route
            path="/admin/change-password"
            element={
              <RequireAuth roles={['admin']}>
                <ChangePasswordPage />
              </RequireAuth>
            }
          />

          <Route
            path="/admin"
            element={
              <RequireAuth roles={['admin']}>
                <Layout>
                  <AdminDashboard />
                </Layout>
              </RequireAuth>
            }
          />

          <Route
            path="/schedule"
            element={
              <RequireAuth roles={['doctor', 'technician']}>
                <Layout>
                  <SchedulePage />
                </Layout>
              </RequireAuth>
            }
          />

          <Route
            path="/requests"
            element={
              <RequireAuth roles={['doctor', 'technician']}>
                <Layout>
                  <RequestsPage />
                </Layout>
              </RequireAuth>
            }
          />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
