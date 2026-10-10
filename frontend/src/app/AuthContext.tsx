import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authApi } from '../api/auth'
import { configureAuthCallbacks, setAuthTokens, type AuthResult, type AuthTokens, type AuthUser } from '../api/client'
import { AuthContext } from './auth-context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [isInitializing, setIsInitializing] = useState(true)

  useEffect(() => {
    let active = true
    configureAuthCallbacks({
      onExpired: () => { setAuthTokens(null); setUser(null) },
      onRefreshed: (tokens: AuthTokens, refreshedUser?: AuthUser) => {
        setAuthTokens(tokens)
        if (refreshedUser) setUser(refreshedUser)
        else void authApi.me().then((value) => { if (active) setUser(value) }).catch(() => { setAuthTokens(null); setUser(null) })
      },
    })
    void authApi.refresh().then((result) => {
      if (!active) return
      setAuthTokens({ accessToken: result.accessToken })
      setUser(result.user)
    }).catch(() => {
      if (!active) return
      setAuthTokens(null)
      setUser(null)
    }).finally(() => { if (active) setIsInitializing(false) })
    return () => { active = false }
  }, [])

  const signIn = useCallback(async (email: string, password: string) => {
    const result: AuthResult = await authApi.login(email, password)
    setAuthTokens({ accessToken: result.accessToken })
    setUser(result.user)
  }, [])

  const signOut = useCallback(async () => {
    // Tokens stay in memory; the refresh token is cleared even if server logout fails.
    try {
      await authApi.logout()
    } catch {
      // Clear client state even when the backend cannot be reached.
    } finally {
      setAuthTokens(null)
      setUser(null)
    }
  }, [])

  const value = useMemo(() => ({ user, isAuthenticated: user !== null, isInitializing, signIn, signOut }), [user, isInitializing, signIn, signOut])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

