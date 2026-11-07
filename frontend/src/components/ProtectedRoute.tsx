import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import type { ReactNode } from 'react'

interface ProtectedRouteProps {
  children: ReactNode
  allowedRole?: 'ADMIN' | 'USER'
}

export default function ProtectedRoute({
  children,
  allowedRole,
}: ProtectedRouteProps) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <div className="p-6">Checking authentication...</div>
  }

  // User is not logged in
  if (!user) {
    return (
      <Navigate
        to="/login"
        state={{ from: location }}
        replace
      />
    )
  }

  // User does not have the required role
  if (allowedRole && user.role !== allowedRole) {
    return (
      <Navigate
        to={user.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard'}
        replace
      />
    )
  }

  return <>{children}</>
}