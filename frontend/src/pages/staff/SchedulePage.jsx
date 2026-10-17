import { useEffect, useMemo, useState } from 'react'
import { Clock, Users, CalendarDays, Scissors, Loader2, AlertCircle } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'

// Schedule comes from GET /api/schedule?eid=<user.id> (today + 13 days). It is fetched every
// time this page mounts, so a work/PTO request granted on the Requests page (which rearranges
// the working table on the server) shows up as soon as the employee navigates back here.

// ─── Shift styling config ─────────────────────────────────────────────────────
// Keys are the exact shifts.block values from the database schema. 'Surgery' is a display
// variant for a doctor's shift where shifts.surgery = true (it is not a block value).
const SHIFT_CONFIG = {
  'Early Morning': { bg: 'bg-teal-50',   badge: 'bg-teal-100 text-teal-800 border-teal-200',       dot: 'bg-teal-500',   label: 'Early Morning', time: '7:30 am – 6:30 pm' },
  '8:30 Shift':    { bg: 'bg-blue-50',   badge: 'bg-blue-100 text-blue-800 border-blue-200',       dot: 'bg-blue-500',   label: '8:30 Shift',    time: '8:30 am – 7:30 pm' },
  'Late Doctor':   { bg: 'bg-indigo-50', badge: 'bg-indigo-100 text-indigo-800 border-indigo-200', dot: 'bg-indigo-500', label: 'Late Doctor',   time: '9:30 am – 8:30 pm' },
  'Sunday Shift':  { bg: 'bg-purple-50', badge: 'bg-purple-100 text-purple-800 border-purple-200', dot: 'bg-purple-500', label: 'Sunday Shift',  time: '8:00 am – 8:00 pm' },
  'Overnight':     { bg: 'bg-slate-100', badge: 'bg-slate-200 text-slate-700 border-slate-300',    dot: 'bg-slate-500',  label: 'Overnight',     time: '8:00 pm – 8:00 am (next day)' },
  'Surgery':       { bg: 'bg-amber-50',  badge: 'bg-amber-100 text-amber-800 border-amber-300',    dot: 'bg-amber-500',  label: 'Surgery',       time: '7:30 am – 6:30 pm' },
  'Off':           { bg: 'bg-white',     badge: 'bg-gray-100 text-gray-400 border-gray-200',       dot: 'bg-gray-300',   label: 'Day Off',       time: null },
}

// The style key for an assignment: doctors' surgery shifts get their own color.
function styleKey(entry, role) {
  if (!entry) return 'Off'
  if (role === 'doctor' && entry.surgery) return 'Surgery'
  return SHIFT_CONFIG[entry.block] ? entry.block : 'Off'
}

// ─── Date helpers (all UTC so DST / browser timezone never shifts a calendar date) ──
const DAY_MS = 86400000
const parseISO = (iso) => new Date(`${iso}T00:00:00Z`)
const fmt = (d, opts) => d.toLocaleDateString('en-US', { timeZone: 'UTC', ...opts })

// Build the 14 calendar days starting at startDate and merge in the assigned shifts.
function buildWeeks(startDate, entries) {
  const byDate = {}
  for (const e of entries) (byDate[e.date] ||= []).push(e)

  const start = parseISO(startDate)
  const days = Array.from({ length: 14 }, (_, i) => {
    const d = new Date(start.getTime() + i * DAY_MS)
    const iso = d.toISOString().slice(0, 10)
    return {
      iso,
      dayName: fmt(d, { weekday: 'long' }),
      date: fmt(d, { month: 'short', day: 'numeric' }),
      isToday: i === 0,
      entries: byDate[iso] || [],
      _d: d,
    }
  })

  return [days.slice(0, 7), days.slice(7, 14)].map((weekDays, i) => ({
    weekLabel: `Week ${i + 1}`,
    dateRange: `${fmt(weekDays[0]._d, { month: 'short', day: 'numeric' })} – ${fmt(weekDays[6]._d, { month: 'short', day: 'numeric', year: 'numeric' })}`,
    days: weekDays,
  }))
}

function ShiftBadge({ styleKey: key }) {
  const cfg = SHIFT_CONFIG[key] || SHIFT_CONFIG['Off']
  return (
    <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full border text-xs font-medium ${cfg.badge}`}>
      {key === 'Surgery' && <Scissors size={11} />}
      {cfg.label}
    </span>
  )
}

function ColleagueChip({ name }) {
  // Skip the "Dr." title so "Dr. Jane Smith" shows JS, not DJ
  const initials = name.split(' ').filter(w => /^[A-Z]/.test(w) && !/^Dr\.?$/.test(w)).slice(0, 2).map(w => w[0]).join('')
  return (
    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-gray-100 text-gray-700 rounded-full text-xs font-medium">
      <span className="w-4 h-4 rounded-full bg-teal-500 flex items-center justify-center text-white text-xs" style={{ fontSize: 9 }}>
        {initials}
      </span>
      {name}
    </span>
  )
}

function Legend({ role }) {
  return (
    <div className="flex flex-wrap gap-3">
      {Object.entries(SHIFT_CONFIG)
        .filter(([k]) => k !== 'Off' && (k !== 'Surgery' || role === 'doctor'))
        .map(([key, cfg]) => (
          <div key={key} className="flex items-center gap-1.5 text-xs text-gray-600">
            <span className={`w-2.5 h-2.5 rounded-full ${cfg.dot}`} />
            {cfg.label}
          </div>
        ))}
    </div>
  )
}

export default function SchedulePage() {
  const { user } = useAuth()
  const [weekIdx, setWeekIdx] = useState(0)
  const [data, setData] = useState(null)       // { startDate, endDate, entries }
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!user) return
    let cancelled = false
    setLoading(true)
    setError(null)

    fetch(`/api/schedule?eid=${encodeURIComponent(user.id)}`)
      .then(res => {
        if (!res.ok) throw new Error('Unable to load your schedule. Please try again.')
        return res.json()
      })
      .then(json => { if (!cancelled) setData(json) })
      .catch(err => { if (!cancelled) setError(err.message) })
      .finally(() => { if (!cancelled) setLoading(false) })

    return () => { cancelled = true }
  }, [user?.id])

  const schedule = useMemo(
    () => (data ? buildWeeks(data.startDate, data.entries) : null),
    [data]
  )

  const role = user?.role
  const colleagueLabel = role === 'doctor' ? 'Technicians' : 'Doctors'
  const week = schedule?.[weekIdx]
  const workingDays = week ? week.days.reduce((n, d) => n + d.entries.length, 0) : 0

  return (
    <div className="p-8">
      {/* Header */}
      <div className="flex items-start justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">My Schedule</h1>
          <p className="text-gray-500 text-sm mt-1">Next 2 weeks — {user?.name}</p>
        </div>
        <div className="flex items-center gap-1 bg-white border border-gray-200 rounded-xl p-1 shadow-sm">
          {[0, 1].map(i => (
            <button
              key={i}
              onClick={() => setWeekIdx(i)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${weekIdx === i ? 'bg-teal-700 text-white shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
            >
              Week {i + 1}
            </button>
          ))}
        </div>
      </div>

      {loading && (
        <div className="flex items-center gap-2 text-gray-500 text-sm">
          <Loader2 size={16} className="animate-spin" /> Loading your schedule…
        </div>
      )}

      {error && !loading && (
        <div className="flex items-center gap-2 bg-red-50 border border-red-200 text-red-700 rounded-xl px-4 py-3 text-sm">
          <AlertCircle size={16} /> {error}
        </div>
      )}

      {week && !loading && !error && (
        <>
          {/* Week summary bar */}
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 mb-6 flex flex-wrap items-center gap-6">
            <div className="flex items-center gap-2 text-gray-700">
              <CalendarDays size={18} className="text-teal-600" />
              <span className="font-semibold text-sm">{week.dateRange}</span>
            </div>
            <div className="flex items-center gap-2 text-gray-700">
              <Clock size={16} className="text-teal-600" />
              <span className="text-sm">{workingDays} shift{workingDays !== 1 ? 's' : ''} this week</span>
            </div>
            <div className="ml-auto">
              <Legend role={role} />
            </div>
          </div>

          {/* Schedule table: one row per shift, or a single "Day Off" row for a free day */}
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  <th className="text-left px-6 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider w-32">Day</th>
                  <th className="text-left px-6 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider w-24">Date</th>
                  <th className="text-left px-6 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider">Shift</th>
                  <th className="text-left px-6 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider">Hours</th>
                  <th className="text-left px-6 py-3.5 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                    <div className="flex items-center gap-1.5">
                      <Users size={13} />
                      {colleagueLabel}
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {week.days.flatMap(day => {
                  const rows = day.entries.length > 0 ? day.entries : [null]
                  return rows.map(entry => {
                    const key = styleKey(entry, role)
                    const cfg = SHIFT_CONFIG[key]
                    const isOff = !entry
                    const coworkers = entry?.coworkers ?? []
                    return (
                      <tr
                        key={`${day.iso}-${entry?.block ?? 'off'}`}
                        className={`transition-colors ${isOff ? 'bg-white' : cfg.bg} hover:brightness-95`}
                      >
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2.5">
                            <span className={`w-2 h-2 rounded-full flex-shrink-0 ${cfg.dot}`} />
                            <span className={`font-semibold ${isOff ? 'text-gray-400' : 'text-gray-900'}`}>
                              {day.dayName}
                            </span>
                            {day.isToday && (
                              <span className="text-[10px] font-semibold uppercase tracking-wide text-teal-700 bg-teal-100 px-1.5 py-0.5 rounded">
                                Today
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4 text-gray-500 text-xs">{day.date}</td>
                        <td className="px-6 py-4">
                          <ShiftBadge styleKey={key} />
                        </td>
                        <td className="px-6 py-4 text-gray-500 text-xs">
                          {cfg.time ?? <span className="text-gray-300">—</span>}
                        </td>
                        <td className="px-6 py-4">
                          {coworkers.length > 0 ? (
                            <div className="flex flex-wrap gap-1.5">
                              {coworkers.map(c => <ColleagueChip key={c} name={c} />)}
                            </div>
                          ) : (
                            <span className="text-gray-300 text-xs">—</span>
                          )}
                        </td>
                      </tr>
                    )
                  })
                })}
              </tbody>
            </table>
          </div>

          <p className="text-xs text-gray-400 mt-4">
            {role === 'doctor' && 'Surgery shifts are highlighted in amber. '}
            Contact your scheduler to report any discrepancies.
          </p>
        </>
      )}
    </div>
  )
}
