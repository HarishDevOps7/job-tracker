
import { useEffect, useState } from 'react'
import type { SubmitEvent } from 'react'
import api from '../../services/api'

interface ChecklistItem {
  id: number
  title: string
  completed: boolean
  completedAt?: string
  createdAt?: string
}

interface InterviewChecklistProps {
  readonly interviewId: number
  readonly companyName: string
  readonly jobTitle: string
  readonly roundName: string
}

function InterviewChecklist({
  interviewId,
  companyName,
  jobTitle,
  roundName,
}: InterviewChecklistProps) {
  const [items, setItems] = useState<ChecklistItem[]>([])
  const [newTask, setNewTask] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const loadChecklist = async () => {
    try {
      setError('')

      const response = await api.get(
        `/interviews/${interviewId}/checklist`
      )

      setItems(response.data)
    } catch {
      setError('Unable to load checklist.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    setLoading(true)
    void loadChecklist()
  }, [interviewId])

  const addTask = async (
    event: SubmitEvent<HTMLFormElement>
  ) => {
    event.preventDefault()

    const title = newTask.trim()

    if (!title) return

    try {
      setSaving(true)
      setError('')

      await api.post(
        `/interviews/${interviewId}/checklist`,
        { title }
      )

      setNewTask('')
      await loadChecklist()
    } catch {
      setError('Unable to add task.')
    } finally {
      setSaving(false)
    }
  }

  const toggleTask = async (item: ChecklistItem) => {
    try {
      setError('')

      await api.patch(
        `/interviews/${interviewId}/checklist/${item.id}`,
        { completed: !item.completed }
      )

      await loadChecklist()
    } catch {
      setError('Unable to update task.')
    }
  }

  const deleteTask = async (itemId: number) => {
    try {
      setError('')

      await api.delete(
        `/interviews/${interviewId}/checklist/${itemId}`
      )

      await loadChecklist()
    } catch {
      setError('Unable to delete task.')
    }
  }

  const resetChecklist = async () => {
    if (!window.confirm('Reset checklist to default tasks?')) {
      return
    }

    try {
      setError('')

      await api.post(
        `/interviews/${interviewId}/checklist/reset`
      )

      await loadChecklist()
    } catch {
      setError('Unable to reset checklist.')
    }
  }

  const completedCount = items.filter(
    item => item.completed
  ).length

  const progress = items.length > 0
    ? Math.round((completedCount / items.length) * 100)
    : 0

  if (loading) {
    return (
      <p className="text-sm text-slate-500">
        Loading checklist...
      </p>
    )
  }

  return (
    <section className="space-y-4">

      {/* Interview Details */}
      <div className="rounded-xl border border-blue-200 bg-blue-50 p-4">
        <p className="text-xs font-semibold uppercase tracking-wide text-blue-600">
          Preparing for interview
        </p>

        <h3 className="mt-1 text-lg font-bold text-slate-800">
          {companyName}
        </h3>

        <p className="text-sm text-slate-700">
          {jobTitle}
        </p>

        <div className="mt-2 flex flex-wrap gap-2">
          <span className="rounded-full bg-white px-3 py-1 text-xs font-medium text-blue-700">
            {roundName}
          </span>

          <span className="rounded-full bg-white px-3 py-1 text-xs text-slate-600">
            Interview ID: {interviewId}
          </span>
        </div>
      </div>

      {/* Checklist Heading */}
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h3 className="text-lg font-semibold text-slate-800">
            Interview Preparation Checklist
          </h3>

          <p className="text-xs text-slate-500">
            {completedCount} of {items.length} tasks completed
          </p>
        </div>

        <button
          type="button"
          onClick={resetChecklist}
          className="text-sm text-blue-600 hover:underline"
        >
          Reset checklist
        </button>
      </div>

      {/* Progress Bar */}
      <div className="h-2 overflow-hidden rounded-full bg-slate-200">
        <div
          className="h-full rounded-full bg-green-500 transition-all"
          style={{ width: `${progress}%` }}
        />
      </div>

      <p className="text-right text-xs font-medium text-slate-600">
        {progress}% completed
      </p>

      {/* Error */}
      {error && (
        <p className="text-sm text-red-600">
          {error}
        </p>
      )}

      {/* Checklist Items */}
      <div className="space-y-2">
        {items.map(item => (
          <div
            key={item.id}
            className="flex items-center gap-3 rounded-lg border border-slate-200 p-3"
          >
            <input
              type="checkbox"
              checked={item.completed}
              onChange={() => void toggleTask(item)}
              className="h-4 w-4 accent-blue-600"
            />

            <span
              className={`flex-1 text-sm ${
                item.completed
                  ? 'text-slate-400 line-through'
                  : 'text-slate-700'
              }`}
            >
              {item.title}
            </span>

            <button
              type="button"
              onClick={() => void deleteTask(item.id)}
              className="text-sm text-red-500 hover:text-red-700"
              aria-label="Delete task"
            >
              Delete
            </button>
          </div>
        ))}
      </div>

      {/* Add New Task */}
      <form onSubmit={addTask} className="flex gap-2">
        <input
          type="text"
          value={newTask}
          onChange={event => setNewTask(event.target.value)}
          placeholder="Add a preparation task..."
          maxLength={200}
          className="min-w-0 flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500"
        />

        <button
          type="submit"
          disabled={saving || !newTask.trim()}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {saving ? 'Adding...' : 'Add'}
        </button>
      </form>

    </section>
  )
}

export default InterviewChecklist
