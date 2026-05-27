import { useState } from 'react'
import { useStudioStore } from '../store/studioStore'
import { useGenerationSse } from '../hooks/useGenerationSse'

const PHASE_LABEL: Record<string, string> = {
  idle: '',
  architect: '🏗 Planning files…',
  coding: '⌨️ Generating code…',
  done: '✅ Generation complete',
  error: '❌ Generation failed',
}

export function ChatPanel() {
  const {
    projectId,
    messages,
    inputDraft,
    phase,
    setInputDraft,
    addMessage,
    setProjectId,
    reset,
  } = useStudioStore()

  const [sseActive, setSseActive] = useState(false)

  // Attach SSE listener whenever we have a projectId and generation is running.
  // onComplete resets sseActive to false so the next handleSend can flip it
  // true again, triggering a fresh EventSource for the new generation.
  useGenerationSse(projectId, sseActive, () => setSseActive(false))

  async function handleSend() {
    const text = inputDraft.trim()
    if (!text) return

    setInputDraft('')
    addMessage({ role: 'user', content: text })

    // Create a project if none exists yet
    let pid = projectId
    if (!pid) {
      const res = await fetch('/api/v1/studio/projects', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: 'My Project', description: text }),
      })
      const project = await res.json()
      pid = String(project.id)
      setProjectId(pid)
    }

    // Start generation — this returns an SSE stream but we use the /events endpoint instead
    await fetch(`/api/v1/studio/projects/${pid}/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text }),
    })

    setSseActive(true)
  }

  function handleReset() {
    setSseActive(false)
    reset()
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Message list */}
      <div style={{ flex: 1, overflowY: 'auto', padding: 12 }}>
        {messages.map((msg, i) => (
          <div
            key={i}
            style={{
              marginBottom: 8,
              textAlign: msg.role === 'user' ? 'right' : 'left',
            }}
          >
            <span
              style={{
                display: 'inline-block',
                padding: '6px 10px',
                borderRadius: 8,
                background: msg.role === 'user' ? '#0070f3' : '#f0f0f0',
                color: msg.role === 'user' ? '#fff' : '#000',
                maxWidth: '85%',
                wordBreak: 'break-word',
              }}
            >
              {msg.content}
            </span>
          </div>
        ))}
        {phase !== 'idle' && (
          <div style={{ color: '#666', fontSize: 13, marginTop: 4 }}>
            {PHASE_LABEL[phase] ?? phase}
          </div>
        )}
      </div>

      {/* Input area */}
      <div style={{ borderTop: '1px solid #ccc', padding: 8, display: 'flex', gap: 6 }}>
        <textarea
          value={inputDraft}
          onChange={(e) => setInputDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              handleSend()
            }
          }}
          placeholder="Describe what you want to build…"
          rows={3}
          style={{ flex: 1, resize: 'none', padding: 6, borderRadius: 4, border: '1px solid #ccc' }}
          disabled={phase === 'architect' || phase === 'coding'}
        />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <button
            onClick={handleSend}
            disabled={phase === 'architect' || phase === 'coding' || !inputDraft.trim()}
            style={{ padding: '6px 12px', cursor: 'pointer' }}
          >
            Send
          </button>
          <button onClick={handleReset} style={{ padding: '6px 12px', cursor: 'pointer' }}>
            Reset
          </button>
        </div>
      </div>
    </div>
  )
}
