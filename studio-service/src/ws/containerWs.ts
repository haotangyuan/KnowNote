import { FastifyPluginAsync } from 'fastify'
import { containers } from '../routes/containerRegistry.js'

const clients = new Map<string, Set<any>>()

export function broadcastToProject(projectId: string, data: object) {
  const sockets = clients.get(projectId)
  if (!sockets) return
  const payload = JSON.stringify(data)
  for (const socket of sockets) {
    try { socket.send(payload) } catch { sockets.delete(socket) }
  }
}

export const containerWsRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.get('/:projectId', { websocket: true }, async (socket: any, req: any) => {
    const { projectId } = req.params as { projectId: string }
    if (!clients.has(projectId)) clients.set(projectId, new Set())
    clients.get(projectId)!.add(socket)

    // Register cleanup BEFORE sending anything, so a send failure doesn't leak the socket
    socket.on('close', () => {
      const sockets = clients.get(projectId)
      if (!sockets) return
      sockets.delete(socket)
      if (sockets.size === 0) clients.delete(projectId)
    })

    // Send current container status immediately on connect
    const entry = containers.get(projectId)
    try {
      socket.send(JSON.stringify({
        type: 'container:status',
        status: entry?.status ?? 'sleeping',
        projectId,
      }))
    } catch { /* client already disconnected; close handler will clean up */ }
  })
}
