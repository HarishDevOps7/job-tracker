
import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'

import { authService, type AuthResponse } from '../services/authService'

interface AuthContextType {
  user: AuthResponse | null
  loading: boolean
  refreshUser: () => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(null)
  const [loading, setLoading] = useState(true)

  const refreshUser = async () => {
    try {
      const response = await authService.getCurrentUser()
      setUser(response.data)
    } catch {
      setUser(null)
    }
  }

  const logout = async () => {
  try {
    await authService.getCsrfToken()
    await authService.logout()
  } finally {
    setUser(null)
  }
}

  useEffect(() => {
    const initializeAuth = async () => {
      try {
        await authService.getCsrfToken()
        await refreshUser()
      } finally {
        setLoading(false)
      }
    }

    initializeAuth()
  }, [])

  return (
    <AuthContext.Provider
      value={{ user, loading, refreshUser, logout }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }

  return context
}
