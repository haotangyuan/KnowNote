import type { PassThrough } from 'stream'

export interface ContainerEntry {
  containerId: string
  projectId: string
  vitePort: number
  apiPort: number
  status: 'starting' | 'running' | 'stopped'
  logStream?: PassThrough
}

// In-memory registry: projectId → ContainerEntry
export const containers = new Map<string, ContainerEntry>()
