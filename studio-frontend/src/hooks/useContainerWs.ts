import { useEffect } from 'react'
import { ContainerStatus, useStudioStore } from '../store/studioStore'

const VALID_STATUSES = new Set<ContainerStatus>([
  'idle', 'starting', 'running', 'stopped', 'error',
])

export function useContainerWs(projectId: string | null) {
  const setContainerStatus = useStudioStore((s) => s.setContainerStatus)

  useEffect(() => {
    if (!projectId) return

    let ws: WebSocket | null = null
    let closed = false
    let retryTimer: ReturnType<typeof setTimeout> | null = null

    function connect() {
      if (closed) return
      ws = new WebSocket(`ws://${location.host}/studio-ws/${projectId}`)

      ws.onopen = () => setContainerStatus('starting')

      ws.onmessage = (event) => {
        if (closed) return
        try {
          const msg = JSON.parse(event.data) as {
            type: string
            status?: string
            previewUrl?: string
            line?: string
          }
          switch (msg.type) {
            case 'container:status':
              if (msg.status && VALID_STATUSES.has(msg.status as ContainerStatus))
                setContainerStatus(msg.status as ContainerStatus)
              break
            case 'container:ready':
              setContainerStatus('running', msg.previewUrl)
              break
            case 'container:log':
              console.debug(`[container] ${msg.line}`)
              break
            case 'container:error':
              setContainerStatus('error')
              break
          }
        } catch { /* ignore non-JSON */ }
      }

      ws.onclose = (evt) => {
        if (closed) return
        setContainerStatus('stopped')
        // Reconnect after 3 s if closed unexpectedly (code 1000 = intentional close)
        if (!evt.wasClean) {
          retryTimer = setTimeout(connect, 3000)
        }
      }
    }

    connect()

    return () => {
      closed = true
      if (retryTimer) clearTimeout(retryTimer)
      ws?.close()
    }
  }, [projectId, setContainerStatus])
}
