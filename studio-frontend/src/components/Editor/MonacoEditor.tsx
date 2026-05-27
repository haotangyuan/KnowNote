import { useCallback, useEffect, useRef } from 'react'
import Editor from '@monaco-editor/react'
import { useStudioStore } from '../../store/studioStore'
import { writeFile } from '../../api/studioServiceApi'

interface Props {
  path: string
  content: string | null
}

export default function MonacoEditor({ path, content }: Props) {
  const { projectId, setFileContent } = useStudioStore()

  const saveTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const handleChange = useCallback(
    (value: string | undefined) => {
      if (value === undefined) return
      setFileContent(path, value)
      if (projectId) {
        if (saveTimer.current) clearTimeout(saveTimer.current)
        saveTimer.current = setTimeout(() => {
          writeFile(projectId, path, value).catch(console.warn)
        }, 500)
      }
    },
    [path, projectId, setFileContent],
  )

  useEffect(() => {
    return () => {
      if (saveTimer.current) clearTimeout(saveTimer.current)
    }
  }, [path])

  return (
    <Editor
      height="100%"
      theme="vs-dark"
      path={path}
      value={content ?? '// Loading...'}
      language={langFromPath(path)}
      onChange={handleChange}
      options={{
        minimap: { enabled: false },
        fontSize: 13,
        lineNumbers: 'on',
        wordWrap: 'on',
        scrollBeyondLastLine: false,
        automaticLayout: true,
      }}
    />
  )
}

function langFromPath(path: string): string {
  const ext = path.split('.').pop()?.toLowerCase() ?? ''
  const map: Record<string, string> = {
    tsx: 'typescript',
    ts: 'typescript',
    jsx: 'javascript',
    js: 'javascript',
    css: 'css',
    html: 'html',
    json: 'json',
    md: 'markdown',
    sql: 'sql',
  }
  return map[ext] ?? 'plaintext'
}
