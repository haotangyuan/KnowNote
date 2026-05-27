import React from 'react'
import { useStudioStore } from '../../store/studioStore'

export default function FileTree() {
  const { generatedFiles, activeFile, setActiveFile } = useStudioStore()
  const paths = generatedFiles.map((f) => f.path).sort()

  if (paths.length === 0) {
    return <div style={styles.empty}>No files yet</div>
  }

  return (
    <div style={styles.tree}>
      <div style={styles.heading}>EXPLORER</div>
      {paths.map((path) => (
        <div
          key={path}
          style={{ ...styles.item, ...(activeFile === path ? styles.activeItem : {}) }}
          onClick={() => setActiveFile(path)}
          title={path}
        >
          {fileIcon(path)} {path}
        </div>
      ))}
    </div>
  )
}

function fileIcon(path: string): string {
  if (path.endsWith('.tsx') || path.endsWith('.jsx')) return '⚛'
  if (path.endsWith('.ts') || path.endsWith('.js')) return '📄'
  if (path.endsWith('.css')) return '🎨'
  if (path.endsWith('.sql')) return '🗄'
  if (path.endsWith('.json')) return '{}'
  return '📄'
}

const styles: Record<string, React.CSSProperties> = {
  tree: { padding: 6, overflowY: 'auto', height: '100%', background: '#252526' },
  heading: { fontSize: 9, color: '#888', marginBottom: 4, paddingLeft: 4, letterSpacing: 1 },
  item: {
    fontSize: 11,
    color: '#9cdcfe',
    padding: '2px 6px',
    cursor: 'pointer',
    borderRadius: 2,
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
  activeItem: { background: '#094771' },
  empty: { padding: 12, fontSize: 11, color: '#555', fontStyle: 'italic' },
}
