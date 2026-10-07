import axios from 'axios'

// Base URL of the Spring Boot API (see frontend/.env.example)
export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const api = axios.create({ baseURL: API_URL + '/api' })

// Attach the JWT to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('shms_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// If the token is missing/expired the backend answers 401 -> go back to the login page
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginCall = error.config?.url?.includes('/auth/login')
    if (error.response?.status === 401 && !isLoginCall) {
      localStorage.removeItem('shms_token')
      localStorage.removeItem('shms_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

/** Human-readable message from an axios error (uses the backend's ApiError.message). */
export function errorMessage(error) {
  if (error?.response?.data?.message) return error.response.data.message
  if (error?.response) return `Request failed (HTTP ${error.response.status})`
  return 'Cannot reach the server. Is the backend running?'
}

/** Full URL for an uploaded image path returned by the API (e.g. /uploads/complaints/x.jpg). */
export function fileUrl(path) {
  return path ? API_URL + path : null
}

export default api
