
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import {
  Briefcase,
  Lock,
  Eye,
  EyeOff,
  CheckCircle,
  ArrowLeft,
} from 'lucide-react'
import { authService } from '../../services/authService'

function ResetPasswordPage() {
  const navigate = useNavigate()
  const location = useLocation()

  const email =
    location.state?.email ||
    sessionStorage.getItem('resetEmail') ||
    ''

  const resetToken = sessionStorage.getItem('resetToken') || ''

  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')

    if (!email || !resetToken) {
      setError('Reset session expired. Please verify your OTP again.')
      return
    }

    if (password.length < 8 || password.length > 100) {
      setError('Password must contain between 8 and 100 characters.')
      return
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    try {
      setLoading(true)

      await authService.getCsrfToken()

      await authService.resetPassword({
        email,
        resetToken,
        newPassword: password,
      })

      sessionStorage.removeItem('resetToken')
      sessionStorage.removeItem('resetEmail')

      setSuccess(true)
    } catch (err: any) {
      setError(
        err.response?.data?.message ||
        'Unable to reset password. Your reset token may have expired.'
      )
    } finally {
      setLoading(false)
    }
  }

  if (success) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center px-4 py-10">
        <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-8 text-center shadow-xl">

          <CheckCircle className="text-green-500 mx-auto mb-5" size={55} />

          <h2 className="text-2xl font-bold text-white">
            Password Reset Successful!
          </h2>

          <p className="text-slate-400 mt-3 text-sm">
            Your password has been updated. You can now log in using your new password.
          </p>

          <button
            onClick={() => navigate('/login', { replace: true })}
            className="w-full mt-7 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 rounded-lg transition"
          >
            Back to Login
          </button>

        </div>
      </div>
    )
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

          <Lock className="text-blue-500 mb-4" size={38} />

          <h2 className="text-2xl font-semibold text-white">
            Reset Password
          </h2>

          <p className="text-slate-400 text-sm mt-2 mb-6">
            Create a new password for {email}.
          </p>

          {error && (
            <div className="bg-red-500/10 border border-red-500/30 text-red-400 p-3 rounded-lg text-sm mb-5">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">

            <div>
              <label className="block text-sm text-slate-300 mb-2">
                New Password
              </label>

              <div className="relative">
                <Lock className="absolute left-3 top-3.5 text-slate-500" size={18} />

                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="Enter new password"
                  minLength={8}
                  maxLength={100}
                  required
                  className="w-full bg-slate-800 border border-slate-700 rounded-lg py-3 pl-10 pr-12 text-white outline-none focus:border-blue-500"
                />

                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-3 text-slate-400 hover:text-white"
                >
                  {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                </button>
              </div>

              <p className="text-xs text-slate-500 mt-2">
                Password must be 8–100 characters.
              </p>
            </div>

            <div>
              <label className="block text-sm text-slate-300 mb-2">
                Confirm New Password
              </label>

              <div className="relative">
                <Lock className="absolute left-3 top-3.5 text-slate-500" size={18} />

                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  placeholder="Confirm new password"
                  required
                  className="w-full bg-slate-800 border border-slate-700 rounded-lg py-3 pl-10 pr-12 text-white outline-none focus:border-blue-500"
                />

                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-3 text-slate-400 hover:text-white"
                >
                  {showConfirmPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading || !password || !confirmPassword}
              className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-semibold py-3 rounded-lg transition"
            >
              {loading ? 'Resetting Password...' : 'Reset Password'}
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

export default ResetPasswordPage
