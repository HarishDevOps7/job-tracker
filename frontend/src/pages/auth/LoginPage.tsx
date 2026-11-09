
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router-dom'
import {
  Eye,
  EyeOff,
  Briefcase,
  Mail,
  Lock,
  Phone,
  ShieldCheck,
} from 'lucide-react'
import { authService } from '../../services/authService'
import { useAuth } from '../../context/AuthContext'

const loginSchema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(8, 'Password must contain at least 8 characters'),
})

type LoginFormData = z.infer<typeof loginSchema>
type LoginMethod = 'email' | 'mobile'

function LoginPage() {
  const navigate = useNavigate()
  const { refreshUser } = useAuth()

  const [method, setMethod] = useState<LoginMethod>('email')
  const [showPassword, setShowPassword] = useState(false)
  const [serverError, setServerError] = useState('')
  const [loading, setLoading] = useState(false)

  const [mobileNumber, setMobileNumber] = useState('')
  const [otp, setOtp] = useState('')
  const [otpStep, setOtpStep] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  })

  const redirectUser = async () => {
    await refreshUser()

    const response = await authService.getCurrentUser()

    if (response.data.role === 'ADMIN') {
      navigate('/admin/dashboard')
    } else {
      navigate('/dashboard')
    }
  }

  const onEmailLogin = async (data: LoginFormData) => {
    try {
      setLoading(true)
      setServerError('')

      await authService.getCsrfToken()
      await authService.login(data)

      await redirectUser()
    } catch (error: any) {
      setServerError(
        error.response?.data?.message ||
        'Login failed. Please check your credentials.'
      )
    } finally {
      setLoading(false)
    }
  }

  const sendMobileOtp = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setServerError('')

    if (!/^\+?[0-9]{10,15}$/.test(mobileNumber.trim())) {
      setServerError('Enter a valid mobile number.')
      return
    }

    try {
      setLoading(true)

      await authService.getCsrfToken()

      await authService.sendMobileLoginOtp({
        mobileNumber: mobileNumber.trim(),
      })

      setOtp('')
      setOtpStep(true)
    } catch (error: any) {
      setServerError(
        error.response?.data?.message ||
        'Unable to send OTP. Please try again.'
      )
    } finally {
      setLoading(false)
    }
  }

  const verifyMobileOtp = async (
    event: React.FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault()
    setServerError('')

    if (!/^\d{6}$/.test(otp)) {
      setServerError('Enter a valid 6-digit OTP.')
      return
    }

    try {
      setLoading(true)

      await authService.getCsrfToken()

      await authService.verifyMobileLoginOtp({
        mobileNumber: mobileNumber.trim(),
        otp,
      })

      await redirectUser()
    } catch (error: any) {
      setServerError(
        error.response?.data?.message ||
        'OTP verification failed. Please try again.'
      )
    } finally {
      setLoading(false)
    }
  }

  const changeMethod = (nextMethod: LoginMethod) => {
    setMethod(nextMethod)
    setOtpStep(false)
    setOtp('')
    setServerError('')
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

          <h2 className="text-2xl font-semibold text-white">
            Welcome back
          </h2>

          <p className="text-slate-400 text-sm mt-2 mb-6">
            Sign in to continue to your account.
          </p>

          {!otpStep && (
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
          )}

          {serverError && (
            <div className="bg-red-500/10 border border-red-500/30 text-red-400 p-3 rounded-lg text-sm mb-5">
              {serverError}
            </div>
          )}

          {method === 'email' ? (
            <form
              onSubmit={handleSubmit(onEmailLogin)}
              className="space-y-5"
            >
              <div>
                <label className="block text-sm text-slate-300 mb-2">
                  Email address
                </label>

                <div className="relative">
                  <Mail className="absolute left-3 top-3.5 text-slate-500" size={18} />

                  <input
                    type="email"
                    placeholder="you@example.com"
                    {...register('email')}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg py-3 pl-10 pr-4 text-white outline-none focus:border-blue-500"
                  />
                </div>

                {errors.email && (
                  <p className="text-red-400 text-xs mt-2">
                    {errors.email.message}
                  </p>
                )}
              </div>

              <div>
                <label className="block text-sm text-slate-300 mb-2">
                  Password
                </label>

                <div className="relative">
                  <Lock className="absolute left-3 top-3.5 text-slate-500" size={18} />

                  <input
                    type={showPassword ? 'text' : 'password'}
                    placeholder="Enter your password"
                    {...register('password')}
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

                {errors.password && (
                  <p className="text-red-400 text-xs mt-2">
                    {errors.password.message}
                  </p>
                )}
              </div>

              <div className="flex justify-end">
                <Link
  to="/forgot-password"
  className="text-sm text-blue-400 hover:text-blue-300"
>
  Forgot password?
</Link>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-semibold py-3 rounded-lg transition"
              >
                {loading ? 'Signing in...' : 'Sign In'}
              </button>
            </form>
          ) : (
            otpStep ? (
              <form onSubmit={verifyMobileOtp} className="space-y-5">
                <div className="text-center">
                  <ShieldCheck className="text-blue-500 mx-auto mb-3" size={45} />

                  <h3 className="text-xl font-semibold text-white">
                    Verify mobile OTP
                  </h3>

                  <p className="text-slate-400 text-sm mt-2">
                    Enter the verification code for {mobileNumber}.
                  </p>
                </div>

                <div>
                  <label className="block text-sm text-slate-300 mb-2">
                    Enter 6-digit OTP
                  </label>

                  <input
                    type="text"
                    inputMode="numeric"
                    maxLength={6}
                    value={otp}
                    onChange={(e) =>
                      setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))
                    }
                    placeholder="Enter OTP"
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg py-3 px-4 text-white text-center tracking-[0.5em] text-xl outline-none focus:border-blue-500"
                  />
                </div>

                <button
                  type="submit"
                  disabled={loading || otp.length !== 6}
                  className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-semibold py-3 rounded-lg transition"
                >
                  {loading ? 'Verifying...' : 'Verify & Sign In'}
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
                  Change mobile number
                </button>
              </form>
            ) : (
              <form onSubmit={sendMobileOtp} className="space-y-5">
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
                      placeholder="Enter registered mobile number"
                      className="w-full bg-slate-800 border border-slate-700 rounded-lg py-3 pl-10 pr-4 text-white outline-none focus:border-blue-500"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-semibold py-3 rounded-lg transition"
                >
                  {loading ? 'Sending OTP...' : 'Send OTP'}
                </button>
              </form>
            )
          )}
            
          {/* Google Login */}
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
            <svg viewBox="0 0 48 48" className="w-5 h-5">
              <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 3.06 13.22l7.98 6.19C12.94 13.72 18.02 9.5 24 9.5z" />
              <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.76 7.18l7.73 6C44.42 37.99 46.98 31.76 46.98 24.55z" />
              <path fill="#FBBC05" d="M10.04 28.59A14.4 14.4 0 0 1 9.26 24c0-1.59.27-3.13.76-4.59l-7.98-6.19A23.9 23.9 0 0 0 0 24c0 3.86.92 7.51 2.54 10.78l7.5-6.19z" />
              <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.91-5.8l-7.73-6c-2.14 1.45-4.87 2.3-8.18 2.3-5.98 0-11.06-4.22-12.96-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
            </svg>
            Continue with Google
          </button>

          <div className="mt-7 text-center text-sm text-slate-400">
            Don't have an account?{' '}
            <Link
              to="/register"
              className="text-blue-400 hover:text-blue-300 font-semibold"
            >
              Create account
            </Link>
          </div>
        </div>

        <p className="text-center text-slate-600 text-xs mt-6">
          © 2026 Job Tracker. All rights reserved.
        </p>

      </div>
    </div>
  )
}

export default LoginPage
