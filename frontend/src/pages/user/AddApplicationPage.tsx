
import { useState, type FormEvent, type ChangeEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ArrowLeft,
  Briefcase,
  CalendarDays,
  Save,
} from 'lucide-react'
import api from '../../services/api'

interface ApplicationForm {
  companyName: string
  jobTitle: string
  source: string
  jobUrl: string
  appliedDate: string
}

// Format today's date using local time
function getTodayDate(): string {
  const today = new Date()

  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')

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
      'Unable to save application. Please try again.'
    )
  }

  return 'Unable to save application. Please try again.'
}

export default function AddApplicationPage() {
  const navigate = useNavigate()

  const today = getTodayDate()

  const [formData, setFormData] = useState<ApplicationForm>({
    companyName: '',
    jobTitle: '',
    source: '',
    jobUrl: '',
    appliedDate: today,
  })

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const handleChange = (
    e: ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target

    setFormData(previous => ({
      ...previous,
      [name]: value,
    }))
  }

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()

    setError('')
    setSuccess('')

    const companyName = formData.companyName.trim()
    const jobTitle = formData.jobTitle.trim()
    const jobUrl = formData.jobUrl.trim()

    if (!companyName || !jobTitle) {
      setError('Company name and job title are required.')
      return
    }

    if (formData.appliedDate > today) {
      setError('Applied date cannot be in the future.')
      return
    }

    try {
      setLoading(true)

      const payload = {
        companyName,
        jobTitle,
        source: formData.source,
        jobUrl,
        appliedDate: formData.appliedDate,
      }

      await api.post('/applications', payload)

      setSuccess('Job application added successfully!')

      setTimeout(() => {
        navigate('/dashboard', { replace: true })
      }, 1000)

    } catch (err: unknown) {
      console.error('Application submission failed:', err)
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 p-4 text-white sm:p-6 md:p-10">

      <div className="mx-auto max-w-3xl">

        <button
          type="button"
          onClick={() => navigate('/dashboard')}
          className="mb-6 flex items-center gap-2 text-slate-400 transition hover:text-white sm:mb-8"
        >
          <ArrowLeft size={18} />
          Back to Dashboard
        </button>

        <div className="rounded-xl border border-slate-800 bg-slate-900 p-4 sm:p-6 md:p-8">

          <div className="mb-6 flex items-center gap-3 sm:mb-8">
            <Briefcase className="shrink-0 text-blue-400" size={32} />

            <div>
              <h1 className="text-xl font-bold sm:text-2xl">
                Add Job Application
              </h1>

              <p className="mt-1 text-sm text-slate-400">
                Enter your job application details.
              </p>
            </div>
          </div>

          {error && (
            <div
              role="alert"
              className="mb-5 rounded-lg border border-red-700 bg-red-900/30 p-3 text-sm text-red-300"
            >
              {error}
            </div>
          )}

          {success && (
            <div
              role="status"
              className="mb-5 rounded-lg border border-green-700 bg-green-900/30 p-3 text-sm text-green-300"
            >
              {success}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">

            <div>
              <label htmlFor="companyName" className="mb-2 block text-sm">
                Company Name *
              </label>

              <input
                id="companyName"
                type="text"
                name="companyName"
                value={formData.companyName}
                onChange={handleChange}
                placeholder="Example: Amazon"
                required
                maxLength={150}
                className="w-full rounded-lg border border-slate-700 bg-slate-800 p-3 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
              />
            </div>

            <div>
              <label htmlFor="jobTitle" className="mb-2 block text-sm">
                Job Title *
              </label>

              <input
                id="jobTitle"
                type="text"
                name="jobTitle"
                value={formData.jobTitle}
                onChange={handleChange}
                placeholder="Example: Java Developer"
                required
                maxLength={150}
                className="w-full rounded-lg border border-slate-700 bg-slate-800 p-3 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
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
                className="w-full rounded-lg border border-slate-700 bg-slate-800 p-3 outline-none transition focus:border-blue-500"
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
                className="w-full rounded-lg border border-slate-700 bg-slate-800 p-3 outline-none transition focus:border-blue-500"
              />
            </div>

            <div>
              <label
                htmlFor="appliedDate"
                className="mb-2 flex items-center gap-2 text-sm"
              >
                <CalendarDays size={16} />
                Applied Date *
              </label>

              <input
                id="appliedDate"
                type="date"
                name="appliedDate"
                value={formData.appliedDate}
                onChange={handleChange}
                max={today}
                required
                className="w-full rounded-lg border border-slate-700 bg-slate-800 p-3 outline-none transition focus:border-blue-500"
              />
            </div>

            <div className="text-sm text-slate-400">
              Initial application status will automatically be APPLIED.
            </div>

            <button
              type="submit"
              disabled={loading}
              className="flex w-full items-center justify-center gap-2 rounded-lg bg-blue-600 px-5 py-3 font-semibold transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Save size={18} />
              {loading ? 'Saving...' : 'Save Application'}
            </button>

          </form>
        </div>
      </div>
    </div>
  )
}
