
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  Briefcase,
  User,
  Mail,
  Lock,
  Eye,
  EyeOff,
  ShieldCheck,
  Phone,
} from 'lucide-react'
import { authService } from '../../services/authService'

type RegistrationMethod = 'email' | 'mobile'

function RegisterPage() {
  const navigate = useNavigate()

  const [method, setMethod] = useState<RegistrationMethod>('email')
  const [otpStep, setOtpStep] = useState(false)

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [mobileNumber, setMobileNumber] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [otp, setOtp] = useState('')

  const [showPassword, setShowPassword] = useState(false)
  const [serverError, setServerError] = useState('')
  const [loading, setLoading] = useState(false)

  const onSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setServerError('')

    if (fullName.trim().length < 2) {
      setServerError('Please enter your full name.')
      return
    }

    if (method === 'email') {
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        setServerError('Enter a valid email address.')
        return
      }

      if (password.length < 8) {
        setServerError('Password must contain at least 8 characters.')
        return
      }

      if (password !== confirmPassword) {
        setServerError('Passwords do not match.')
        return
      }
    }

    if (method === 'mobile') {
      if (!/^\+?[0-9]{10,15}$/.test(mobileNumber.trim())) {
        setServerError('Enter a valid mobile number.')
        return
      }
    }

    try {
      setLoading(true)

      await authService.getCsrfToken()

      if (method === 'email') {
        await authService.register({
          fullName: fullName.trim(),
          email: email.trim(),
          password,
        })
      } else {
        await authService.registerMobile({
          fullName: fullName.trim(),
          mobileNumber: mobileNumber.trim(),
        })
      }

      setOtp('')
      setOtpStep(true)
    } catch (error: any) {
      setServerError(
        error.response?.data?.message ||
        'Registration failed. Please try again.'
      )
    } finally {
      setLoading(false)
    }
  }

  const verifyOtp = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setServerError('')

    if (!/^\d{6}$/.test(otp)) {
      setServerError('Enter a valid 6-digit OTP.')
      return
    }

    try {
      setLoading(true)

      await authService.getCsrfToken()

      if (method === 'email') {
        await authService.verifyEmailOtp({
          email: email.trim(),
          otp,
        })
      } else {
        await authService.verifyMobileOtp({
          mobileNumber: mobileNumber.trim(),
          otp,
        })
      }

      navigate('/login', {
        state: {
          message:
            method === 'email'
              ? 'Email verified successfully. Please log in.'
              : 'Mobile number verified successfully. Please log in.',
        },
      })
    } catch (error: any) {
      setServerError(
        error.response?.data?.message ||
        'OTP verification failed. Please try again.'
      )
    } finally {
      setLoading(false)
    }
  }

  const changeMethod = (nextMethod: RegistrationMethod) => {
    setMethod(nextMethod)
    setServerError('')
    setOtp('')
  }

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center px-4 py-10">
      <div className="w-full max-w-md">

        {/* Header */}
        <div className="text-center mb-8">
          <div className="inline-flex bg-blue-600 p-4 rounded-2xl mb-4">
            <Briefcase className="text-white" size={32} />
          </div>

          <h1 className="text-3xl font-bold text-white">
            Job Tracker
          </h1>

          <p className="text-slate-400 mt-2">
            Start managing your career today.
          </p>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 shadow-xl">

          {otpStep ? (
            <>
              {/* OTP Verification */}
              <div className="flex justify-center mb-5">
                <ShieldCheck className="text-blue-500" size={48} />
              </div>

              <h2 className="text-2xl font-semibold text-white text-center">
                Verify your {method === 'email' ? 'email' : 'mobile number'}
              </h2>

              <p className="text-slate-400 text-sm mt-3 mb-7 text-center">
                We sent a 6-digit verification code to{' '}
                <span className="text-blue-400">
                  {method === 'email' ? email : mobileNumber}
                </span>.
                {method === 'email' && ' The code is valid for 5 minutes.'}
              </p>

              {serverError && (
                <div className="bg-red-500/10 border border-red-500/30 text-red-400 p-3 rounded-lg text-sm mb-5">
                  {serverError}
                </div>
              )}

              <form onSubmit={verifyOtp} className="space-y-5">

                <div>
                  <label className="block text-sm text-slate-300 mb-2">
                    Enter verification OTP
                  </label>

                  <input
                    type="text"
                    inputMode="numeric"
                    maxLength={6}
                    value={otp}
                    onChange={(e) =>
                      setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))
                    }
                    placeholder="Enter 6-digit OTP"
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg py-3 px-4 text-white text-center tracking-[0.5em] text-xl outline-none focus:border-blue-500"
                  />
                </div>

                <button
                  type="submit"
                  disabled={loading || otp.length !== 6}
                  className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-semibold py-3 rounded-lg transition"
                >
                  {loading ? 'Verifying...' : 'Verify OTP'}
                </button>

                <button
                  type="button"
                  onClick={() => {
                    setOtpStep(false)
                    setOtp('')
                    setServerError('')
                  }}
                  className="w-full text-slate-400 hover:text-white text-sm"
                >
                  Back to registration
                </button>

              </form>
            </>
          ) : (
            <>
              {/* Registration */}
              <h2 className="text-2xl font-semibold text-white">
                Create your account
              </h2>

              <p className="text-slate-400 text-sm mt-2 mb-6">
                Choose your registration method.
              </p>

              {/* Registration Method */}
              <div className="grid grid-cols-2 gap-3 mb-6">

                <button
                  type="button"
                  onClick={() => changeMethod('email')}
                  className={`py-3 rounded-lg font-medium transition ${
                    method === 'email'
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-800 text-slate-400 hover:text-white'
                  }`}
                >
                  <Mail size={17} className="inline mr-2" />
                  Email
                </button>

                <button
                  type="button"
                  onClick={() => changeMethod('mobile')}
                  className={`py-3 rounded-lg font-medium transition ${
                    method === 'mobile'
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-800 text-slate-400 hover:text-white'
                  }`}
                >
                  <Phone size={17} className="inline mr-2" />
                  Mobile
                </button>

              </div>

              {serverError && (
                <div className="bg-red-500/10 border border-red-500/30 text-red-400 p-3 rounded-lg text-sm mb-5">
                  {serverError}
                </div>
              )}

              <form onSubmit={onSubmit} className="space-y-5">

                {/* Full Name */}
                <div>
                  <label className="block text-sm text-slate-300 mb-2">
                    Full name
                  </label>

                  <div className="relative">
                    <User className="absolute left-3 top-3.5 text-slate-500" size={18} />

                    <input
                      type="text"
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                      placeholder="Your full name"
                      className="w-full bg-slate-800 border border-slate-700 rounded-lg py-3 pl-10 pr-4 text-white outline-none focus:border-blue-500"
                    />
                  </div>
                </div>

                {method === 'email' ? (
                  <>
                    {/* Email */}
                    <div>
                      <label className="block text-sm text-slate-300 mb-2">
                        Email address
                      </label>

                      <div className="relative">
                        <Mail className="absolute left-3 top-3.5 text-slate-500" size={18} />

                        <input
                          type="email"
                          value={email}
                          onChange={(e) => setEmail(e.target.value)}
                          placeholder="you@example.com"
                          className="w-full bg-slate-800 border border-slate-700 rounded-lg py-3 pl-10 pr-4 text-white outline-none focus:border-blue-500"
                        />
                      </div>
                    </div>

                    {/* Password */}
                    <div>
                      <label className="block text-sm text-slate-300 mb-2">
                        Password
                      </label>

                      <div className="relative">
                        <Lock className="absolute left-3 top-3.5 text-slate-500" size={18} />

                        <input
                          type={showPassword ? 'text' : 'password'}
                          value={password}
                          onChange={(e) => setPassword(e.target.value)}
                          placeholder="Minimum 8 characters"
                          className="w-full bg-slate-800 border border-slate-700 rounded-lg py-3 pl-10 pr-12 text-white outline-none focus:border-blue-500"
                        />

                        <button
                          type="button"
                          onClick={() => setShowPassword(!showPassword)}
                          className="absolute right-3 top-3 text-slate-400"
                        >
                          {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                        </button>
                      </div>
                    </div>

                    {/* Confirm Password */}
                    <div>
                      <label className="block text-sm text-slate-300 mb-2">
                        Confirm password
                      </label>

                      <input
                        type="password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        placeholder="Re-enter your password"
                        className="w-full bg-slate-800 border border-slate-700 rounded-lg py-3 px-4 text-white outline-none focus:border-blue-500"
                      />
                    </div>
                  </>
                ) : (
                  /* Mobile */
                  <div>
                    <label className="block text-sm text-slate-300 mb-2">
                      Mobile number
                    </label>

                    <div className="relative">
                      <Phone className="absolute left-3 top-3.5 text-slate-500" size={18} />

                      <input
                        type="tel"
                        value={mobileNumber}
                        onChange={(e) => setMobileNumber(e.target.value)}
                        placeholder="Enter mobile number"
                        className="w-full bg-slate-800 border border-slate-700 rounded-lg py-3 pl-10 pr-4 text-white outline-none focus:border-blue-500"
                      />
                    </div>

                    <p className="text-slate-500 text-xs mt-2">
                      Include country code if required by your backend.
                    </p>
                  </div>
                )}

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-semibold py-3 rounded-lg transition"
                >
                  {loading ? 'Creating account...' : 'Create Account'}
                </button>

              </form>
            </>
          )}

          {/* Google Registration - hidden during OTP verification */}
          {!otpStep && (
            <>
              <div className="relative my-6">
                <div className="absolute inset-0 flex items-center">
                  <div className="w-full border-t border-slate-700"></div>
                </div>

                <div className="relative flex justify-center">
                  <span className="bg-slate-900 px-3 text-sm text-slate-500">
                    OR CONTINUE WITH
                  </span>
                </div>
              </div>

              <button
                type="button"
                onClick={() => authService.googleLogin()}
                className="w-full flex items-center justify-center gap-3 bg-white hover:bg-slate-200 text-slate-900 font-semibold py-3 rounded-lg transition"
              >
                <span className="font-bold text-xl">G</span>
                Continue with Google
              </button>
            </>
          )}

          {/* Login Link */}
          <div className="mt-7 text-center text-sm text-slate-400">
            Already have an account?{' '}
            <Link
              to="/login"
              className="text-blue-400 hover:text-blue-300 font-semibold"
            >
              Sign in
            </Link>
          </div>

        </div>
      </div>
    </div>
  )
}

export default RegisterPage
