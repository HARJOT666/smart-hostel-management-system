import { useCallback, useEffect, useState } from 'react'
import api, { errorMessage } from '../api/client'

/**
 * Loads data from a GET endpoint and tracks loading / error state.
 * Re-runs when the url changes; call reload() after a change.
 *
 * If the url changes while a request is still running (e.g. typing in a search box),
 * the answer of the OLD request is ignored, so a slow old response can never
 * overwrite newer data.
 */
export default function useApi(url) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(Boolean(url))
  const [error, setError] = useState(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    if (!url) return undefined
    let current = true            // becomes false when url changes or the page closes
    setLoading(true)
    setError(null)
    api.get(url)
      .then((response) => { if (current) setData(response.data) })
      .catch((e) => { if (current) setError(errorMessage(e)) })
      .finally(() => { if (current) setLoading(false) })
    return () => { current = false }
  }, [url, version])

  const reload = useCallback(() => setVersion((v) => v + 1), [])

  return { data, loading, error, reload, setData }
}
