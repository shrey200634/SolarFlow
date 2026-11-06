import type { User } from '../types/api'

const KEY = 'solarflow.session'

export interface StoredSession {
  token: string
  expiresAt: number // epoch ms
  user: User
}

function read(): StoredSession | null {
  try {
    const raw = sessionStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as StoredSession) : null
  } catch {
    return null
  }
}

export const sessionStore = {
  get(): StoredSession | null {
    const s = read()
    if (s && s.expiresAt <= Date.now()) {
      this.clear()
      return null
    }
    return s
  },
  getToken(): string | null {
    return this.get()?.token ?? null
  },
  set(s: StoredSession) {
    try {
      sessionStorage.setItem(KEY, JSON.stringify(s))
    } catch {
      /* storage blocked: session lives only until refresh */
    }
  },
  clear() {
    try {
      sessionStorage.removeItem(KEY)
    } catch {
      /* ignore */
    }
  },
}