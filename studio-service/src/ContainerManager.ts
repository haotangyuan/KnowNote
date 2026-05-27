import Docker from 'dockerode'
import fs from 'fs'
import path from 'path'
import net from 'net'
import { PassThrough } from 'stream'
import { config } from './config.js'
import { containers, ContainerEntry } from './routes/containerRegistry.js'
import { broadcastToProject } from './ws/containerWs.js'

const docker = new Docker({ socketPath: config.dockerSocket })

// In-flight start promises — deduplicates concurrent start calls for the same projectId
const startingPromises = new Map<string, Promise<ContainerEntry>>()

// --- Port allocation ---

const usedPorts = new Set<number>()

function allocatePort(): number {
  // Find a free port in range 32000–33999
  for (let p = 32000; p < 34000; p++) {
    if (!usedPorts.has(p)) {
      usedPorts.add(p)
      return p
    }
  }
  throw new Error('No free ports available')
}

function releasePort(port: number) {
  usedPorts.delete(port)
}

// --- Wait for port to be open ---

function waitForPort(port: number, timeoutMs = 30000): Promise<void> {
  return new Promise((resolve, reject) => {
    const start = Date.now()
    function attempt() {
      const sock = net.createConnection({ port, host: 'localhost' })
      sock.on('connect', () => { sock.destroy(); resolve() })
      sock.on('error', () => {
        sock.destroy()
        if (Date.now() - start > timeoutMs) {
          reject(new Error(`Port ${port} did not open within ${timeoutMs}ms`))
        } else {
          setTimeout(attempt, 500)
        }
      })
    }
    attempt()
  })
}

// --- Start container ---

export function startContainer(
  projectId: string,
  onLog: (line: string) => void
): Promise<ContainerEntry> {
  const inflight = startingPromises.get(projectId)
  if (inflight) return inflight

  const promise = _doStartContainer(projectId, onLog).finally(() => {
    startingPromises.delete(projectId)
  })
  startingPromises.set(projectId, promise)
  return promise
}

async function _doStartContainer(
  projectId: string,
  onLog: (line: string) => void
): Promise<ContainerEntry> {
  // If already running, return existing entry
  const existing = containers.get(projectId)
  if (existing && existing.status === 'running') {
    return existing
  }

  const workspacePath = path.join(config.workspaceBase, projectId)
  fs.mkdirSync(workspacePath, { recursive: true })

  const vitePort = allocatePort()
  const apiPort = allocatePort()

  try {
    broadcastToProject(projectId, { type: 'container:starting', progress: 'Creating container...' })

    // Remove old stopped container if it exists
    try {
      const old = docker.getContainer(`studio-${projectId}`)
      await old.remove({ force: true })
    } catch {
      // ignore — container doesn't exist
    }

    const container = await docker.createContainer({
      Image: config.sandboxImage,
      name: `studio-${projectId}`,
      Env: [
        `PROJECT_ID=${projectId}`,
        `VITE_HMR_HOST=${config.hostName}`,
        `VITE_HMR_CLIENT_PORT=${config.port}`,
      ],
      HostConfig: {
        Binds: [`${workspacePath}:/workspace`],
        PortBindings: {
          '3000/tcp': [{ HostPort: String(vitePort) }],
          '4000/tcp': [{ HostPort: String(apiPort) }],
        },
        Memory: 512 * 1024 * 1024,
        NanoCpus: 500_000_000,
      },
      ExposedPorts: {
        '3000/tcp': {},
        '4000/tcp': {},
      },
    })

    await container.start()

    // Register entry as 'starting'
    const entry: ContainerEntry = {
      containerId: container.id,
      projectId,
      vitePort,
      apiPort,
      status: 'starting',
    }
    containers.set(projectId, entry)

    // Stream logs using dockerode's demuxer to handle multiplexed output
    const logStream = await container.logs({ follow: true, stdout: true, stderr: true, tail: 0 }) as NodeJS.ReadableStream
    const stdoutPass = new PassThrough()
    const stderrPass = new PassThrough()
    ;(docker.modem as any).demuxStream(logStream, stdoutPass, stderrPass)

    for (const stream of [stdoutPass, stderrPass]) {
      stream.on('data', (chunk: Buffer) => {
        const line = chunk.toString('utf8').trim()
        if (line) {
          onLog(line)
          broadcastToProject(projectId, { type: 'container:log', line })
        }
      })
      stream.on('error', (e) => console.error(`[${projectId}] log stream error`, e))
    }

    // Store stdout stream in entry for cleanup on stop
    entry.logStream = stdoutPass

    // Wait for Vite dev server to be ready on vitePort
    broadcastToProject(projectId, { type: 'container:starting', progress: 'Waiting for dev server...' })
    try {
      await waitForPort(vitePort, 60000)
    } catch (err: any) {
      entry.status = 'stopped'
      broadcastToProject(projectId, { type: 'container:error', message: String(err) })
      // Stop and remove the container that never became ready
      try {
        await container.stop({ t: 2 })
        await container.remove()
      } catch {
        // ignore cleanup errors
      }
      throw err  // outer try/catch will release ports
    }

    entry.status = 'running'
    broadcastToProject(projectId, {
      type: 'container:ready',
      previewUrl: `/studio/preview/${projectId}/`,
    })

    return entry
  } catch (err) {
    releasePort(vitePort)
    releasePort(apiPort)
    containers.delete(projectId)
    throw err
  }
}

// --- Stop container ---

export async function stopContainer(projectId: string): Promise<void> {
  const entry = containers.get(projectId)
  if (!entry) return

  // Destroy log stream first
  entry.logStream?.destroy()

  try {
    const container = docker.getContainer(entry.containerId)
    await container.stop({ t: 5 })
    await container.remove()
  } catch {
    // ignore errors during cleanup
  }

  releasePort(entry.vitePort)
  releasePort(entry.apiPort)
  containers.delete(projectId)

  broadcastToProject(projectId, { type: 'container:stopped' })
}

// --- Get status ---

export function getContainerStatus(projectId: string): { status: string; vitePort?: number; apiPort?: number } {
  const entry = containers.get(projectId)
  if (!entry) return { status: 'sleeping' }
  return { status: entry.status, vitePort: entry.vitePort, apiPort: entry.apiPort }
}
