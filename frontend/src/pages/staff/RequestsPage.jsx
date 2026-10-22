import { useState, useEffect } from 'react'
import { Umbrella, Thermometer, CheckCircle2, XCircle, Clock, AlertCircle, CalendarDays, Info } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'


function validateRequest(type, startDateStr, endDateStr, balance) {
  const today = new Date()
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

const SHIFT_LABELS = {
  'Early Morning': 'Early Morning (7:30am – 6:30pm)',
  '8:30 Shift':    '8:30 Shift (8:30am – 7:30pm)',
  'Late Doctor':   'Late Doctor (9:30am – 8:30pm)',
  'Sunday Shift':  'Sunday Shift (8am – 8pm)',
  'Overnight':     'Overnight (8pm – 8am)',
}

// Shifts that run on a given day: Sundays have a different set from Monday-Saturday.
function blocksForDate(isoStr) {
  if (!isoStr) return []
  const isSunday = new Date(isoStr + 'T00:00:00').getDay() === 0
  return isSunday
    ? ['Sunday Shift', 'Overnight']
    : ['Early Morning', '8:30 Shift', 'Late Doctor', 'Overnight']
}

// YYYY-MM-DD in the local time zone (toISOString would use UTC).
function localISO(d) {
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

// Work requests can be made for the generated schedule: tomorrow through 13 days out.
function workDateRange() {
  const min = new Date(); min.setDate(min.getDate() + 1)
  const max = new Date(); max.setDate(max.getDate() + 13)
  return { min: localISO(min), max: localISO(max) }
}

const TYPE_STYLE = {
  'Vacation':   { icon: Umbrella,    text: 'text-teal-700', active: 'bg-teal-700 border-teal-700' },
  'Sick Day':   { icon: Thermometer, text: 'text-rose-600', active: 'bg-rose-500 border-rose-500' },
  'Work Shift': { icon: Clock,       text: 'text-sky-700',  active: 'bg-sky-600 border-sky-600' },
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
  const [data, setData] = useState({
  vacation : { used: 0, total : 8 },
  sick: { used: 0, total: 4 },
  history:[],
})
const[loading, setLoading] = useState(true);
const [error, setError] = useState(null);

useEffect(() => {
  if (!user?.id) return

  async function loadPTO() {
    setLoading(true)
    setError(null)

    try {
      const response = await fetch(
        `/api/requests/my?eid=${user.id}`
      )

      if (!response.ok) {
        throw new Error('Unable to load PTO balances.')
      }

      const balances = await response.json()
      setData(prev => ({ ...prev, ...balances }))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  loadPTO()
}, [user?.id])

  const [activeTab, setActiveTab] = useState('new')
  const [reqType, setReqType] = useState('Vacation')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [workDate, setWorkDate] = useState('')
  const [workBlock, setWorkBlock] = useState('')
  const [note, setNote] = useState('')
  const [result, setResult] = useState(null) // { status: 'Approved'|'Denied', message }

  const days = daysBetween(startDate, endDate)
  const balance = reqType === 'Vacation' ? data.vacation : data.sick
  const workRange = workDateRange()
  const workBlocks = blocksForDate(workDate)

  const submitWorkRequest = async () => {
    if (!workDate || !workBlock) {
      setResult({ status: 'Denied', message: 'Please select a date and a shift.' })
      return
    }

    try {
      const response = await fetch('/api/requests/work', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ eid: user.id, sdate: workDate, block: workBlock }),
      })

      if (!response.ok) {
        const err = await response.json().catch(() => null)
        throw new Error(err?.message ?? 'Unable to submit request.')
      }

      // { approved: true/false, message: '...' }
      const decision = await response.json()
      const status = decision.approved ? 'Approved' : 'Denied'

      const newRequest = {
        id: Date.now(),
        type: 'Work Shift',
        block: workBlock,
        startDate: formatDate(workDate),
        endDate: '—',
        days: '—',
        status,
        deniedReason: decision.approved ? null : decision.message,
      }
      setData(prev => ({ ...prev, history: [newRequest, ...prev.history] }))

      setResult({ status, message: decision.message })
      if (decision.approved) {
        setWorkDate('')
        setWorkBlock('')
        setNote('')
      }
    } catch (err) {
      setResult({ status: 'Denied', message: err.message })
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setResult(null)

    if (reqType === 'Work Shift') {
      await submitWorkRequest()
      return
    }

    if (!startDate || !endDate) {
      setResult({ status: 'Denied', message: 'Please select date(s).' })
      return
    }

    try {
      const reason = reqType === 'Sick Day' ? 'Sick' : 'Vacation'

      const response = await fetch('/api/requests/pto', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ eid: user.id, reason, startDate, endDate }),
      })

      if (!response.ok) {
        const err = await response.json().catch(() => null)
        throw new Error(err?.message ?? 'Unable to submit request.')
      }

      const decision = await response.json()
      const status = decision.approved ? 'Approved' : 'Denied'

      const newRequest = {
        id: Date.now(),
        type: reqType,
        startDate: formatDate(startDate),
        endDate: formatDate(endDate),
        days,
        status,
        deniedReason: decision.approved ? null : decision.message,
      }
      setData(prev => ({ ...prev, history: [newRequest, ...prev.history] }))

      const balanceResponse = await fetch(`/api/requests/my?eid=${user.id}`)
      if (balanceResponse.ok) {
        const balances = await balanceResponse.json()
        setData(prev => ({ ...prev, ...balances }))
      }

      setResult({ status, message: decision.message })
      if (decision.approved) {
        setStartDate('')
        setEndDate('')
        setNote('')
      }
    } catch (err) {
      setResult({ status: 'Denied', message: err.message })
    }
  }


  if (loading) return <p className="p-8">Loading PTO balances...</p>
  if (error) return <p className="p-8">{error}</p>

  return (
    <div className="p-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Time Off Requests</h1>
        <p className="text-gray-500 text-sm mt-1">Manage your time off and shift requests — {user?.name}</p>
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
                  {['Vacation', 'Sick Day', 'Work Shift'].map(type => {
                    const TypeIcon = TYPE_STYLE[type].icon
                    return (
                      <button
                        key={type}
                        type="button"
                        onClick={() => { setReqType(type); setResult(null) }}
                        className={`flex items-center gap-2 px-4 py-2.5 rounded-xl border text-sm font-medium transition-all ${
                          reqType === type
                            ? `${TYPE_STYLE[type].active} text-white shadow-sm`
                            : 'bg-white border-gray-200 text-gray-600 hover:border-gray-300'
                        }`}
                      >
                        <TypeIcon size={15} />
                        {type}
                      </button>
                    )
                  })}
                </div>
              </div>

              {/* Shift fields (work requests) */}
              {reqType === 'Work Shift' && (
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1.5">Date</label>
                    <div className="relative">
                      <CalendarDays size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                      <input
                        type="date"
                        value={workDate}
                        min={workRange.min}
                        max={workRange.max}
                        onChange={e => { setWorkDate(e.target.value); setWorkBlock('') }}
                        required
                        className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent transition"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1.5">Shift</label>
                    <select
                      value={workBlock}
                      onChange={e => setWorkBlock(e.target.value)}
                      required
                      disabled={!workDate}
                      className="w-full px-4 py-2.5 rounded-xl border border-gray-200 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-transparent transition disabled:bg-gray-50 disabled:text-gray-400"
                    >
                      <option value="">{workDate ? 'Select a shift…' : 'Pick a date first'}</option>
                      {workBlocks.map(block => (
                        <option key={block} value={block}>{SHIFT_LABELS[block]}</option>
                      ))}
                    </select>
                  </div>
                </div>
              )}

              {/* Date fields */}
              {reqType !== 'Work Shift' && (
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
              )}

              {/* Day count + balance preview */}
              {reqType !== 'Work Shift' && startDate && endDate && (
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
                  Shift requests are for a shift you are not already working, within the next <strong className="mx-1">two weeks</strong>.
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
                      <span className={`inline-flex items-center gap-1.5 text-xs font-medium ${TYPE_STYLE[req.type].text}`}>
                        {(() => { const TypeIcon = TYPE_STYLE[req.type].icon; return <TypeIcon size={12} /> })()}
                        {req.type}{req.block ? ` · ${req.block}` : ''}
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
