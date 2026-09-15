import { useState } from 'react'
import { ChevronLeft, ChevronRight, Clock, Users, CalendarDays, Scissors } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'

// ─── Shift styling config ─────────────────────────────────────────────────────
const SHIFT_CONFIG = {
  'Early Morning': { bg: 'bg-teal-50',   badge: 'bg-teal-100 text-teal-800 border-teal-200',   dot: 'bg-teal-500',   label: 'Early Morning', time: '7:30 am – 6:30 pm' },
  '8.30 Shift':    { bg: 'bg-blue-50',   badge: 'bg-blue-100 text-blue-800 border-blue-200',     dot: 'bg-blue-500',   label: '8:30 Shift',    time: '8:30 am – 7:30 pm' },
  'Late Doctor':   { bg: 'bg-indigo-50', badge: 'bg-indigo-100 text-indigo-800 border-indigo-200', dot: 'bg-indigo-500', label: 'Late Doctor',   time: '9:30 am – 8:30 pm' },
  'Surgery':       { bg: 'bg-amber-50',  badge: 'bg-amber-100 text-amber-800 border-amber-300',  dot: 'bg-amber-500',  label: 'Surgery',       time: '7:30 am – 6:30 pm' },
  'Overnight':     { bg: 'bg-slate-100', badge: 'bg-slate-200 text-slate-700 border-slate-300',  dot: 'bg-slate-500',  label: 'Overnight',     time: '8:00 pm – 8:00 am' },
  'Sunday':        { bg: 'bg-purple-50', badge: 'bg-purple-100 text-purple-800 border-purple-200', dot: 'bg-purple-500', label: 'Sunday Shift', time: '8:00 am – 8:00 pm' },
  'Off':           { bg: 'bg-white',     badge: 'bg-gray-100 text-gray-400 border-gray-200',     dot: 'bg-gray-300',   label: 'Day Off',       time: null },
}

// ─── Mock schedule data (replace with GET /api/schedule?weeks=2) ──────────────
const DOCTOR_SCHEDULE = [
  {
    weekLabel: 'Week 1',
    dateRange: 'Sep 15 – Sep 21, 2026',
    days: [
      { dayName: 'Monday',    date: 'Sep 15', shift: 'Early Morning', colleagues: ['Alex Johnson', 'Sam Williams'] },
      { dayName: 'Tuesday',   date: 'Sep 16', shift: 'Off',           colleagues: [] },
      { dayName: 'Wednesday', date: 'Sep 17', shift: 'Surgery',       colleagues: ['Casey Brown'] },
      { dayName: 'Thursday',  date: 'Sep 18', shift: 'Off',           colleagues: [] },
      { dayName: 'Friday',    date: 'Sep 19', shift: 'Late Doctor',   colleagues: ['Morgan Lee', 'Jordan Garcia'] },
      { dayName: 'Saturday',  date: 'Sep 20', shift: 'Off',           colleagues: [] },
      { dayName: 'Sunday',    date: 'Sep 21', shift: 'Off',           colleagues: [] },
    ],
  },
  {
    weekLabel: 'Week 2',
    dateRange: 'Sep 22 – Sep 28, 2026',
    days: [
      { dayName: 'Monday',    date: 'Sep 22', shift: 'Early Morning', colleagues: ['Alex Johnson'] },
      { dayName: 'Tuesday',   date: 'Sep 23', shift: 'Off',           colleagues: [] },
      { dayName: 'Wednesday', date: 'Sep 24', shift: '8.30 Shift',    colleagues: ['Sam Williams', 'Casey Brown'] },
      { dayName: 'Thursday',  date: 'Sep 25', shift: 'Off',           colleagues: [] },
      { dayName: 'Friday',    date: 'Sep 26', shift: 'Off',           colleagues: [] },
      { dayName: 'Saturday',  date: 'Sep 27', shift: 'Early Morning', colleagues: ['Morgan Lee'] },
      { dayName: 'Sunday',    date: 'Sep 28', shift: 'Off',           colleagues: [] },
    ],
  },
]

const TECH_SCHEDULE = [
  {
    weekLabel: 'Week 1',
    dateRange: 'Sep 15 – Sep 21, 2026',
    days: [
      { dayName: 'Monday',    date: 'Sep 15', shift: 'Early Morning', colleagues: ['Dr. Jane Smith'] },
      { dayName: 'Tuesday',   date: 'Sep 16', shift: '8.30 Shift',    colleagues: ['Dr. Michael Jones'] },
      { dayName: 'Wednesday', date: 'Sep 17', shift: 'Off',           colleagues: [] },
      { dayName: 'Thursday',  date: 'Sep 18', shift: 'Early Morning', colleagues: ['Dr. Emily Chen'] },
      { dayName: 'Friday',    date: 'Sep 19', shift: 'Off',           colleagues: [] },
      { dayName: 'Saturday',  date: 'Sep 20', shift: 'Off',           colleagues: [] },
      { dayName: 'Sunday',    date: 'Sep 21', shift: 'Sunday',        colleagues: ['Dr. Robert Davis'] },
    ],
  },
  {
    weekLabel: 'Week 2',
    dateRange: 'Sep 22 – Sep 28, 2026',
    days: [
      { dayName: 'Monday',    date: 'Sep 22', shift: 'Early Morning', colleagues: ['Dr. Jane Smith'] },
      { dayName: 'Tuesday',   date: 'Sep 23', shift: 'Off',           colleagues: [] },
      { dayName: 'Wednesday', date: 'Sep 24', shift: 'Off',           colleagues: [] },
      { dayName: 'Thursday',  date: 'Sep 25', shift: 'Late Doctor',   colleagues: ['Dr. Lisa Martinez'] },
      { dayName: 'Friday',    date: 'Sep 26', shift: 'Overnight',     colleagues: ['Dr. Michael Jones'] },
      { dayName: 'Saturday',  date: 'Sep 27', shift: 'Off',           colleagues: [] },
      { dayName: 'Sunday',    date: 'Sep 28', shift: 'Off',           colleagues: [] },
    ],
  },
]
// ─────────────────────────────────────────────────────────────────────────────

function ShiftBadge({ type }) {
  const cfg = SHIFT_CONFIG[type] || SHIFT_CONFIG['Off']
  return (
    <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full border text-xs font-medium ${cfg.badge}`}>
      {type === 'Surgery' && <Scissors size={11} />}
      {cfg.label}
    </span>
  )
}

function ColleagueChip({ name }) {
  const initials = name.split(' ').filter(w => /^[A-Z]/.test(w)).slice(0, 2).map(w => w[0]).join('')
  return (
    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-gray-100 text-gray-700 rounded-full text-xs font-medium">
      <span className="w-4 h-4 rounded-full bg-teal-500 flex items-center justify-center text-white text-xs" style={{ fontSize: 9 }}>
        {initials}
      </span>
      {name}
    </span>
  )
}

function Legend() {
  return (
    <div className="flex flex-wrap gap-3">
      {Object.entries(SHIFT_CONFIG).filter(([k]) => k !== 'Off').map(([key, cfg]) => (
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

  const schedule = user?.role === 'doctor' ? DOCTOR_SCHEDULE : TECH_SCHEDULE
  const week = schedule[weekIdx]
  const colleagueLabel = user?.role === 'doctor' ? 'Technicians' : 'Doctor'

  const workingDays = week.days.filter(d => d.shift !== 'Off').length

  return (
    <div className="p-8">
      {/* Header */}
      <div className="flex items-start justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">My Schedule</h1>
          <p className="text-gray-500 text-sm mt-1">Next 2 weeks — {user?.name}</p>
        </div>
        <div className="flex items-center gap-1 bg-white border border-gray-200 rounded-xl p-1 shadow-sm">
          <button
            onClick={() => setWeekIdx(0)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${weekIdx === 0 ? 'bg-teal-700 text-white shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
          >
            Week 1
          </button>
          <button
            onClick={() => setWeekIdx(1)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${weekIdx === 1 ? 'bg-teal-700 text-white shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
          >
            Week 2
          </button>
        </div>
      </div>

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
          <Legend />
        </div>
      </div>

      {/* Schedule table */}
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
            {week.days.map((day) => {
              const cfg = SHIFT_CONFIG[day.shift] || SHIFT_CONFIG['Off']
              const isOff = day.shift === 'Off'
              return (
                <tr
                  key={day.date}
                  className={`transition-colors ${isOff ? 'bg-white' : cfg.bg} hover:brightness-95`}
                >
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2.5">
                      <span className={`w-2 h-2 rounded-full flex-shrink-0 ${cfg.dot}`} />
                      <span className={`font-semibold ${isOff ? 'text-gray-400' : 'text-gray-900'}`}>
                        {day.dayName}
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-gray-500 text-xs">{day.date}</td>
                  <td className="px-6 py-4">
                    <ShiftBadge type={day.shift} />
                  </td>
                  <td className="px-6 py-4 text-gray-500 text-xs">
                    {cfg.time ?? <span className="text-gray-300">—</span>}
                  </td>
                  <td className="px-6 py-4">
                    {day.colleagues.length > 0 ? (
                      <div className="flex flex-wrap gap-1.5">
                        {day.colleagues.map(c => <ColleagueChip key={c} name={c} />)}
                      </div>
                    ) : (
                      <span className="text-gray-300 text-xs">—</span>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      <p className="text-xs text-gray-400 mt-4">
        Surgery shifts are highlighted in amber. Contact your scheduler to report any discrepancies.
      </p>
    </div>
  )
}
