import { FastifyPluginAsync } from 'fastify'
import httpProxy from 'http-proxy'
import { containers } from './containerRegistry.js'

const proxy = httpProxy.createProxyServer({})

proxy.on('error', (err: Error, _req: any, res: any) => {
  console.error('[preview proxy] error:', err.message)
  if (res && typeof res.end === 'function') res.end()
})

export const previewProxyRoutes: FastifyPluginAsync = async (fastify) => {
  // GET /preview/:projectId/* — proxy to container's Vite dev server
  fastify.get('/:projectId/*', async (req: any, reply) => {
    const { projectId } = req.params as { projectId: string }
    const entry = containers.get(projectId)

    if (!entry || entry.status !== 'running') {
      return reply.code(503).send({ error: 'Container not running' })
    }

    // Hijack socket control — http-proxy's success callback is never called,
    // so we must hand off the raw socket to http-proxy and let it handle the response.
    reply.hijack()
    proxy.web(req.raw, reply.raw, {
      target: `http://localhost:${entry.vitePort}`,
      changeOrigin: true,
    })
  })
}
