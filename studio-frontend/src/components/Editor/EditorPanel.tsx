import React from 'react'
import { useStudioStore } from '../../store/studioStore'
import FileTree from './FileTree'
import MonacoEditor from './MonacoEditor'

export default function EditorPanel() {
  const { generatedFiles, activeFile, setActiveFile } = useStudioStore()
  // Treat all generated files as open tabs (same as VS Code auto-open on generation)
  const openTabs = generatedFiles.map((f) => f.path).sort()

  return (
    <div style={styles.panel}>
      {/* Tab bar */}
      <div style={styles.tabBar}>
        {openTabs.map((path) => (
          <div
            key={path}
            style={{ ...styles.tab, ...(activeFile === path ? styles.activeTab : {}) }}
            onClick={() => setActiveFile(path)}
          >
            {path.split('/').pop()}
          </div>
        ))}
      </div>

      {/* Body: file tree + editor */}
      <div style={styles.body}>
        <div style={styles.fileTree}>
          <FileTree />
        </div>
        <div style={styles.editor}>
          {activeFile ? (
            <MonacoEditor
              path={activeFile}
              content={generatedFiles.find((f) => f.path === activeFile)?.content ?? null}
            />
          ) : (
            <div style={styles.placeholder}>
              Select a file from the explorer, or generate code from the chat.
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  panel: { display: 'flex', flexDirection: 'column', height: '100%', background: '#1e1e1e' },
  tabBar: {
    display: 'flex',
    alignItems: 'center',
    background: '#252526',
    borderBottom: '1px solid #333',
    height: 32,
    padding: '0 6px',
    gap: 1,
    overflowX: 'auto',
  },
  tab: {
    padding: '0 12px',
    height: '100%',
    display: 'flex',
    alignItems: 'center',
    fontSize: 11,
    color: '#666',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  },
  activeTab: { background: '#1e1e1e', color: '#9cdcfe', borderTop: '1px solid #7eb8f7' },
  body: { display: 'flex', flex: 1, overflow: 'hidden' },
  fileTree: { width: '38%', borderRight: '1px solid #333', overflow: 'hidden' },
  editor: { flex: 1, overflow: 'hidden' },
  placeholder: { padding: 24, fontSize: 12, color: '#555', fontStyle: 'italic' },
}
