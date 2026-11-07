
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router-dom'
import { Briefcase, User, Mail, Lock, Eye, EyeOff } from 'lucide-react'
import { authService } from '../../services/authService'

const registerSchema = z.object({
  fullName: z.string().trim().min(2, 'Enter your full name'),
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(8, 'Password must contain at least 8 characters'),
  confirmPassword: z.string().min(8, 'Confirm your password'),
}).refine(data => data.password === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

type RegisterFormData = z.infer<typeof registerSchema>

function RegisterPage() {
  const navigate = useNavigate()

  const [showPassword, setShowPassword] = useState(false)
  const [serverError, setServerError] = useState('')
  const [loading, setLoading] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  })

  const onSubmit = async (data: RegisterFormData) => {
    try {
      setLoading(true)
      setServerError('')

      await authService.getCsrfToken()

      await authService.register({
        fullName: data.fullName,
        email: data.email,
        password: data.password,
      })

      navigate('/login', {
        state: { message: 'Registration successful. Please log in.' },
      })

    } catch (error: any) {
      setServerError(
        error.response?.data?.message ||
        'Registration failed. Please try again.'
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center px-4 py-10">

      <div className="w-full max-w-md">

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

          <h2 className="text-2xl font-semibold text-white">
            Create your account
          </h2>

          <p className="text-slate-400 text-sm mt-2 mb-7">
            Join Job Tracker and organize your job search.
          </p>

          {serverError && (
            <div className="bg-red-500/10 border border-red-500/30 text-red-400 p-3 rounded-lg text-sm mb-5">
              {serverError}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">

            <div>
              <label className="block text-sm text-slate-300 mb-2">
                Full name
              </label>

              <div className="relative">
                <User className="absolute left-3 top-3.5 text-slate-500" size={18} />

                <input
                  type="text"
                  placeholder="Your full name"
                  {...register('fullName')}
                  className="w-full bg-slate-800 border border-slate-700 rounded-lg py-3 pl-10 pr-4 text-white outline-none focus:border-blue-500"
                />
              </div>

              {errors.fullName && (
                <p className="text-red-400 text-xs mt-2">
                  {errors.fullName.message}
                </p>
              )}
            </div>

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
                  placeholder="Minimum 8 characters"
                  {...register('password')}
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

              {errors.password && (
                <p className="text-red-400 text-xs mt-2">
                  {errors.password.message}
                </p>
              )}
            </div>

            <div>
              <label className="block text-sm text-slate-300 mb-2">
                Confirm password
              </label>

              <input
                type="password"
                placeholder="Re-enter your password"
                {...register('confirmPassword')}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg py-3 px-4 text-white outline-none focus:border-blue-500"
              />

              {errors.confirmPassword && (
                <p className="text-red-400 text-xs mt-2">
                  {errors.confirmPassword.message}
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-60 text-white font-semibold py-3 rounded-lg transition"
            >
              {loading ? 'Creating account...' : 'Create Account'}
            </button>

          </form>

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
