import { FastifyPluginAsync } from 'fastify'
import { startContainer, stopContainer, getContainerStatus } from '../ContainerManager.js'

export const containerRoutes: FastifyPluginAsync = async (fastify) => {
  // POST /containers/:projectId/start
  fastify.post('/:projectId/start', async (req: any, reply) => {
    const { projectId } = req.params as { projectId: string }
    try {
      const entry = await startContainer(projectId, (line) => {
        console.log(`[${projectId}] ${line}`)
      })
      reply.send({ status: entry.status, vitePort: entry.vitePort, apiPort: entry.apiPort })
    } catch (err: any) {
      reply.code(500).send({ error: err.message })
    }
  })

  // DELETE /containers/:projectId/stop
  fastify.delete('/:projectId/stop', async (req: any, reply) => {
    const { projectId } = req.params as { projectId: string }
    await stopContainer(projectId)
    reply.send({ status: 'stopped' })
  })

  // GET /containers/:projectId/status
  fastify.get('/:projectId/status', async (req: any, reply) => {
    const { projectId } = req.params as { projectId: string }
    reply.send(getContainerStatus(projectId))
  })
}
