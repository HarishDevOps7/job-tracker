
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Briefcase, Mail, ArrowLeft } from 'lucide-react'
import { authService } from '../../services/authService'

function ForgotPasswordPage() {
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (!email.trim()) {
      setError('Please enter your email address.')
      return
    }

    try {
      setLoading(true)

      await authService.getCsrfToken()

      const response = await authService.forgotPassword({
        email: email.trim(),
      })

      setSuccess(response.data.message)

      sessionStorage.setItem('resetEmail', email.trim())

      navigate('/verify-reset-otp', {
        state: { email: email.trim() },
      })
    } catch (err: any) {
      setError(
        err.response?.data?.message ||
        'Unable to process your request. Please try again.'
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center px-4 py-10">
      <div className="w-full max-w-md">

        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center rounded-2xl bg-blue-600 p-4 mb-4">
            <Briefcase className="text-white" size={32} />
          </div>

          <h1 className="text-3xl font-bold text-white">
            Job Tracker
          </h1>

          <p className="text-slate-400 mt-2">
            Manage your career. Track your success.
          </p>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 shadow-xl">

          <Mail className="text-blue-500 mb-4" size={35} />

          <h2 className="text-2xl font-semibold text-white">
            Forgot Password?
          </h2>

          <p className="text-slate-400 text-sm mt-2 mb-6">
            Enter your registered email address. If eligible,
            we'll send you a password reset OTP.
          </p>

          {error && (
            <div className="bg-red-500/10 border border-red-500/30 text-red-400 p-3 rounded-lg text-sm mb-5">
              {error}
            </div>
          )}

          {success && (
            <div className="bg-green-500/10 border border-green-500/30 text-green-400 p-3 rounded-lg text-sm mb-5">
              {success}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">

            <div>
              <label className="block text-sm text-slate-300 mb-2">
                Email address
              </label>

              <div className="relative">
                <Mail
                  className="absolute left-3 top-3.5 text-slate-500"
                  size={18}
                />

                <input
                  type="email"
                  required
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="you@example.com"
                  className="w-full bg-slate-800 border border-slate-700 rounded-lg py-3 pl-10 pr-4 text-white outline-none focus:border-blue-500"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-semibold py-3 rounded-lg transition"
            >
              {loading ? 'Sending OTP...' : 'Send Reset OTP'}
            </button>

          </form>

          <div className="mt-7 text-center">
            <Link
              to="/login"
              className="inline-flex items-center gap-2 text-sm text-blue-400 hover:text-blue-300"
            >
              <ArrowLeft size={16} />
              Back to Login
            </Link>
          </div>

        </div>
      </div>
    </div>
  )
}

export default ForgotPasswordPage
