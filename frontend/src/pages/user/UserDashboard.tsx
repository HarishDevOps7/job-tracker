
import {
  useCallback,
  useEffect,
  useState,
  type ChangeEvent,
  type FormEvent,
} from 'react'

import {
  Briefcase,
  CalendarDays,
  CheckCircle,
  Clock,
  Plus,
  LogOut,
  TrendingUp,
  FileText,
  Trash2,
  Pencil,
  X,
  Search,
  Filter,
} from 'lucide-react'

import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import api from '../../services/api'

interface DashboardStats {
  totalApplications: number
  applied: number
  assessment: number
  interview: number
  offer: number
  rejected: number
  withdrawn: number
  noResponse: number
}

interface JobApplication {
  id: number
  companyName: string
  jobTitle: string
  source: string
  jobUrl: string
  appliedDate: string
  status: string
}

interface EditForm {
  companyName: string
  jobTitle: string
  source: string
  jobUrl: string
  appliedDate: string
}

const getToday = () => {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

export default function UserDashboard() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [applications, setApplications] = useState<JobApplication[]>([])

  const [loading, setLoading] = useState(true)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [updatingStatusId, setUpdatingStatusId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)

  // SEARCH AND FILTER STATES
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [sourceFilter, setSourceFilter] = useState('ALL')

  // PAGINATION STATE
  const [currentPage, setCurrentPage] = useState(1)
  const pageSize = 10

  const [editForm, setEditForm] = useState<EditForm>({
    companyName: '',
    jobTitle: '',
    source: '',
    jobUrl: '',
    appliedDate: '',
  })

  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // LOAD DASHBOARD DATA
  const fetchDashboardData = useCallback(async () => {
    try {
      setLoading(true)
      setError('')

      const [statsResponse, applicationsResponse] = await Promise.all([
        api.get('/applications/stats'),
        api.get('/applications'),
      ])

      setStats(statsResponse.data)
      setApplications(applicationsResponse.data)
    } catch (err) {
      console.error('Dashboard loading failed:', err)
      setError('Unable to load dashboard data. Please refresh the page.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchDashboardData()
  }, [fetchDashboardData])

  // RESET PAGE WHEN FILTERS CHANGE
  useEffect(() => {
    setCurrentPage(1)
  }, [searchTerm, statusFilter, sourceFilter])

  // OPEN EDIT FORM
  const handleEdit = (application: JobApplication) => {
    setError('')
    setSuccess('')

    setEditForm({
      companyName: application.companyName,
      jobTitle: application.jobTitle,
      source: application.source || '',
      jobUrl: application.jobUrl || '',
      appliedDate: application.appliedDate,
    })

    setEditingId(application.id)
  }

  // HANDLE EDIT FORM INPUTS
  const handleEditChange = (
    e: ChangeEvent<HTMLInputElement>
  ) => {
    const { name, value } = e.target

    setEditForm((previous) => ({
      ...previous,
      [name]: value,
    }))
  }

  // UPDATE APPLICATION DETAILS
  const handleUpdate = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()

    if (editingId === null) return

    setError('')
    setSuccess('')

    if (editForm.appliedDate > getToday()) {
      setError('Applied date cannot be in the future.')
      return
    }

    try {
      setSaving(true)

      await api.put(`/applications/${editingId}`, editForm)

      setEditingId(null)
      setSuccess('Application updated successfully.')

      await fetchDashboardData()
    } catch (err: any) {
      console.error('Update application failed:', err)

      setError(
        err.response?.data?.message ||
        'Unable to update application. Please try again.'
      )
    } finally {
      setSaving(false)
    }
  }

  // UPDATE APPLICATION STATUS
  const handleStatusChange = async (
    id: number,
    status: string
  ) => {
    try {
      setUpdatingStatusId(id)
      setError('')
      setSuccess('')

      await api.patch(`/applications/${id}/status`, {
        status,
      })

      setSuccess('Application status updated successfully.')

      await fetchDashboardData()
    } catch (err: any) {
      console.error('Status update failed:', err)

      setError(
        err.response?.data?.message ||
        'Unable to update application status.'
      )
    } finally {
      setUpdatingStatusId(null)
    }
  }

  // DELETE APPLICATION
  const handleDelete = async (id: number, companyName: string) => {
    const confirmed = window.confirm(
      `Are you sure you want to delete the application for ${companyName}?`
    )

    if (!confirmed) return

    try {
      setDeletingId(id)
      setError('')
      setSuccess('')

      await api.delete(`/applications/${id}`)

      setSuccess(`${companyName} application deleted successfully.`)

      await fetchDashboardData()
    } catch (err) {
      console.error('Delete application failed:', err)
      setError('Unable to delete application. Please try again.')
    } finally {
      setDeletingId(null)
    }
  }

  // LOGOUT
  const handleLogout = async () => {
    try {
      await logout()
      navigate('/login', { replace: true })
    } catch (error) {
      console.error('Logout failed:', error)
    }
  }

  // CLEAR SEARCH AND FILTERS
  const handleClearFilters = () => {
    setSearchTerm('')
    setStatusFilter('ALL')
    setSourceFilter('ALL')
    setCurrentPage(1)
  }

  // FILTER APPLICATIONS
  const filteredApplications = applications
    .filter((application) => {
      const search = searchTerm.toLowerCase().trim()

      const companyName = application.companyName || ''
      const jobTitle = application.jobTitle || ''
      const source = application.source || ''

      const matchesSearch =
        companyName.toLowerCase().includes(search) ||
        jobTitle.toLowerCase().includes(search)

      const matchesStatus =
        statusFilter === 'ALL' ||
        application.status === statusFilter

      const matchesSource =
        sourceFilter === 'ALL' ||
        source.toLowerCase() === sourceFilter.toLowerCase()

      return matchesSearch && matchesStatus && matchesSource
    })
    .sort(
      (a, b) =>
        new Date(b.appliedDate).getTime() -
        new Date(a.appliedDate).getTime()
    )

  // PAGINATION CALCULATIONS
  const totalPages = Math.ceil(
    filteredApplications.length / pageSize
  )

  const startIndex = (currentPage - 1) * pageSize
  const endIndex = startIndex + pageSize

  const paginatedApplications = filteredApplications.slice(
    startIndex,
    endIndex
  )

  // DASHBOARD CARDS
  const dashboardCards = [
    {
      title: 'Total Applications',
      value: stats?.totalApplications ?? 0,
      icon: Briefcase,
      color: 'text-blue-400',
    },
    {
      title: 'Interviews',
      value: stats?.interview ?? 0,
      icon: CalendarDays,
      color: 'text-purple-400',
    },
    {
      title: 'Offers Received',
      value: stats?.offer ?? 0,
      icon: CheckCircle,
      color: 'text-green-400',
    },
    {
      title: 'Pending Applications',
      value: (stats?.applied ?? 0) + (stats?.assessment ?? 0),
      icon: Clock,
      color: 'text-orange-400',
    },
  ]

  return (
    <div className="min-h-screen bg-slate-950 text-white flex">

      {/* SIDEBAR */}
      <aside className="hidden md:block w-64 bg-slate-900 border-r border-slate-800 p-6">
        <h1 className="text-2xl font-bold mb-10">
          Job Tracker
        </h1>

        <nav className="space-y-6">
          <div className="flex items-center gap-3 text-blue-400">
            <TrendingUp size={20} />
            Dashboard
          </div>

          <button
            type="button"
            onClick={() => navigate('/applications')}
            className="flex w-full items-center gap-3 text-left text-slate-400 transition-colors hover:text-blue-400"
          >
            <Briefcase size={20} />
            My Applications
          </button>

          <button
  type="button"
  onClick={() => navigate('/interviews')}
  className="flex w-full items-center gap-3 text-left text-slate-400 hover:text-blue-400 transition-colors"
>
  <CalendarDays size={20} />
  Interviews
</button>

          <div className="flex items-center gap-3 text-slate-400">
            <FileText size={20} />
            My Resume
          </div>
        </nav>
      </aside>

      {/* MAIN CONTENT */}
      <main className="flex-1 p-6 md:p-10">

        {/* HEADER */}
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

        {/* SUCCESS MESSAGE */}
        {success && (
          <div className="bg-green-900/30 border border-green-700 text-green-300 p-4 rounded-lg mb-6">
            {success}
          </div>
        )}

        {/* ERROR MESSAGE */}
        {error && (
          <div className="bg-red-900/30 border border-red-700 text-red-300 p-4 rounded-lg mb-6">
            {error}
          </div>
        )}

        {/* EDIT APPLICATION FORM */}
        {editingId !== null && (
          <section className="bg-slate-900 border border-blue-700 rounded-xl p-6 mb-8">

            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-semibold">
                Edit Job Application
              </h3>

              <button
                type="button"
                onClick={() => setEditingId(null)}
                disabled={saving}
                className="text-slate-400 hover:text-white"
              >
                <X size={22} />
              </button>
            </div>

            <form onSubmit={handleUpdate} className="space-y-5">

              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">

                <div>
                  <label className="block text-sm mb-2">
                    Company Name *
                  </label>

                  <input
                    name="companyName"
                    value={editForm.companyName}
                    onChange={handleEditChange}
                    required
                    maxLength={150}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg p-3"
                  />
                </div>

                <div>
                  <label className="block text-sm mb-2">
                    Job Title *
                  </label>

                  <input
                    name="jobTitle"
                    value={editForm.jobTitle}
                    onChange={handleEditChange}
                    required
                    maxLength={150}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg p-3"
                  />
                </div>

                <div>
                  <label className="block text-sm mb-2">
                    Application Source
                  </label>

                  <input
                    name="source"
                    value={editForm.source}
                    onChange={handleEditChange}
                    placeholder="LinkedIn, Naukri, Referral..."
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg p-3"
                  />
                </div>

                <div>
                  <label className="block text-sm mb-2">
                    Job URL
                  </label>

                  <input
                    type="url"
                    name="jobUrl"
                    value={editForm.jobUrl}
                    onChange={handleEditChange}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg p-3"
                  />
                </div>

                <div>
                  <label className="block text-sm mb-2">
                    Applied Date *
                  </label>

                  <input
                    type="date"
                    name="appliedDate"
                    value={editForm.appliedDate}
                    onChange={handleEditChange}
                    max={getToday()}
                    required
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg p-3"
                  />
                </div>

              </div>

              <div className="flex flex-col sm:flex-row gap-3">

                <button
                  type="submit"
                  disabled={saving}
                  className="bg-blue-600 hover:bg-blue-700 disabled:opacity-50 px-6 py-3 rounded-lg font-semibold"
                >
                  {saving ? 'Updating...' : 'Update Application'}
                </button>

                <button
                  type="button"
                  disabled={saving}
                  onClick={() => setEditingId(null)}
                  className="bg-slate-700 hover:bg-slate-600 px-6 py-3 rounded-lg"
                >
                  Cancel
                </button>

              </div>

            </form>
          </section>
        )}

        {/* STATISTICS */}
        <section className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-6 mb-10">

          {dashboardCards.map((stat) => {
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
                  {loading ? '...' : stat.value}
                </h3>
              </div>
            )
          })}

        </section>

        {/* QUICK ACTIONS */}
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

        {/* MY APPLICATIONS */}
        <section className="bg-slate-900 border border-slate-800 rounded-xl p-6">

          <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3 mb-6">
            <h3 className="text-xl font-semibold">
              My Applications
            </h3>

            <span className="text-sm text-slate-400">
              {filteredApplications.length} of {applications.length} applications
            </span>
          </div>

          {/* SEARCH AND FILTER */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">

            {/* SEARCH */}
            <div className="relative">
              <Search
                size={18}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
              />

              <input
                type="text"
                placeholder="Search company or job title..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg pl-10 pr-3 py-3 text-white"
              />
            </div>

            {/* STATUS FILTER */}
            <div className="relative">
              <Filter
                size={18}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
              />

              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg pl-10 pr-3 py-3 text-white"
              >
                <option value="ALL">All Statuses</option>
                <option value="APPLIED">Applied</option>
                <option value="ASSESSMENT">Assessment</option>
                <option value="INTERVIEW">Interview</option>
                <option value="OFFER">Offer</option>
                <option value="REJECTED">Rejected</option>
                <option value="WITHDRAWN">Withdrawn</option>
                <option value="NO_RESPONSE">No Response</option>
              </select>
            </div>

            {/* SOURCE FILTER */}
            <select
              value={sourceFilter}
              onChange={(e) => setSourceFilter(e.target.value)}
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-3 text-white"
            >
              <option value="ALL">All Sources</option>
              <option value="LinkedIn">LinkedIn</option>
              <option value="Naukri">Naukri</option>
              <option value="Company Portal">Company Portal</option>
              <option value="Indeed">Indeed</option>
              <option value="Referral">Referral</option>
            </select>

          </div>

          {/* CLEAR FILTERS AND RECORD COUNT */}
          <div className="flex justify-between items-center mb-4">

            <p className="text-sm text-slate-400">
              Showing {filteredApplications.length} applications
            </p>

            <button
              onClick={handleClearFilters}
              className="text-blue-400 hover:text-blue-300 text-sm"
            >
              Clear Filters
            </button>

          </div>

          {/* APPLICATION TABLE */}
          {loading ? (
            <p className="text-center text-slate-400 py-8">
              Loading applications...
            </p>
          ) : filteredApplications.length === 0 ? (
            <div className="text-center py-12">

              <Briefcase
                size={42}
                className="mx-auto text-slate-600 mb-4"
              />

              <h4 className="text-lg font-medium">
                No applications found
              </h4>

              <p className="text-slate-400 text-sm mt-2">
                Try changing your search or filters.
              </p>

              <button
                onClick={handleClearFilters}
                className="mt-4 text-blue-400 hover:text-blue-300"
              >
                Clear Filters
              </button>

            </div>
          ) : (
            <div className="overflow-x-auto">

              <table className="w-full text-left">

                <thead>
                  <tr className="text-slate-400 border-b border-slate-800 text-sm">
                    <th className="p-3">Company</th>
                    <th className="p-3">Job Title</th>
                    <th className="p-3">Source</th>
                    <th className="p-3">Applied Date</th>
                    <th className="p-3">Status</th>
                    <th className="p-3">Actions</th>
                  </tr>
                </thead>

                <tbody>

                  {paginatedApplications.map((application) => (

                    <tr
                      key={application.id}
                      className="border-b border-slate-800 hover:bg-slate-800/50"
                    >

                      <td className="p-3 font-medium">
                        {application.companyName}
                      </td>

                      <td className="p-3 text-slate-300">
                        {application.jobTitle}
                      </td>

                      <td className="p-3 text-slate-400">
                        {application.source || '-'}
                      </td>

                      <td className="p-3 text-slate-400">
                        {application.appliedDate}
                      </td>

                      {/* STATUS DROPDOWN */}
                      <td className="p-3">

                        <select
                          value={application.status}
                          disabled={updatingStatusId === application.id}
                          onChange={(e) =>
                            handleStatusChange(
                              application.id,
                              e.target.value
                            )
                          }
                          className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white disabled:opacity-50"
                        >
                          <option value="APPLIED">Applied</option>
                          <option value="ASSESSMENT">Assessment</option>
                          <option value="INTERVIEW">Interview</option>
                          <option value="OFFER">Offer</option>
                          <option value="REJECTED">Rejected</option>
                          <option value="WITHDRAWN">Withdrawn</option>
                          <option value="NO_RESPONSE">No Response</option>
                        </select>

                        {updatingStatusId === application.id && (
                          <p className="text-xs text-blue-400 mt-1">
                            Updating...
                          </p>
                        )}

                      </td>

                      {/* ACTIONS */}
                      <td className="p-3">

                        <div className="flex items-center gap-3">

                          <button
                            onClick={() => handleEdit(application)}
                            title="Edit application"
                            className="flex items-center gap-1 text-blue-400 hover:text-blue-300"
                          >
                            <Pencil size={17} />
                            Edit
                          </button>

                          <button
                            onClick={() =>
                              handleDelete(
                                application.id,
                                application.companyName
                              )
                            }
                            disabled={deletingId === application.id}
                            title="Delete application"
                            className="flex items-center gap-1 text-red-400 hover:text-red-300 disabled:opacity-50"
                          >
                            <Trash2 size={17} />

                            {deletingId === application.id
                              ? 'Deleting...'
                              : 'Delete'}
                          </button>

                        </div>

                      </td>

                    </tr>

                  ))}

                </tbody>
              </table>

            </div>
          )}

          {/* PAGINATION */}
          {!loading && filteredApplications.length > 0 && (
            <div className="flex flex-col sm:flex-row justify-between items-center gap-4 mt-6">

              <p className="text-sm text-slate-400">
                Showing {startIndex + 1} to{' '}
                {Math.min(endIndex, filteredApplications.length)} of{' '}
                {filteredApplications.length} applications
              </p>

              <div className="flex flex-wrap items-center gap-2">

                <button
                  onClick={() =>
                    setCurrentPage((page) => Math.max(1, page - 1))
                  }
                  disabled={currentPage === 1}
                  className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Previous
                </button>

                {Array.from(
                  { length: totalPages },
                  (_, index) => index + 1
                ).map((page) => (

                  <button
                    key={page}
                    onClick={() => setCurrentPage(page)}
                    className={`px-3 py-2 rounded-lg ${
                      currentPage === page
                        ? 'bg-blue-600 text-white'
                        : 'bg-slate-800 hover:bg-slate-700 text-slate-300'
                    }`}
                  >
                    {page}
                  </button>

                ))}

                <button
                  onClick={() =>
                    setCurrentPage((page) =>
                      Math.min(totalPages, page + 1)
                    )
                  }
                  disabled={currentPage === totalPages}
                  className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Next
                </button>

              </div>
            </div>
          )}

        </section>

      </main>
    </div>
  )
}
