import { useState } from 'react'
import { Umbrella, Thermometer, CheckCircle2, XCircle, Clock, AlertCircle, CalendarDays, Info } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'

// ─── Mock time-off data (replace with GET /api/requests/my) ──────────────────
const MOCK_INITIAL = {
  vacation: { used: 2, total: 8 },
  sick:     { used: 1, total: 4 },
  history: [
    { id: 1, type: 'Vacation', startDate: 'Oct 10, 2026', endDate: 'Oct 12, 2026', days: 3, status: 'Approved',  deniedReason: null },
    { id: 2, type: 'Sick Day', startDate: 'Sep 5, 2026',  endDate: 'Sep 5, 2026',  days: 1, status: 'Approved',  deniedReason: null },
    { id: 3, type: 'Vacation', startDate: 'Nov 1, 2026',  endDate: 'Nov 2, 2026',  days: 2, status: 'Denied',    deniedReason: 'We will be short on doctors if we grant your request.' },
  ],
}
// ─────────────────────────────────────────────────────────────────────────────

// Minimum advance notice: vacation = 21 days, sick = tomorrow (not today)
function validateRequest(type, startDateStr, endDateStr, balance) {
  const today = new Date('2026-09-15')
  const start = new Date(startDateStr)
  const end   = new Date(endDateStr)

  if (!startDateStr || !endDateStr) return 'Please select date(s).'
  if (end < start) return 'End date cannot be before start date.'

  const diffMs   = end - start + 86400000
  const days     = Math.round(diffMs / 86400000)

  if (type === 'Vacation') {
    const weeksAhead = (start - today) / (1000 * 60 * 60 * 24 * 7)
    if (weeksAhead < 3) return 'Request is too late — vacation requests must be made at least 3 weeks in advance.'
    if (balance.used + days > balance.total) return `You don't have enough vacation time. You have ${balance.total - balance.used} day(s) remaining.`
  }

  if (type === 'Sick Day') {
    const todayStr = today.toISOString().split('T')[0]
    if (startDateStr === todayStr) return "A sick day request cannot be for today — it can be for any other day."
    if (balance.used + days > balance.total) return "You don't have any more sick days."
  }

  return null
}

function daysBetween(start, end) {
  if (!start || !end) return 0
  const s = new Date(start), e = new Date(end)
  return Math.max(1, Math.round((e - s) / 86400000) + 1)
}

function formatDate(isoStr) {
  if (!isoStr) return ''
  const d = new Date(isoStr + 'T00:00:00')
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

function BalanceCard({ icon: Icon, iconBg, label, used, total, color }) {
  const remaining = total - used
  const pct = Math.round((used / total) * 100)
  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${iconBg}`}>
            <Icon size={20} className={color} />
          </div>
          <span className="font-semibold text-gray-800">{label}</span>
        </div>
        <span className="text-2xl font-bold text-gray-900">{remaining}<span className="text-base text-gray-400 font-normal">/{total}</span></span>
      </div>
      <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
        <div
          className={`h-full rounded-full transition-all ${pct > 75 ? 'bg-red-400' : pct > 50 ? 'bg-yellow-400' : color.replace('text-', 'bg-')}`}
          style={{ width: `${pct}%` }}
        />
      </div>
      <div className="flex justify-between mt-2 text-xs text-gray-400">
        <span>{used} used</span>
        <span>{remaining} remaining</span>
      </div>
    </div>
  )
}

const STATUS_CONFIG = {
  Approved: { icon: CheckCircle2, color: 'text-green-600', bg: 'bg-green-50 border-green-200', label: 'Approved' },
  Denied:   { icon: XCircle,      color: 'text-red-500',   bg: 'bg-red-50 border-red-200',     label: 'Denied'   },
  Pending:  { icon: Clock,        color: 'text-yellow-600', bg: 'bg-yellow-50 border-yellow-200', label: 'Pending' },
}

function StatusBadge({ status }) {
  const cfg = STATUS_CONFIG[status] || STATUS_CONFIG.Pending
  const Icon = cfg.icon
  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full border text-xs font-medium ${cfg.bg} ${cfg.color}`}>
      <Icon size={12} />
      {cfg.label}
    </span>
  )
}

export default function RequestsPage() {
  const { user } = useAuth()
  const [data, setData] = useState(MOCK_INITIAL)

  const [activeTab, setActiveTab] = useState('new')
  const [reqType, setReqType] = useState('Vacation')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [note, setNote] = useState('')
  const [result, setResult] = useState(null) // { status: 'Approved'|'Denied', message }

  const days = daysBetween(startDate, endDate)
  const balance = reqType === 'Vacation' ? data.vacation : data.sick

  const handleSubmit = (e) => {
    e.preventDefault()
    setResult(null)

    const validationError = validateRequest(reqType, startDate, endDate, balance)
    if (validationError) {
      // TODO: POST /api/requests → backend returns { granted: false, reason: '...' }
      setResult({ status: 'Denied', message: validationError })
      return
    }

    // TODO: POST /api/requests  { type: reqType, startDate, endDate, note }
    // For now: optimistically grant and update balance
    const newRequest = {
      id: Date.now(),
      type: reqType,
      startDate: formatDate(startDate),
      endDate: formatDate(endDate),
      days,
      status: 'Approved',
      deniedReason: null,
    }

    setData(prev => ({
      ...prev,
      vacation: reqType === 'Vacation' ? { ...prev.vacation, used: prev.vacation.used + days } : prev.vacation,
      sick:     reqType === 'Sick Day'  ? { ...prev.sick,     used: prev.sick.used     + days } : prev.sick,
      history: [newRequest, ...prev.history],
    }))

    setResult({ status: 'Approved', message: `Your ${reqType.toLowerCase()} request for ${formatDate(startDate)}${startDate !== endDate ? ' – ' + formatDate(endDate) : ''} has been approved.` })
    setStartDate('')
    setEndDate('')
    setNote('')
  }

  return (
    <div className="p-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Time Off Requests</h1>
        <p className="text-gray-500 text-sm mt-1">Manage your vacation and sick day requests — {user?.name}</p>
      </div>

      {/* Balance cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
        <BalanceCard
          icon={Umbrella}
          iconBg="bg-teal-100"
          color="text-teal-600"
          label="Vacation Days"
          used={data.vacation.used}
          total={data.vacation.total}
        />
        <BalanceCard
          icon={Thermometer}
          iconBg="bg-rose-100"
          color="text-rose-500"
          label="Sick Days"
          used={data.sick.used}
          total={data.sick.total}
        />
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-gray-100 p-1 rounded-xl w-fit mb-6">
        {['new', 'history'].map(tab => (
          <button
            key={tab}
            onClick={() => { setActiveTab(tab); setResult(null) }}
            className={`px-5 py-2 rounded-lg text-sm font-medium transition-all ${
              activeTab === tab ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            {tab === 'new' ? 'New Request' : `History (${data.history.length})`}
          </button>
        ))}
      </div>

      {/* New Request form */}
      {activeTab === 'new' && (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
          <div className="px-6 py-5 border-b border-gray-50">
            <h2 className="font-semibold text-gray-900">Submit a Request</h2>
          </div>
          <div className="px-6 py-6">
            {/* Result banner */}
            {result && (
              <div className={`flex items-start gap-3 rounded-xl border px-4 py-4 mb-6 text-sm ${
                result.status === 'Approved'
                  ? 'bg-green-50 border-green-200 text-green-800'
                  : 'bg-red-50 border-red-200 text-red-700'
              }`}>
                {result.status === 'Approved'
                  ? <CheckCircle2 size={18} className="flex-shrink-0 mt-0.5 text-green-500" />
                  : <XCircle size={18} className="flex-shrink-0 mt-0.5 text-red-500" />
                }
                <div>
                  <p className="font-semibold mb-0.5">{result.status === 'Approved' ? 'Request Approved' : 'Request Denied'}</p>
                  <p>{result.message}</p>
                </div>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6 max-w-lg">
              {/* Type selector */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Request Type</label>
                <div className="flex gap-3">
                  {['Vacation', 'Sick Day'].map(type => (
                    <button
                      key={type}
                      type="button"
                      onClick={() => setReqType(type)}
                      className={`flex items-center gap-2 px-4 py-2.5 rounded-xl border text-sm font-medium transition-all ${
                        reqType === type
                          ? type === 'Vacation'
                            ? 'bg-teal-700 border-teal-700 text-white shadow-sm'
                            : 'bg-rose-500 border-rose-500 text-white shadow-sm'
                          : 'bg-white border-gray-200 text-gray-600 hover:border-gray-300'
                      }`}
                    >
                      {type === 'Vacation' ? <Umbrella size={15} /> : <Thermometer size={15} />}
                      {type}
                    </button>
                  ))}
                </div>
              </div>

              {/* Date fields */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">Start Date</label>
                  <div className="relative">
                    <CalendarDays size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                    <input
                      type="date"
                      value={startDate}
                      onChange={e => { setStartDate(e.target.value); if (!endDate || e.target.value > endDate) setEndDate(e.target.value) }}
                      required
                      className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent transition"
                    />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">End Date</label>
                  <div className="relative">
                    <CalendarDays size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                    <input
                      type="date"
                      value={endDate}
                      min={startDate}
                      onChange={e => setEndDate(e.target.value)}
                      required
                      className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent transition"
                    />
                  </div>
                </div>
              </div>

              {/* Day count + balance preview */}
              {startDate && endDate && (
                <div className="flex items-center gap-2 bg-gray-50 border border-gray-100 rounded-xl px-4 py-3 text-sm text-gray-600">
                  <Info size={15} className="text-teal-500 flex-shrink-0" />
                  <span>
                    <strong>{days} day{days !== 1 ? 's' : ''}</strong> selected ·{' '}
                    {balance.total - balance.used - days >= 0
                      ? <span className="text-green-600">{balance.total - balance.used - days} day{balance.total - balance.used - days !== 1 ? 's' : ''} remaining after approval</span>
                      : <span className="text-red-500">Not enough {reqType.toLowerCase()} days remaining</span>
                    }
                  </span>
                </div>
              )}

              {/* Notes */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Notes <span className="text-gray-400 font-normal">(optional)</span></label>
                <textarea
                  value={note}
                  onChange={e => setNote(e.target.value)}
                  rows={3}
                  placeholder="Any additional context for your request…"
                  className="w-full px-4 py-3 rounded-xl border border-gray-200 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent transition"
                />
              </div>

              {/* Policy reminders */}
              <div className="bg-blue-50 border border-blue-100 rounded-xl px-4 py-3 space-y-1">
                <p className="text-xs font-semibold text-blue-700 mb-1.5">Policy Reminders</p>
                <div className="flex items-start gap-2 text-xs text-blue-600">
                  <AlertCircle size={13} className="flex-shrink-0 mt-0.5" />
                  Vacation requests must be submitted at least <strong className="mx-1">3 weeks</strong> in advance.
                </div>
                <div className="flex items-start gap-2 text-xs text-blue-600">
                  <AlertCircle size={13} className="flex-shrink-0 mt-0.5" />
                  Sick day requests cannot be for <strong className="mx-1">today</strong>, but can be for any other day.
                </div>
                <div className="flex items-start gap-2 text-xs text-blue-600">
                  <AlertCircle size={13} className="flex-shrink-0 mt-0.5" />
                  Requests are processed immediately on a first-come, first-served basis.
                </div>
              </div>

              <button
                type="submit"
                className="w-full bg-teal-700 hover:bg-teal-800 text-white font-semibold py-3 rounded-xl text-sm transition-colors shadow-sm"
              >
                Submit Request
              </button>
            </form>
          </div>
        </div>
      )}

      {/* History */}
      {activeTab === 'history' && (
        <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
          {data.history.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-gray-400">
              <CalendarDays size={40} className="mb-3 opacity-30" />
              <p className="text-sm">No requests yet</p>
            </div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  {['Type', 'Start', 'End', 'Days', 'Status', 'Note'].map(col => (
                    <th key={col} className="text-left px-5 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider">{col}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {data.history.map(req => (
                  <tr key={req.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-5 py-4">
                      <span className={`inline-flex items-center gap-1.5 text-xs font-medium ${req.type === 'Vacation' ? 'text-teal-700' : 'text-rose-600'}`}>
                        {req.type === 'Vacation' ? <Umbrella size={12} /> : <Thermometer size={12} />}
                        {req.type}
                      </span>
                    </td>
                    <td className="px-5 py-4 text-gray-600">{req.startDate}</td>
                    <td className="px-5 py-4 text-gray-600">{req.endDate}</td>
                    <td className="px-5 py-4 text-gray-500">{req.days}</td>
                    <td className="px-5 py-4"><StatusBadge status={req.status} /></td>
                    <td className="px-5 py-4 text-gray-400 text-xs max-w-xs">
                      {req.deniedReason ?? <span className="text-gray-200">—</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  )
}
