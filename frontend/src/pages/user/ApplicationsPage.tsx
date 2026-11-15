
import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent, ChangeEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ArrowLeft,
  Briefcase,
  Search,
  Plus,
  Pencil,
  Trash2,
  ExternalLink,
  RefreshCw,
  X,
  CalendarDays,
} from 'lucide-react'
import api from '../../services/api'

type ApplicationStatus =
  | 'APPLIED'
  | 'ASSESSMENT'
  | 'INTERVIEW'
  | 'OFFER'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'NO_RESPONSE'

interface JobApplication {
  id: number
  companyName: string
  jobTitle: string
  source: string | null
  jobUrl: string | null
  appliedDate: string
  status: ApplicationStatus
}

interface ApplicationForm {
  companyName: string
  jobTitle: string
  source: string
  jobUrl: string
  appliedDate: string
}

const statuses: ApplicationStatus[] = [
  'APPLIED',
  'ASSESSMENT',
  'INTERVIEW',
  'OFFER',
  'REJECTED',
  'WITHDRAWN',
  'NO_RESPONSE',
]

const statusStyles: Record<ApplicationStatus, string> = {
  APPLIED: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
  ASSESSMENT: 'bg-purple-500/10 text-purple-400 border-purple-500/30',
  INTERVIEW: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/30',
  OFFER: 'bg-green-500/10 text-green-400 border-green-500/30',
  REJECTED: 'bg-red-500/10 text-red-400 border-red-500/30',
  WITHDRAWN: 'bg-orange-500/10 text-orange-400 border-orange-500/30',
  NO_RESPONSE: 'bg-slate-500/10 text-slate-400 border-slate-500/30',
}

const emptyForm: ApplicationForm = {
  companyName: '',
  jobTitle: '',
  source: '',
  jobUrl: '',
  appliedDate: '',
}

function getTodayDate(): string {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function getErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as {
      response?: {
        data?: {
          message?: string
          detail?: string
        }
      }
    }).response

    return (
      response?.data?.message ||
      response?.data?.detail ||
      'Something went wrong. Please try again.'
    )
  }

  return 'Something went wrong. Please try again.'
}

function formatStatus(status: ApplicationStatus): string {
  return status.replaceAll('_', ' ')
}

function formatDate(date: string): string {
  if (!date) return '-'

  return new Date(`${date}T00:00:00`).toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

export default function ApplicationsPage() {
  const navigate = useNavigate()

  const [applications, setApplications] = useState<JobApplication[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')

  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [formData, setFormData] = useState<ApplicationForm>(emptyForm)

  const loadApplications = useCallback(async () => {
    try {
      setLoading(true)
      setError('')

      const response = await api.get('/applications')
      setApplications(response.data)
    } catch (err: unknown) {
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadApplications()
  }, [loadApplications])

  const filteredApplications = useMemo(() => {
    const searchValue = search.trim().toLowerCase()

    return applications.filter(application => {
      const matchesSearch =
        application.companyName.toLowerCase().includes(searchValue) ||
        application.jobTitle.toLowerCase().includes(searchValue) ||
        (application.source || '').toLowerCase().includes(searchValue)

      const matchesStatus =
        statusFilter === 'ALL' ||
        application.status === statusFilter

      return matchesSearch && matchesStatus
    })
  }, [applications, search, statusFilter])

  const openAddModal = () => {
    setEditingId(null)
    setFormData({
      ...emptyForm,
      appliedDate: getTodayDate(),
    })
    setError('')
    setSuccess('')
    setModalOpen(true)
  }

  const openEditModal = (application: JobApplication) => {
    setEditingId(application.id)

    setFormData({
      companyName: application.companyName,
      jobTitle: application.jobTitle,
      source: application.source || '',
      jobUrl: application.jobUrl || '',
      appliedDate: application.appliedDate || getTodayDate(),
    })

    setError('')
    setSuccess('')
    setModalOpen(true)
  }

  const handleChange = (
    event: ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { name, value } = event.target

    setFormData(previous => ({
      ...previous,
      [name]: value,
    }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    setError('')
    setSuccess('')

    const companyName = formData.companyName.trim()
    const jobTitle = formData.jobTitle.trim()
    const jobUrl = formData.jobUrl.trim()

    if (!companyName || !jobTitle) {
      setError('Company name and job title are required.')
      return
    }

    if (formData.appliedDate > getTodayDate()) {
      setError('Applied date cannot be in the future.')
      return
    }

    const payload = {
      companyName,
      jobTitle,
      source: formData.source,
      jobUrl,
      appliedDate: formData.appliedDate,
    }

    try {
      setSaving(true)

      if (editingId !== null) {
        await api.put(`/applications/${editingId}`, payload)
        setSuccess('Application updated successfully.')
      } else {
        await api.post('/applications', payload)
        setSuccess('Application added successfully.')
      }

      setModalOpen(false)
      await loadApplications()
    } catch (err: unknown) {
      setError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    const confirmed = window.confirm(
      'Are you sure you want to delete this application?'
    )

    if (!confirmed) return

    try {
      setError('')
      setSuccess('')

      await api.delete(`/applications/${id}`)

      setSuccess('Application deleted successfully.')
      await loadApplications()
    } catch (err: unknown) {
      setError(getErrorMessage(err))
    }
  }

  const handleStatusChange = async (
    id: number,
    status: ApplicationStatus
  ) => {
    try {
      setError('')
      setSuccess('')

      await api.patch(`/applications/${id}/status`, { status })

      setSuccess('Application status updated.')
      await loadApplications()
    } catch (err: unknown) {
      setError(getErrorMessage(err))
    }
  }

  const totalCount = applications.length

  const appliedCount = applications.filter(
    item => item.status === 'APPLIED'
  ).length

  const interviewCount = applications.filter(
    item => item.status === 'INTERVIEW'
  ).length

  const offerCount = applications.filter(
    item => item.status === 'OFFER'
  ).length

  return (
    <div className="min-h-screen bg-slate-950 p-4 text-white sm:p-6 md:p-8">

      <div className="mx-auto max-w-7xl">

        {/* Header */}
        <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">

          <div>
            <button
              type="button"
              onClick={() => navigate('/dashboard')}
              className="mb-4 flex items-center gap-2 text-sm text-slate-400 hover:text-white"
            >
              <ArrowLeft size={17} />
              Back to Dashboard
            </button>

            <h1 className="flex items-center gap-3 text-2xl font-bold sm:text-3xl">
              <Briefcase className="text-blue-400" />
              Job Applications
            </h1>

            <p className="mt-2 text-sm text-slate-400">
              Manage and track all your job applications.
            </p>
          </div>

          <button
            type="button"
            onClick={openAddModal}
            className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-5 py-3 font-semibold hover:bg-blue-700"
          >
            <Plus size={18} />
            Add Application
          </button>

        </div>

        {/* Notifications */}
        {error && (
          <div role="alert" className="mb-4 rounded-lg border border-red-700 bg-red-900/30 p-3 text-sm text-red-300">
            {error}
          </div>
        )}

        {success && (
          <div role="status" className="mb-4 rounded-lg border border-green-700 bg-green-900/30 p-3 text-sm text-green-300">
            {success}
          </div>
        )}

        {/* Statistics */}
        <div className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">

          {[
            { label: 'Total Applications', value: totalCount, color: 'text-white' },
            { label: 'Applied', value: appliedCount, color: 'text-blue-400' },
            { label: 'Interviews', value: interviewCount, color: 'text-cyan-400' },
            { label: 'Offers', value: offerCount, color: 'text-green-400' },
          ].map(item => (
            <div key={item.label} className="rounded-xl border border-slate-800 bg-slate-900 p-4 sm:p-5">
              <p className="text-xs text-slate-400 sm:text-sm">
                {item.label}
              </p>
              <p className={`mt-2 text-2xl font-bold sm:text-3xl ${item.color}`}>
                {item.value}
              </p>
            </div>
          ))}

        </div>

        {/* Search and Filter */}
        <div className="mb-5 flex flex-col gap-3 rounded-xl border border-slate-800 bg-slate-900 p-4 sm:flex-row">

          <div className="relative flex-1">
            <Search className="absolute left-3 top-3 text-slate-500" size={18} />

            <input
              type="search"
              value={search}
              onChange={event => setSearch(event.target.value)}
              placeholder="Search company, job title or source..."
              className="w-full rounded-lg border border-slate-700 bg-slate-800 py-2.5 pl-10 pr-3 text-sm outline-none focus:border-blue-500"
            />
          </div>

          <select
            value={statusFilter}
            onChange={event => setStatusFilter(event.target.value)}
            className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-2.5 text-sm outline-none focus:border-blue-500"
          >
            <option value="ALL">All Statuses</option>
            {statuses.map(status => (
              <option key={status} value={status}>
                {formatStatus(status)}
              </option>
            ))}
          </select>

          <button
            type="button"
            onClick={() => void loadApplications()}
            disabled={loading}
            title="Refresh applications"
            className="flex items-center justify-center gap-2 rounded-lg border border-slate-700 px-4 py-2.5 text-sm hover:bg-slate-800 disabled:opacity-50"
          >
            <RefreshCw size={16} />
            Refresh
          </button>

        </div>

        {/* Applications Table */}
        <div className="overflow-hidden rounded-xl border border-slate-800 bg-slate-900">

          <div className="flex items-center justify-between border-b border-slate-800 p-4">
            <h2 className="font-semibold">All Applications</h2>
            <span className="text-xs text-slate-400">
              {filteredApplications.length} records
            </span>
          </div>

          {loading ? (
            <div className="flex min-h-52 items-center justify-center gap-3 text-slate-400">
              <RefreshCw className="animate-spin" size={20} />
              Loading applications...
            </div>
          ) : filteredApplications.length === 0 ? (
            <div className="flex min-h-64 flex-col items-center justify-center p-6 text-center">
              <Briefcase size={40} className="mb-4 text-slate-600" />

              <h3 className="font-semibold text-slate-300">
                No applications found
              </h3>

              <p className="mt-2 text-sm text-slate-500">
                Try changing your search or add a new application.
              </p>

              <button
                type="button"
                onClick={openAddModal}
                className="mt-5 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium hover:bg-blue-700"
              >
                Add your first application
              </button>
            </div>
          ) : (
            <>
              {/* Desktop Table */}
              <div className="hidden overflow-x-auto md:block">
                <table className="w-full min-w-[850px] text-left text-sm">

                  <thead className="bg-slate-800/70 text-xs uppercase text-slate-400">
                    <tr>
                      <th className="px-5 py-4">Company</th>
                      <th className="px-5 py-4">Job Title</th>
                      <th className="px-5 py-4">Source</th>
                      <th className="px-5 py-4">Applied Date</th>
                      <th className="px-5 py-4">Status</th>
                      <th className="px-5 py-4">Actions</th>
                    </tr>
                  </thead>

                  <tbody className="divide-y divide-slate-800">

                    {filteredApplications.map(application => (
                      <tr key={application.id} className="transition hover:bg-slate-800/40">

                        <td className="px-5 py-4 font-semibold text-white">
                          {application.companyName}
                        </td>

                        <td className="px-5 py-4 text-slate-300">
                          {application.jobTitle}
                        </td>

                        <td className="px-5 py-4 text-slate-400">
                          {application.source || '-'}
                        </td>

                        <td className="px-5 py-4 whitespace-nowrap text-slate-400">
                          {formatDate(application.appliedDate)}
                        </td>

                        <td className="px-5 py-4">
                          <select
                            value={application.status}
                            onChange={event =>
                              void handleStatusChange(
                                application.id,
                                event.target.value as ApplicationStatus
                              )
                            }
                            className={`rounded-full border px-3 py-1.5 text-xs font-semibold outline-none ${statusStyles[application.status]}`}
                          >
                            {statuses.map(status => (
                              <option key={status} value={status} className="bg-slate-900 text-white">
                                {formatStatus(status)}
                              </option>
                            ))}
                          </select>
                        </td>

                        <td className="px-5 py-4">
                          <div className="flex items-center gap-2">

                            <button
                              type="button"
                              onClick={() => openEditModal(application)}
                              title="Edit application"
                              className="rounded-lg p-2 text-blue-400 hover:bg-blue-500/10"
                            >
                              <Pencil size={17} />
                            </button>

                            {application.jobUrl && (
                              <a
                                href={application.jobUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                title="Open job URL"
                                className="rounded-lg p-2 text-cyan-400 hover:bg-cyan-500/10"
                              >
                                <ExternalLink size={17} />
                              </a>
                            )}

                            <button
                              type="button"
                              onClick={() => void handleDelete(application.id)}
                              title="Delete application"
                              className="rounded-lg p-2 text-red-400 hover:bg-red-500/10"
                            >
                              <Trash2 size={17} />
                            </button>

                          </div>
                        </td>

                      </tr>
                    ))}

                  </tbody>
                </table>
              </div>

              {/* Mobile Cards */}
              <div className="space-y-3 p-3 md:hidden">
                {filteredApplications.map(application => (
                  <div
                    key={application.id}
                    className="rounded-xl border border-slate-700 bg-slate-800/50 p-4"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <h3 className="break-words font-semibold text-white">
                          {application.companyName}
                        </h3>
                        <p className="mt-1 break-words text-sm text-slate-400">
                          {application.jobTitle}
                        </p>
                      </div>

                      {application.jobUrl && (
                        <a
                          href={application.jobUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          aria-label="Open job URL"
                          className="shrink-0 text-cyan-400"
                        >
                          <ExternalLink size={18} />
                        </a>
                      )}
                    </div>

                    <div className="mt-4 flex flex-wrap items-center gap-2 text-xs text-slate-400">
                      <span>{application.source || 'Source not specified'}</span>
                      <span>·</span>
                      <span>{formatDate(application.appliedDate)}</span>
                    </div>

                    <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
                      <select
                        value={application.status}
                        onChange={event =>
                          void handleStatusChange(
                            application.id,
                            event.target.value as ApplicationStatus
                          )
                        }
                        className={`max-w-full rounded-full border px-3 py-2 text-xs font-semibold outline-none ${statusStyles[application.status]}`}
                      >
                        {statuses.map(status => (
                          <option key={status} value={status} className="bg-slate-900 text-white">
                            {formatStatus(status)}
                          </option>
                        ))}
                      </select>

                      <div className="flex gap-2">
                        <button
                          type="button"
                          onClick={() => openEditModal(application)}
                          aria-label="Edit application"
                          className="rounded-lg p-2 text-blue-400 hover:bg-blue-500/10"
                        >
                          <Pencil size={17} />
                        </button>

                        <button
                          type="button"
                          onClick={() => void handleDelete(application.id)}
                          aria-label="Delete application"
                          className="rounded-lg p-2 text-red-400 hover:bg-red-500/10"
                        >
                          <Trash2 size={17} />
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}

        </div>

      </div>

      {/* Add / Edit Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/70 p-3 backdrop-blur-sm">

          <div className="my-4 w-full max-w-xl rounded-2xl border border-slate-700 bg-slate-900 p-5 shadow-2xl sm:p-7">

            <div className="mb-6 flex items-center justify-between">
              <div>
                <h2 className="text-xl font-bold">
                  {editingId !== null ? 'Edit Application' : 'Add Application'}
                </h2>
                <p className="mt-1 text-sm text-slate-400">
                  Enter your job application details.
                </p>
              </div>

              <button
                type="button"
                onClick={() => setModalOpen(false)}
                aria-label="Close modal"
                className="rounded-lg p-2 text-slate-400 hover:bg-slate-800 hover:text-white"
              >
                <X size={21} />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">

              {error && (
                <div role="alert" className="rounded-lg border border-red-700 bg-red-900/30 p-3 text-sm text-red-300">
                  {error}
                </div>
              )}

              <div>
                <label htmlFor="companyName" className="mb-2 block text-sm">
                  Company Name *
                </label>
                <input
                  id="companyName"
                  name="companyName"
                  value={formData.companyName}
                  onChange={handleChange}
                  required
                  maxLength={150}
                  placeholder="Example: Amazon"
                  className="w-full rounded-lg border border-slate-700 bg-slate-800 p-3 outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label htmlFor="jobTitle" className="mb-2 block text-sm">
                  Job Title *
                </label>
                <input
                  id="jobTitle"
                  name="jobTitle"
                  value={formData.jobTitle}
                  onChange={handleChange}
                  required
                  maxLength={150}
                  placeholder="Example: Java Developer"
                  className="w-full rounded-lg border border-slate-700 bg-slate-800 p-3 outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label htmlFor="source" className="mb-2 block text-sm">
                  Application Source
                </label>
                <select
                  id="source"
                  name="source"
                  value={formData.source}
                  onChange={handleChange}
                  className="w-full rounded-lg border border-slate-700 bg-slate-800 p-3 outline-none focus:border-blue-500"
                >
                  <option value="">Select Source</option>
                  <option value="LinkedIn">LinkedIn</option>
                  <option value="Naukri">Naukri</option>
                  <option value="Company Portal">Company Portal</option>
                  <option value="Indeed">Indeed</option>
                  <option value="Referral">Referral</option>
                  <option value="Other">Other</option>
                </select>
              </div>

              <div>
                <label htmlFor="jobUrl" className="mb-2 block text-sm">
                  Job URL
                </label>
                <input
                  id="jobUrl"
                  type="url"
                  name="jobUrl"
                  value={formData.jobUrl}
                  onChange={handleChange}
                  placeholder="https://company.jobs/"
                  className="w-full rounded-lg border border-slate-700 bg-slate-800 p-3 outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label htmlFor="appliedDate" className="mb-2 flex items-center gap-2 text-sm">
                  <CalendarDays size={16} />
                  Applied Date *
                </label>
                <input
                  id="appliedDate"
                  type="date"
                  name="appliedDate"
                  value={formData.appliedDate}
                  onChange={handleChange}
                  max={getTodayDate()}
                  required
                  className="w-full rounded-lg border border-slate-700 bg-slate-800 p-3 outline-none focus:border-blue-500"
                />
              </div>

              <div className="flex flex-col-reverse gap-3 border-t border-slate-800 pt-5 sm:flex-row sm:justify-end">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="rounded-lg border border-slate-700 px-5 py-3 text-sm hover:bg-slate-800"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={saving}
                  className="rounded-lg bg-blue-600 px-5 py-3 text-sm font-semibold hover:bg-blue-700 disabled:opacity-50"
                >
                  {saving ? 'Saving...' : editingId !== null ? 'Update Application' : 'Save Application'}
                </button>
              </div>

            </form>
          </div>
        </div>
      )}

    </div>
  )
}
