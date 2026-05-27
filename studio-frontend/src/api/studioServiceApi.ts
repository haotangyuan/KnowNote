const BASE = '/studio' // proxied to :3001 via Vite

export async function listFiles(projectId: string): Promise<string[]> {
  const res = await fetch(`${BASE}/files/${projectId}`)
  const data = await res.json()
  return data.files ?? []
}

export async function readFile(projectId: string, path: string): Promise<string> {
  const res = await fetch(`${BASE}/files/${projectId}/${path}`)
  if (!res.ok) throw new Error(`readFile ${path}: HTTP ${res.status}`)
  return res.text()
}

export async function writeFile(projectId: string, path: string, content: string): Promise<void> {
  const res = await fetch(`${BASE}/files/${projectId}/${path}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'text/plain' },
    body: content,
  })
  if (!res.ok) throw new Error(`writeFile ${path}: HTTP ${res.status}`)
}

export function getPreviewUrl(projectId: string): string {
  return `/studio/preview/${projectId}/`
}

export async function startContainer(projectId: string) {
  return fetch(`${BASE}/containers/${projectId}/start`, { method: 'POST' }).then((r) => r.json())
}

export async function getContainerStatus(projectId: string) {
  return fetch(`${BASE}/containers/${projectId}/status`).then((r) => r.json())
}
