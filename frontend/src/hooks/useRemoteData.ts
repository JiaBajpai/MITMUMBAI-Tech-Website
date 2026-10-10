import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError } from '../api/client'

export interface RemoteState<T> { data: T | null; loading: boolean; error: Error | null; reload: () => void }

export function useRemoteData<T>(key: string, load: (signal: AbortSignal) => Promise<T>): RemoteState<T> {
  const loadRef = useRef(load)
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)
  const [version, setVersion] = useState(0)
  useEffect(() => { loadRef.current = load }, [load])
  useEffect(() => {
    const controller = new AbortController()
    // eslint-disable-next-line react-hooks/set-state-in-effect -- reset request state when its key changes
    setLoading(true)
    setError(null)
    loadRef.current(controller.signal).then((result) => {
      if (!controller.signal.aborted) setData(result)
    }).catch((cause: unknown) => {
      if (!controller.signal.aborted) setError(cause instanceof Error ? cause : new Error('Unable to load this information.'))
    }).finally(() => {
      if (!controller.signal.aborted) setLoading(false)
    })
    return () => controller.abort()
  }, [key, version])
  const reload = useCallback(() => setVersion((current) => current + 1), [])
  return { data, loading, error, reload }
}

export function readableError(error: Error) {
  if (error instanceof ApiError) {
    if (error.status === 403) return 'Your account does not have access to this information or action.'
    if (error.status === 404) return 'This item is no longer available.'
    if (error.status === 502) return error.message
    if (error.status >= 500) return 'The service could not complete the request. Try again in a moment.'
    return error.message
  }
  return error.message || 'Unable to reach the service. Check your connection and try again.'
}
