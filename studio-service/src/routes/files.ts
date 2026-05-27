import { FastifyPluginAsync } from 'fastify'
import { readFile, writeFile, listFiles } from '../FileService.js'

export const fileRoutes: FastifyPluginAsync = async (fastify) => {
  // GET /files/:projectId — list all files in workspace
  fastify.get('/:projectId', async (req: any, reply) => {
    const { projectId } = req.params as { projectId: string }
    const files = listFiles(projectId)
    return { files }
  })

  // GET /files/:projectId/* — read file content (returns raw text)
  fastify.get('/:projectId/*', async (req: any, reply) => {
    const { projectId } = req.params as { projectId: string }
    const filePath = req.params['*'] as string
    try {
      const content = readFile(projectId, filePath)
      reply.type('text/plain; charset=utf-8').send(content)
    } catch (err: any) {
      reply.code(404).send({ error: 'File not found', path: filePath })
    }
  })

  // PUT /files/:projectId/* — write file content
  fastify.put('/:projectId/*', async (req: any, reply) => {
    const { projectId } = req.params as { projectId: string }
    const filePath = req.params['*'] as string
    const body = req.body as { content: string } | string
    const content = typeof body === 'string' ? body : body.content
    try {
      writeFile(projectId, filePath, content)
      return { ok: true }
    } catch (err: any) {
      reply.code(400).send({ error: err.message })
    }
  })
}
