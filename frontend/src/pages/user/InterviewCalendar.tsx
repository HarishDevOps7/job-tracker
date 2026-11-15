
import '@fullcalendar/react/skeleton.css'
import '@fullcalendar/react/themes/classic/theme.css'
import '@fullcalendar/react/themes/classic/palette.css'
import './InterviewCalendar.css'
import InterviewChecklist from './InterviewChecklist'
import { useCallback, useEffect, useState } from 'react'
import type { ChangeEvent, SubmitEvent } from 'react'
import { useNavigate } from 'react-router-dom'

import FullCalendar from '@fullcalendar/react'
import classicThemePlugin from '@fullcalendar/react/themes/classic'
import dayGridPlugin from '@fullcalendar/react/daygrid'
import timeGridPlugin from '@fullcalendar/react/timegrid'
import interactionPlugin from '@fullcalendar/react/interaction'

import api from '../../services/api'

interface Interview {
  id: number
  jobApplicationId: number
  companyName: string
  jobTitle: string
  roundName: string
  interviewDateTime: string
  mode: 'ONLINE' | 'PHONE' | 'IN_PERSON'
  interviewerName?: string
  meetingLink?: string
  notes?: string
  status:
    | 'SCHEDULED'
    | 'COMPLETED'
    | 'PASSED'
    | 'FAILED'
    | 'RESCHEDULED'
    | 'CANCELLED'
}

interface JobApplication {
  id: number
  companyName: string
  jobTitle: string
}

interface InterviewForm {
  jobApplicationId: number
  roundName: string
  interviewDateTime: string
  mode: 'ONLINE' | 'PHONE' | 'IN_PERSON'
  interviewerName: string
  meetingLink: string
  notes: string
  status: Interview['status']
}

const emptyForm: InterviewForm = {
  jobApplicationId: 0,
  roundName: '',
  interviewDateTime: '',
  mode: 'ONLINE',
  interviewerName: '',
  meetingLink: '',
  notes: '',
  status: 'SCHEDULED',
}

const statusColors: Record<Interview['status'], string> = {
  SCHEDULED: '#2563eb',
  COMPLETED: '#64748b',
  PASSED: '#16a34a',
  FAILED: '#dc2626',
  RESCHEDULED: '#d97706',
  CANCELLED: '#94a3b8',
}

function toLocalInputValue(value: string): string {
  const date = new Date(value)

  const pad = (number: number) =>
    String(number).padStart(2, '0')

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function toLocalDateTime(value: string): string {
  return value.length === 16 ? `${value}:00` : value
}

function getErrorMessage(error: unknown, fallback: string): string {
  if (
    typeof error === 'object' &&
    error !== null &&
    'response' in error
  ) {
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
      fallback
    )
  }

  return fallback
}

function InterviewCalendar() {
  const navigate = useNavigate()

  const [interviews, setInterviews] = useState<Interview[]>([])
  const [applications, setApplications] = useState<JobApplication[]>([])

  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)

  const [form, setForm] = useState<InterviewForm>(emptyForm)

  // Load interviews and job applications
  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      setError('')

      const [interviewResponse, applicationResponse] =
        await Promise.all([
          api.get('/interviews'),
          api.get('/applications'),
        ])

      setInterviews(interviewResponse.data)
      setApplications(applicationResponse.data)
    } catch (err: unknown) {
      setError(
        getErrorMessage(
          err,
          'Unable to load interview calendar.'
        )
      )
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadData()
  }, [loadData])

  // Open new interview form
  const openNewInterview = (date?: string) => {
    let interviewDateTime = ''

    if (date) {
      interviewDateTime = date.includes('T')
        ? date.slice(0, 16)
        : `${date}T09:00`
    }

    setEditingId(null)

    setForm({
      ...emptyForm,
      interviewDateTime,
      jobApplicationId: applications[0]?.id ?? 0,
    })

    setError('')
    setSuccess('')
    setModalOpen(true)
  }

  // Open existing interview
  const openInterview = (interviewId: number) => {
    const interview = interviews.find(
      item => item.id === interviewId
    )

    if (!interview) return

    setEditingId(interview.id)

    setForm({
      jobApplicationId: interview.jobApplicationId,
      roundName: interview.roundName,
      interviewDateTime: toLocalInputValue(
        interview.interviewDateTime
      ),
      mode: interview.mode,
      interviewerName: interview.interviewerName || '',
      meetingLink: interview.meetingLink || '',
      notes: interview.notes || '',
      status: interview.status,
    })

    setError('')
    setSuccess('')
    setModalOpen(true)
  }

  // Handle form changes
  const handleChange = (
    event: ChangeEvent<
      HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
    >
  ) => {
    const { name, value } = event.target

    setForm(previous => ({
      ...previous,
      [name]:
        name === 'jobApplicationId'
          ? Number(value)
          : value,
    }))
  }

  // Create or update interview
  const handleSubmit = async (
    event: SubmitEvent<HTMLFormElement>
  ) => {
    event.preventDefault()

    if (!form.jobApplicationId) {
      setError('Please select a job application.')
      return
    }

    if (!form.interviewDateTime) {
      setError('Please select an interview date and time.')
      return
    }

    try {
      setSaving(true)
      setError('')
      setSuccess('')

      const payload = {
        ...form,
        interviewDateTime: toLocalDateTime(
          form.interviewDateTime
        ),
      }

      if (editingId !== null) {
        await api.put(
          `/interviews/${editingId}`,
          payload
        )

        setSuccess('Interview updated successfully.')
      } else {
        await api.post('/interviews', payload)

        setSuccess('Interview scheduled successfully.')
      }

      setModalOpen(false)
      setForm(emptyForm)

      await loadData()
    } catch (err: unknown) {
      setError(
        getErrorMessage(err, 'Unable to save interview.')
      )
    } finally {
      setSaving(false)
    }
  }

  // Delete interview
  const handleDelete = async () => {
    if (editingId === null) return

    const confirmed = window.confirm(
      'Are you sure you want to delete this interview?'
    )

    if (!confirmed) return

    try {
      setSaving(true)
      setError('')

      await api.delete(`/interviews/${editingId}`)

      setModalOpen(false)
      setSuccess('Interview deleted successfully.')

      await loadData()
    } catch (err: unknown) {
      setError(
        getErrorMessage(err, 'Unable to delete interview.')
      )
    } finally {
      setSaving(false)
    }
  }

  // Update interview status
  const handleStatusChange = async (
    status: Interview['status']
  ) => {
    if (editingId === null) return

    try {
      setSaving(true)
      setError('')

      await api.patch(
        `/interviews/${editingId}/status`,
        { status }
      )

      setForm(previous => ({
        ...previous,
        status,
      }))

      setSuccess('Interview status updated.')

      await loadData()
    } catch (err: unknown) {
      setError(
        getErrorMessage(
          err,
          'Unable to update interview status.'
        )
      )
    } finally {
      setSaving(false)
    }
  }

  // Calendar events
  const events = interviews.map(interview => ({
  id: String(interview.id),
  title: `${interview.companyName} · ${interview.roundName}`,
  start: interview.interviewDateTime,
  color: statusColors[interview.status],
  contrastColor: '#ffffff',
  extendedProps: {
    companyName: interview.companyName,
    roundName: interview.roundName,
    jobTitle: interview.jobTitle,
    mode: interview.mode,
    status: interview.status,
  },
}))
  // Upcoming interviews
  const upcomingInterviews = interviews
    .filter(interview => {
      return (
        interview.status === 'SCHEDULED' &&
        new Date(interview.interviewDateTime).getTime() >=
          Date.now()
      )
    })
    .sort((a, b) => {
      return (
        new Date(a.interviewDateTime).getTime() -
        new Date(b.interviewDateTime).getTime()
      )
    })
    .slice(0, 5)

  // Statistics
  const scheduledCount = interviews.filter(
    item => item.status === 'SCHEDULED'
  ).length

  const completedCount = interviews.filter(
    item => item.status === 'COMPLETED'
  ).length

  const passedCount = interviews.filter(
    item => item.status === 'PASSED'
  ).length

  const failedCount = interviews.filter(
    item => item.status === 'FAILED'
  ).length

  let submitButtonLabel = 'Schedule Interview'

  if (saving) {
    submitButtonLabel = 'Saving...'
  } else if (editingId !== null) {
    submitButtonLabel = 'Update Interview'
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 p-8 text-slate-600">
        Loading interview calendar...
      </div>
    )
  }

  return (
    <div className="interview-calendar-page min-h-screen bg-slate-50 p-4 md:p-8">
      <div className="mx-auto max-w-7xl">

        {/* Header */}
        <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div>
            <h1 className="text-3xl font-bold text-slate-800">
              Interview Calendar
            </h1>

            <p className="mt-1 text-sm text-slate-500">
              Manage your upcoming interviews and schedules.
            </p>
          </div>

          <div className="flex gap-3">
            <button
              type="button"
              onClick={() => navigate('/dashboard')}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-slate-700 transition hover:bg-slate-100"
            >
              Dashboard
            </button>

            <button
              type="button"
              onClick={() => openNewInterview()}
              className="rounded-lg bg-blue-600 px-4 py-2 font-medium text-white shadow-sm transition hover:bg-blue-700"
            >
              + Schedule Interview
            </button>
          </div>
        </div>

        {/* Notifications */}
        {error && (
          <div className="mb-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {success && (
          <div className="mb-4 rounded-lg border border-green-200 bg-green-50 p-3 text-sm text-green-700">
            {success}
          </div>
        )}

        {/* Statistics */}
        <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">

          <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-sm text-slate-500">
              Total Interviews
            </p>
            <p className="mt-2 text-3xl font-bold text-slate-800">
              {interviews.length}
            </p>
          </div>

          <div className="rounded-xl border border-blue-100 bg-white p-5 shadow-sm">
            <p className="text-sm text-slate-500">
              Scheduled
            </p>
            <p className="mt-2 text-3xl font-bold text-blue-600">
              {scheduledCount}
            </p>
          </div>

          <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-sm text-slate-500">
              Completed
            </p>
            <p className="mt-2 text-3xl font-bold text-slate-600">
              {completedCount}
            </p>
          </div>

          <div className="rounded-xl border border-green-100 bg-white p-5 shadow-sm">
            <p className="text-sm text-slate-500">
              Passed
            </p>
            <p className="mt-2 text-3xl font-bold text-green-600">
              {passedCount}
            </p>
          </div>

          <div className="rounded-xl border border-red-100 bg-white p-5 shadow-sm">
            <p className="text-sm text-slate-500">
              Failed
            </p>
            <p className="mt-2 text-3xl font-bold text-red-600">
              {failedCount}
            </p>
          </div>

        </div>

        {/* Calendar and Upcoming Interviews */}
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-4">

          {/* Calendar */}
          <section className="min-w-0 rounded-xl border border-slate-200 bg-white p-4 shadow-sm lg:col-span-3 md:p-5">

            {/* Status Legend */}
            <div className="mb-5 flex flex-wrap gap-x-4 gap-y-2 text-xs font-medium text-slate-600">
              {Object.entries(statusColors).map(
                ([status, color]) => (
                  <span
                    key={status}
                    className="flex items-center gap-1.5"
                  >
                    <span
                      className="h-2.5 w-2.5 rounded-full"
                      style={{ backgroundColor: color }}
                    />
                    {status.replace('_', ' ')}
                  </span>
                )
              )}
            </div>

            {/* FullCalendar */}
<FullCalendar
  className="interview-fullcalendar"

  plugins={[
    classicThemePlugin,
    dayGridPlugin,
    timeGridPlugin,
    interactionPlugin,
  ]}

  initialView="dayGridMonth"

  headerToolbar={{
    left: 'prev,next today',
    center: 'title',
    right: 'dayGridMonth,timeGridWeek,timeGridDay',
  }}

  toolbarClass="calendar-toolbar"
  headerToolbarClass="calendar-header-toolbar"
  toolbarSectionClass="calendar-toolbar-section"
  toolbarTitleClass="calendar-toolbar-title"
  buttonGroupClass="calendar-button-group"

  buttonClass={(info) =>
    info.isSelected
      ? 'calendar-button calendar-button-active'
      : 'calendar-button'
  }

  dayRowClass="calendar-row"
  dayHeaderClass="calendar-day-header"

  dayCellClass={(state) =>
    state.isToday
      ? 'calendar-cell calendar-cell-today'
      : 'calendar-cell'
  }

  dayCellTopClass="calendar-date-area"

  eventClass="calendar-event"
  eventInnerClass="calendar-event-inner"
  eventTimeClass="calendar-event-time"
  eventTitleClass="calendar-event-title"

  events={events}

  dateClick={(info) =>
    openNewInterview(info.dateStr)
  }

  eventClick={(info) =>
    openInterview(Number(info.event.id))
  }
height="auto"
contentHeight={600}
expandRows={false}
dayMaxEvents={3}

  slotMinTime="07:00:00"
  slotMaxTime="22:00:00"
/>            



          </section>

          {/* Upcoming Interviews */}
          <aside className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">

            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-800">
                Upcoming Interviews
              </h2>

              <span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-600">
                {upcomingInterviews.length}
              </span>
            </div>

            {upcomingInterviews.length === 0 ? (
              <div className="flex min-h-40 flex-col items-center justify-center text-center">
                <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-blue-50 text-blue-600">
                  <span className="text-2xl">📅</span>
                </div>

                <p className="text-sm font-medium text-slate-700">
                  No upcoming interviews
                </p>

                <p className="mt-1 text-xs text-slate-400">
                  Your scheduled interviews will appear here.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {upcomingInterviews.map(interview => (
                  <button
                    key={interview.id}
                    type="button"
                    onClick={() => openInterview(interview.id)}
                    className="w-full rounded-xl border border-slate-200 p-3 text-left transition hover:border-blue-300 hover:bg-blue-50"
                  >
                    <div className="mb-2 flex items-start justify-between gap-2">
                      <p className="font-semibold text-slate-800">
                        {interview.companyName}
                      </p>

                      <span className="rounded-full bg-blue-100 px-2 py-1 text-[10px] font-semibold text-blue-700">
                        Scheduled
                      </span>
                    </div>

                    <p className="text-sm text-slate-600">
                      {interview.jobTitle}
                    </p>

                    <p className="mt-2 text-xs text-slate-500">
                      {new Date(
                        interview.interviewDateTime
                      ).toLocaleString()}
                    </p>

                    <p className="mt-1 text-xs font-medium text-blue-600">
                      {interview.roundName} · {interview.mode}
                    </p>
                  </button>
                ))}
              </div>
            )}

            <button
              type="button"
              onClick={() => openNewInterview()}
              className="mt-5 w-full rounded-lg border border-dashed border-blue-300 py-2.5 text-sm font-medium text-blue-600 transition hover:bg-blue-50"
            >
              + Schedule another interview
            </button>

          </aside>

        </div>

        {/* Interview Modal */}
        {modalOpen && (
          <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-slate-900/50 p-3 backdrop-blur-sm">

<div className="my-3 w-full max-w-3xl rounded-2xl border border-slate-100 bg-white p-4 shadow-2xl sm:p-6">              {/* Modal Header */}
              <div className="mb-5 flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-bold text-slate-800">
                    {editingId !== null
                      ? 'Edit Interview'
                      : 'Schedule Interview'}
                  </h2>

                  <p className="mt-1 text-xs text-slate-500">
                    Enter your interview details below.
                  </p>
                </div>

                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="rounded-lg px-3 py-1 text-2xl text-slate-400 transition hover:bg-slate-100 hover:text-slate-800"
                  aria-label="Close"
                >
                  ×
                </button>
              </div>

              {/* Interview Form */}
              <form
                onSubmit={handleSubmit}
                className="space-y-4"
              >

                {/* Job Application */}
                <div>
                  <label
                    htmlFor="jobApplicationId"
                    className="mb-1.5 block text-sm font-medium text-slate-700"
                  >
                    Job Application
                  </label>

                  <select
                    id="jobApplicationId"
                    name="jobApplicationId"
                    value={form.jobApplicationId}
                    onChange={handleChange}
                    required
                    className="w-full rounded-lg border border-slate-300 bg-white p-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  >
                    <option value={0} disabled>
                      Select application
                    </option>

                    {applications.map(application => (
                      <option
                        key={application.id}
                        value={application.id}
                      >
                        {application.companyName} - {application.jobTitle}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Interview Round */}
                <div>
                  <label
                    htmlFor="roundName"
                    className="mb-1.5 block text-sm font-medium text-slate-700"
                  >
                    Interview Round
                  </label>

                  <input
                    id="roundName"
                    name="roundName"
                    value={form.roundName}
                    onChange={handleChange}
                    required
                    placeholder="Technical Round 1"
                    className="w-full rounded-lg border border-slate-300 p-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  />
                </div>

                {/* Date and Time */}
                <div>
                  <label
                    htmlFor="interviewDateTime"
                    className="mb-1.5 block text-sm font-medium text-slate-700"
                  >
                    Interview Date & Time
                  </label>

                  <input
                    id="interviewDateTime"
                    type="datetime-local"
                    name="interviewDateTime"
                    value={form.interviewDateTime}
                    onChange={handleChange}
                    required
                    className="w-full rounded-lg border border-slate-300 p-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  />
                </div>

                {/* Interview Mode */}
                <div>
                  <label
                    htmlFor="mode"
                    className="mb-1.5 block text-sm font-medium text-slate-700"
                  >
                    Interview Mode
                  </label>

                  <select
                    id="mode"
                    name="mode"
                    value={form.mode}
                    onChange={handleChange}
                    className="w-full rounded-lg border border-slate-300 bg-white p-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  >
                    <option value="ONLINE">Online</option>
                    <option value="PHONE">Phone</option>
                    <option value="IN_PERSON">In Person</option>
                  </select>
                </div>

                {/* Interviewer */}
                <div>
                  <label
                    htmlFor="interviewerName"
                    className="mb-1.5 block text-sm font-medium text-slate-700"
                  >
                    Interviewer Name
                  </label>

                  <input
                    id="interviewerName"
                    name="interviewerName"
                    value={form.interviewerName}
                    onChange={handleChange}
                    className="w-full rounded-lg border border-slate-300 p-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  />
                </div>

                {/* Meeting Link */}
                <div>
                  <label
                    htmlFor="meetingLink"
                    className="mb-1.5 block text-sm font-medium text-slate-700"
                  >
                    Meeting Link
                  </label>

                  <input
                    id="meetingLink"
                    type="url"
                    name="meetingLink"
                    value={form.meetingLink}
                    onChange={handleChange}
                    placeholder="https://..."
                    className="w-full rounded-lg border border-slate-300 p-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  />
                </div>

                {/* Notes */}
                <div>
                  <label
                    htmlFor="notes"
                    className="mb-1.5 block text-sm font-medium text-slate-700"
                  >
                    Notes
                  </label>

                  <textarea
                    id="notes"
                    name="notes"
                    value={form.notes}
                    onChange={handleChange}
                    rows={3}
                    className="w-full rounded-lg border border-slate-300 p-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  />
                </div>

                {/* Status */}
                {editingId !== null && (
                  <div>
                    <label
                      htmlFor="status"
                      className="mb-1.5 block text-sm font-medium text-slate-700"
                    >
                      Status
                    </label>

                    <select
                      id="status"
                      name="status"
                      value={form.status}
                      onChange={handleChange}
                      className="w-full rounded-lg border border-slate-300 bg-white p-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                    >
                      {Object.keys(statusColors).map(status => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {/* Form Actions */}
                <div className="flex flex-wrap justify-between gap-3 border-t border-slate-100 pt-4">

                  {editingId !== null && (
                    <button
                      type="button"
                      onClick={handleDelete}
                      disabled={saving}
                      className="rounded-lg bg-red-50 px-4 py-2 text-sm font-medium text-red-700 transition hover:bg-red-100 disabled:opacity-50"
                    >
                      Delete
                    </button>
                  )}

                  <div className="ml-auto flex gap-3">
                    <button
                      type="button"
                      onClick={() => setModalOpen(false)}
                      className="rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
                    >
                      Cancel
                    </button>

                    <button
                      type="submit"
                      disabled={saving}
                      className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-blue-700 disabled:opacity-50"
                    >
                      {submitButtonLabel}
                    </button>
                  </div>

                </div>
              </form>

              {/* Quick Status Updates */}
              {editingId !== null && (
                <div className="mt-5 border-t border-slate-200 pt-4">

                  <p className="mb-3 text-sm font-semibold text-slate-700">
                    Quick status update
                  </p>

                  <div className="flex flex-wrap gap-2">
                    {(
                      [
                        'COMPLETED',
                        'PASSED',
                        'FAILED',
                        'CANCELLED',
                      ] as const
                    ).map(status => (
                      <button
                        key={status}
                        type="button"
                        disabled={
                          saving || form.status === status
                        }
                        onClick={() => handleStatusChange(status)}
                        className="rounded-lg border px-3 py-2 text-xs font-medium transition hover:bg-slate-50 disabled:opacity-40"
                        style={{
                          borderColor: statusColors[status],
                          color: statusColors[status],
                        }}
                      >
                        {status}
                      </button>
                    ))}
                  </div>

                </div>
              )}

              {/* Interview Preparation Checklist - outside the interview form */}
              {editingId !== null && (
                <div className="mt-6 border-t border-slate-200 pt-5">
                  <InterviewChecklist
                    key={editingId}
                    interviewId={editingId}
                    companyName={
                      interviews.find(item => item.id === editingId)?.companyName ?? ''
                    }
                    jobTitle={
                      interviews.find(item => item.id === editingId)?.jobTitle ?? ''
                    }
                    roundName={
                      interviews.find(item => item.id === editingId)?.roundName ?? ''
                    }
                  />
                </div>
              )}

            </div>
          </div>
        )}

      </div>
    </div>
  )
}

export default InterviewCalendar
