import { useEffect } from 'react'
import { useStudioStore } from '../store/studioStore'

/**
 * Opens an SSE connection to /api/v1/studio/projects/{projectId}/events
 * and dispatches Zustand actions as events arrive.
 *
 * The connection is opened when projectId is non-null and active is true,
 * and closed (cleanup) when either becomes false or the component unmounts.
 *
 * @param onComplete - Called when gen_complete is received so the parent can
 *   set active back to false, allowing the next generation to reopen the SSE.
 */
export function useGenerationSse(
  projectId: string | null,
  active: boolean,
  onComplete?: () => void,
): void {
  // store is intentionally excluded from the effect deps array: useStudioStore()
  // returns a new object reference on every render, but the action functions
  // inside it are stable Zustand refs. Including store would reconnect the SSE
  // on every state change.
  const store = useStudioStore()

  useEffect(() => {
    if (!projectId || !active) return

    const es = new EventSource(`/api/v1/studio/projects/${projectId}/events`)

    es.addEventListener('phase', (e: MessageEvent) => {
      const data = JSON.parse(e.data)
      store.setPhase(data.phase)
    })

    es.addEventListener('architect_done', (e: MessageEvent) => {
      const data = JSON.parse(e.data)
      store.setPlannedFiles(data.files)
      store.setPhase('coding')
    })

    es.addEventListener('file_chunk', (e: MessageEvent) => {
      const data = JSON.parse(e.data)
      store.appendFileChunk(data.path, data.content)
    })

    es.addEventListener('file_done', (e: MessageEvent) => {
      const data = JSON.parse(e.data)
      store.finalizeFile(data.path)
    })

    // gen_summary arrives before gen_complete and advances phase to 'done'
    es.addEventListener('gen_summary', () => {
      store.setPhase('done')
    })

    // gen_complete is the terminal event — close the connection and notify
    // the parent so it can reset sseActive to false, allowing a subsequent
    // generation to open a fresh EventSource.
    es.addEventListener('gen_complete', () => {
      es.close()
      onComplete?.()
    })

    es.addEventListener('error', (e: MessageEvent) => {
      const data = JSON.parse(e.data)
      store.addMessage({ role: 'assistant', content: `Error: ${data.message}` })
      store.setPhase('error')
      es.close()
      onComplete?.()
    })

    es.onerror = () => {
      // Network-level error (connection dropped, server unavailable).
      // Close to avoid reconnect loop; phase stays as-is for the UI to show.
      es.close()
      onComplete?.()
    }

    return () => {
      es.close()
    }
  }, [projectId, active]) // eslint-disable-line react-hooks/exhaustive-deps
}
