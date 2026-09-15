import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { PawPrint, Eye, EyeOff, AlertCircle } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const user = await login(username.trim(), password)
      if (user.role === 'admin') {
        navigate(user.isFirstLogin ? '/admin/change-password' : '/admin')
      } else {
        navigate('/schedule')
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex">
      {/* Left panel */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-teal-900 via-teal-800 to-teal-700 flex-col justify-between p-12 relative overflow-hidden">
        {/* Decorative circles */}
        <div className="absolute -top-24 -left-24 w-96 h-96 rounded-full bg-teal-600 opacity-20" />
        <div className="absolute -bottom-32 -right-16 w-80 h-80 rounded-full bg-teal-500 opacity-15" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-64 h-64 rounded-full bg-teal-600 opacity-10" />

        {/* Logo */}
        <div className="relative flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-white/10 backdrop-blur flex items-center justify-center">
            <PawPrint size={24} className="text-white" />
          </div>
          <span className="text-white text-xl font-bold tracking-tight">ACME Vet Clinic</span>
        </div>

        {/* Hero text */}
        <div className="relative">
          <h1 className="text-4xl font-bold text-white leading-tight mb-4">
            Smarter scheduling<br />for exceptional care.
          </h1>
          <p className="text-teal-200 text-lg leading-relaxed">
            Manage your shifts, request time off, and coordinate with your team — all in one place.
          </p>
        </div>

        {/* Stats row */}
        <div className="relative flex gap-8">
          {[['10', 'Doctors'], ['20', 'Technicians'], ['24/7', 'Coverage']].map(([val, label]) => (
            <div key={label}>
              <p className="text-white text-2xl font-bold">{val}</p>
              <p className="text-teal-300 text-sm">{label}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Right panel — login form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 bg-gray-50">
        <div className="w-full max-w-md">
          {/* Mobile logo */}
          <div className="lg:hidden flex items-center gap-3 mb-10">
            <div className="w-10 h-10 rounded-xl bg-teal-700 flex items-center justify-center">
              <PawPrint size={20} className="text-white" />
            </div>
            <span className="text-teal-900 text-lg font-bold">ACME Vet Clinic</span>
          </div>

          <h2 className="text-2xl font-bold text-gray-900 mb-1">Welcome back</h2>
          <p className="text-gray-500 text-sm mb-8">Sign in to view your schedule</p>

          {error && (
            <div className="flex items-center gap-2.5 bg-red-50 border border-red-200 text-red-700 rounded-xl px-4 py-3 mb-6 text-sm">
              <AlertCircle size={16} className="flex-shrink-0" />
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">Username</label>
              <input
                type="text"
                value={username}
                onChange={e => setUsername(e.target.value)}
                placeholder="e.g. dr.smith"
                required
                className="w-full px-4 py-3 rounded-xl border border-gray-200 bg-white text-gray-900 text-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent transition"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">Password</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  className="w-full px-4 py-3 pr-12 rounded-xl border border-gray-200 bg-white text-gray-900 text-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent transition"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(v => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-teal-700 hover:bg-teal-800 disabled:opacity-60 text-white font-semibold py-3 rounded-xl text-sm transition-colors shadow-sm mt-2"
            >
              {loading ? 'Signing in…' : 'Sign In'}
            </button>
          </form>

          {/* Demo credentials hint */}
          <div className="mt-8 p-4 bg-teal-50 border border-teal-100 rounded-xl">
            <p className="text-xs font-semibold text-teal-800 mb-2">Demo credentials</p>
            <div className="space-y-1 text-xs text-teal-700">
              <p><span className="font-medium">Admin:</span> admin / admin</p>
              <p><span className="font-medium">Doctor:</span> dr.smith / password123</p>
              <p><span className="font-medium">Technician:</span> tech.johnson / password123</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
