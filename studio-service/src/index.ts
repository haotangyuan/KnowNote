import Fastify from 'fastify'
import cors from '@fastify/cors'
import websocket from '@fastify/websocket'
import { config } from './config.js'
import { containerRoutes } from './routes/containers.js'
import { fileRoutes } from './routes/files.js'
import { containerWsRoutes } from './ws/containerWs.js'
import { registerWsUpgrade } from './routes/proxy.js'
import { previewProxyRoutes } from './routes/previewProxy.js'

const fastify = Fastify({ logger: true })

await fastify.register(cors, { origin: '*' })
await fastify.register(websocket)

fastify.register(containerRoutes, { prefix: '/containers' })
fastify.register(fileRoutes, { prefix: '/files' })
fastify.register(containerWsRoutes, { prefix: '/ws' })
fastify.register(previewProxyRoutes, { prefix: '/preview' })

fastify.get('/health', async () => ({ ok: true }))

try {
  await fastify.listen({ port: config.port, host: '0.0.0.0' })
} catch (err) {
  fastify.log.error(err)
  process.exit(1)
}

registerWsUpgrade(fastify.server)

console.log(`Studio service running on port ${config.port}`)
