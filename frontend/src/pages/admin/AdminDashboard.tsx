
import { LogOut, Users, Briefcase, Activity, Settings } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export default function AdminDashboard() {
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

  return (
    <div className="min-h-screen bg-slate-950 text-white flex">

      <aside className="w-64 bg-slate-900 border-r border-slate-800 p-6">
        <h1 className="text-2xl font-bold mb-10">Job Tracker</h1>

        <nav className="space-y-4">
          <div className="flex gap-3 items-center text-blue-400">
            <Activity size={20} /> Overview
          </div>
          <div className="flex gap-3 items-center text-slate-400">
            <Users size={20} /> User Management
          </div>
          <div className="flex gap-3 items-center text-slate-400">
            <Briefcase size={20} /> Job Monitoring
          </div>
          <div className="flex gap-3 items-center text-slate-400">
            <Settings size={20} /> Settings
          </div>
        </nav>
      </aside>

      <main className="flex-1 p-8">
        <header className="flex justify-between items-center mb-10">
          <div>
            <h2 className="text-3xl font-bold">Admin Dashboard</h2>
            <p className="text-slate-400 mt-2">
              Welcome back, {user?.fullName || 'Admin'}
            </p>
          </div>

          <button
            onClick={handleLogout}
            className="flex items-center gap-2 bg-red-600 hover:bg-red-700 px-4 py-2 rounded-lg"
          >
            <LogOut size={18} />
            Logout
          </button>
        </header>

        <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-slate-900 border border-slate-800 p-6 rounded-xl">
            <Users className="text-blue-400 mb-4" size={28} />
            <p className="text-slate-400">Total Users</p>
            <h3 className="text-3xl font-bold mt-2">--</h3>
          </div>

          <div className="bg-slate-900 border border-slate-800 p-6 rounded-xl">
            <Briefcase className="text-green-400 mb-4" size={28} />
            <p className="text-slate-400">Job Applications</p>
            <h3 className="text-3xl font-bold mt-2">--</h3>
          </div>

          <div className="bg-slate-900 border border-slate-800 p-6 rounded-xl">
            <Activity className="text-purple-400 mb-4" size={28} />
            <p className="text-slate-400">Active Users</p>
            <h3 className="text-3xl font-bold mt-2">--</h3>
          </div>
        </section>

      </main>
    </div>
  )
}
