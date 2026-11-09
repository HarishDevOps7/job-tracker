
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Briefcase, ShieldCheck, ArrowLeft } from 'lucide-react'
import { authService } from '../../services/authService'

function VerifyResetOtpPage() {
  const navigate = useNavigate()
  const location = useLocation()

  const email =
    location.state?.email ||
    sessionStorage.getItem('resetEmail') ||
    ''

  const [otp, setOtp] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')

    if (!email) {
      setError('Email address is missing. Please request a new OTP.')
      return
    }

    if (!/^\d{6}$/.test(otp)) {
      setError('Enter a valid 6-digit OTP.')
      return
    }

    try {
      setLoading(true)

      await authService.getCsrfToken()

      const response = await authService.verifyResetOtp({
        email,
        otp,
      })

      const resetToken = response.data.resetToken

      if (!resetToken) {
        setError('Reset token was not received. Please try again.')
        return
      }

      // Keep the reset token for the next page only.
      sessionStorage.setItem('resetToken', resetToken)

      navigate('/reset-password', {
        state: { email },
      })
    } catch (err: any) {
      setError(
        err.response?.data?.message ||
        'OTP verification failed. Please check your OTP.'
      )
    } finally {
      setLoading(false)
    }
  }

  if (!email) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center px-4">
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 max-w-md w-full text-center">
          <h2 className="text-xl text-white font-semibold">
            Email address missing
          </h2>
          <p className="text-slate-400 text-sm mt-3">
            Please request a password reset OTP first.
          </p>
          <Link
            to="/forgot-password"
            className="inline-block mt-6 text-blue-400 hover:text-blue-300"
          >
            Request OTP
          </Link>
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
          <h1 className="text-3xl font-bold text-white">Job Tracker</h1>
          <p className="text-slate-400 mt-2">
            Manage your career. Track your success.
          </p>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 shadow-xl">

          <ShieldCheck className="text-blue-500 mb-4" size={40} />

          <h2 className="text-2xl font-semibold text-white">
            Verify OTP
          </h2>

          <p className="text-slate-400 text-sm mt-2 mb-6">
            Enter the 6-digit code sent to {email}.
          </p>

          {error && (
            <div className="bg-red-500/10 border border-red-500/30 text-red-400 p-3 rounded-lg text-sm mb-5">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">

            <div>
              <label className="block text-sm text-slate-300 mb-2">
                6-digit OTP
              </label>

              <input
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                value={otp}
                onChange={(event) =>
                  setOtp(event.target.value.replace(/\D/g, '').slice(0, 6))
                }
                placeholder="Enter OTP"
                className="w-full bg-slate-800 border border-slate-700 rounded-lg py-4 px-4 text-white text-center tracking-[0.6em] text-xl outline-none focus:border-blue-500"
              />
            </div>

            <button
              type="submit"
              disabled={loading || otp.length !== 6}
              className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-semibold py-3 rounded-lg transition"
            >
              {loading ? 'Verifying...' : 'Verify OTP'}
            </button>

          </form>

          <div className="mt-7 text-center">
            <Link
              to="/forgot-password"
              className="inline-flex items-center gap-2 text-sm text-blue-400 hover:text-blue-300"
            >
              <ArrowLeft size={16} />
              Back to Forgot Password
            </Link>
          </div>

        </div>
      </div>
    </div>
  )
}

export default VerifyResetOtpPage
