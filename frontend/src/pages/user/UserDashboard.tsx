
import {
  Briefcase,
  CalendarDays,
  CheckCircle,
  Clock,
  Plus,
  LogOut,
  TrendingUp,
  FileText,
} from 'lucide-react'

import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export default function UserDashboard() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    try {
      await logout()
      navigate('/login', { replace: true })
    } catch (error) {
      console.error('Logout failed:', error)
    }
  }

  const stats = [
    {
      title: 'Total Applications',
      value: '0',
      icon: Briefcase,
      color: 'text-blue-400',
    },
    {
      title: 'Interviews',
      value: '0',
      icon: CalendarDays,
      color: 'text-purple-400',
    },
    {
      title: 'Offers Received',
      value: '0',
      icon: CheckCircle,
      color: 'text-green-400',
    },
    {
      title: 'Pending Applications',
      value: '0',
      icon: Clock,
      color: 'text-orange-400',
    },
  ]

  return (
    <div className="min-h-screen bg-slate-950 text-white flex">

      {/* Sidebar */}
      <aside className="hidden md:block w-64 bg-slate-900 border-r border-slate-800 p-6">
        <h1 className="text-2xl font-bold mb-10">
          Job Tracker
        </h1>

        <nav className="space-y-6">
          <div className="flex items-center gap-3 text-blue-400">
            <TrendingUp size={20} />
            Dashboard
          </div>

          <div className="flex items-center gap-3 text-slate-400">
            <Briefcase size={20} />
            My Applications
          </div>

          <div className="flex items-center gap-3 text-slate-400">
            <CalendarDays size={20} />
            Interviews
          </div>

          <div className="flex items-center gap-3 text-slate-400">
            <FileText size={20} />
            My Resume
          </div>
        </nav>
      </aside>

      {/* Main Content */}
      <main className="flex-1 p-6 md:p-10">

        {/* Header */}
        <header className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-10">
          <div>
            <h2 className="text-3xl font-bold">
              Dashboard
            </h2>

            <p className="text-slate-400 mt-2">
              Welcome back, {user?.fullName || 'User'}!
            </p>
          </div>

          <button
            onClick={handleLogout}
            className="flex items-center justify-center gap-2 bg-red-600 hover:bg-red-700 px-4 py-2 rounded-lg"
          >
            <LogOut size={18} />
            Logout
          </button>
        </header>

        {/* Statistics */}
        <section className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-6 mb-10">
          {stats.map((stat) => {
            const Icon = stat.icon

            return (
              <div
                key={stat.title}
                className="bg-slate-900 border border-slate-800 p-6 rounded-xl"
              >
                <Icon className={`${stat.color} mb-4`} size={28} />

                <p className="text-slate-400 text-sm">
                  {stat.title}
                </p>

                <h3 className="text-3xl font-bold mt-2">
                  {stat.value}
                </h3>
              </div>
            )
          })}
        </section>

        {/* Quick Actions */}
        <section className="bg-slate-900 border border-slate-800 rounded-xl p-6 mb-8">
          <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4">
            <div>
              <h3 className="text-xl font-semibold">
                Start your job search
              </h3>

              <p className="text-slate-400 mt-2 text-sm">
                Track your job applications and manage your career.
              </p>
            </div>

            <button
              onClick={() => navigate('/applications/new')}
              className="flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 px-5 py-3 rounded-lg"
            >
              <Plus size={18} />
              Add Application
            </button>
          </div>
        </section>

        {/* Recent Applications */}
        <section className="bg-slate-900 border border-slate-800 rounded-xl p-6">
          <h3 className="text-xl font-semibold mb-6">
            Recent Applications
          </h3>

          <div className="text-center py-12">
            <Briefcase
              size={42}
              className="mx-auto text-slate-600 mb-4"
            />

            <h4 className="text-lg font-medium">
              No applications yet
            </h4>

            <p className="text-slate-400 text-sm mt-2">
              Start tracking your job applications here.
            </p>
          </div>
        </section>

      </main>
    </div>
  )
}
