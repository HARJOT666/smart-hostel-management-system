import { createContext, useCallback, useContext, useState } from 'react'

const ToastContext = createContext(() => {})

/** Small success/error messages in the bottom-right corner. */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const notify = useCallback((message, type = 'success') => {
    const id = Date.now() + Math.random()
    setToasts((list) => [...list, { id, message, type }])
    setTimeout(() => setToasts((list) => list.filter((t) => t.id !== id)), 4000)
  }, [])

  return (
    <ToastContext.Provider value={notify}>
      {children}
      <div className="toast-host">
        {toasts.map((t) => (
          <div key={t.id} className={`toast ${t.type === 'error' ? 'toast-error' : ''}`} role="status">
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  return useContext(ToastContext)
}
