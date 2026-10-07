import { createContext, useContext, useState } from 'react'
import api from '../api/client'

const AuthContext = createContext(null)

/** Keeps the logged-in user (and JWT) in React state and in localStorage. */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('shms_user')
    return saved ? JSON.parse(saved) : null
  })

  async function login(username, password) {
    const { data } = await api.post('/auth/login', { username, password })
    const loggedIn = { id: data.userId, username: data.username, name: data.name, role: data.role }
    localStorage.setItem('shms_token', data.token)
    localStorage.setItem('shms_user', JSON.stringify(loggedIn))
    setUser(loggedIn)
    return loggedIn
  }

  function logout() {
    localStorage.removeItem('shms_token')
    localStorage.removeItem('shms_user')
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}

/** Where each role lands after login */
export function homeFor(role) {
  if (role === 'STUDENT') return '/student'
  if (role === 'MAINTENANCE') return '/maintenance'
  return '/admin'
}
