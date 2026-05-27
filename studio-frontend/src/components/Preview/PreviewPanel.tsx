import React, { useEffect } from 'react'
import { useStudioStore } from '../../store/studioStore'
import { useContainerWs } from '../../hooks/useContainerWs'
import { startContainer, getPreviewUrl } from '../../api/studioServiceApi'

export default function PreviewPanel() {
  const { projectId, containerStatus, previewUrl, setContainerStatus } = useStudioStore()
  useContainerWs(projectId)

  // Start container when project is loaded
  useEffect(() => {
    if (!projectId) return
    let cancelled = false

    setContainerStatus('starting')
    startContainer(projectId)
      .then((data: { previewUrl?: string }) => {
        if (!cancelled && data.previewUrl)
          setContainerStatus('running', data.previewUrl)
      })
      .catch(() => { if (!cancelled) setContainerStatus('error') })

    return () => { cancelled = true }
  }, [projectId]) // eslint-disable-line react-hooks/exhaustive-deps -- setContainerStatus is stable Zustand action; startContainer is a module-level import

  const statusColor =
    containerStatus === 'running' ? '#3fb950'
    : containerStatus === 'starting' ? '#ffa657'
    : '#888'

  const iframeSrc = previewUrl ?? (projectId ? getPreviewUrl(projectId) : '')

  return (
    <div style={styles.panel}>
      {/* Address bar */}
      <div style={styles.addressBar}>
        <div style={styles.dots}>
          {(['#da3633', '#e3b341', '#3fb950'] as const).map((c, i) => (
            <div key={i} style={{ width: 8, height: 8, borderRadius: '50%', background: c }} />
          ))}
        </div>
        <div style={styles.url}>{iframeSrc || 'Container starting...'}</div>
        <span style={{ fontSize: 11, color: statusColor }}>
          ● {containerStatus}
        </span>
      </div>

      {/* Preview iframe */}
      <div style={{ flex: 1, overflow: 'hidden' }}>
        {containerStatus === 'running' && iframeSrc ? (
          <>
            {/* allow-same-origin required for previewed app's fetch/WS; combined with
                allow-scripts this is a known same-origin sandbox escape; acceptable for
                developer-controlled local preview content */}
            <iframe
              src={iframeSrc}
              style={styles.iframe}
              title="App Preview"
              sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-modals"
            />
          </>
        ) : (
          <div style={styles.loading}>
            {containerStatus === 'starting' ? '⏳ Starting container...' : '🚀 Preview will appear here'}
          </div>
        )}
      </div>

      {/* Status bar */}
      <div style={styles.statusBar}>
        <span>🗄 studio-{projectId ?? '—'}</span>
        <span style={{ color: containerStatus === 'running' ? '#3fb950' : '#555' }}>
          ● {containerStatus === 'running' ? 'Container running' : 'Waiting...'}
        </span>
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  panel: { display: 'flex', flexDirection: 'column', height: '100%' },
  addressBar: { height: 32, background: '#252526', borderBottom: '1px solid #333', display: 'flex', alignItems: 'center', padding: '0 8px', gap: 8 },
  dots: { display: 'flex', gap: 4 },
  url: { flex: 1, background: '#1e1e1e', borderRadius: 3, padding: '2px 8px', fontSize: 10, color: '#8b949e', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  iframe: { width: '100%', height: '100%', border: 'none', background: '#fff' },
  loading: { display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: '#555', fontSize: 13 },
  statusBar: { padding: '4px 10px', background: '#252526', borderTop: '1px solid #333', fontSize: 10, color: '#8b949e', display: 'flex', justifyContent: 'space-between' },
}
