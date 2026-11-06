import axios from 'axios'
import { sessionStore } from '../utils/session'
import type { ApiError } from '../types/api'

export const SESSION_EXPIRED_EVENT = 'solarflow:session-expired'

const base = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081'

export const api = axios.create({
  baseURL: `${base}/api/v1`,
  timeout: 30_000,
})

// Attach the JWT. Never set Content-Type here: Axios adds the multipart boundary itself.
api.interceptors.request.use((config) => {
  const token = sessionStore.getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// A 401 while we hold a token means the session expired (JWT lives 15 min, no refresh).
api.interceptors.response.use(
  (res) => res,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401 && sessionStore.getToken()) {
      sessionStore.clear()
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
    }
    return Promise.reject(error)
  },
)

export interface ParsedError {
  status?: number
  message: string
  fieldErrors: Record<string, string>
}

/** 401/403 bodies are empty and some Spring errors are not ApiError, so be defensive. */
export function parseError(err: unknown): ParsedError {
  if (axios.isAxiosError(err)) {
    if (!err.response) {
      return {
        message: "Can't reach the server. Check your connection and try again.",
        fieldErrors: {},
      }
    }
    const { status, data } = err.response
    const body: Partial<ApiError> = data && typeof data === 'object' ? data : {}
    let message = typeof body.message === 'string' ? body.message.trim() : ''
    if (!message) {
      if (status === 401) message = 'Your session has expired. Sign in again.'
      else if (status === 403) message = "You don't have permission to do this."
      else if (status === 404) message = 'Not found.'
      else if (status >= 500) message = 'Something went wrong on our side. Try again.'
      else message = 'The request could not be completed.'
    }
    return { status, message, fieldErrors: body.fieldErrors ?? {} }
  }
  return { message: 'Something went wrong. Try again.', fieldErrors: {} }
}