import httpProxy from 'http-proxy'
import { containers } from './containerRegistry.js'

const proxy = httpProxy.createProxyServer({ ws: true })

proxy.on('error', (err: Error, _req: any, res: any) => {
  console.error('[proxy] WebSocket proxy error:', err.message)
  if (res && typeof res.end === 'function') res.end()
})

export function registerWsUpgrade(server: any) {
  server.on('upgrade', (req: any, socket: any, head: any) => {
    const match = req.url?.match(/^\/hmr\/([^/]+)/)
    if (!match) return socket.destroy()

    const projectId = match[1]
    const entry = containers.get(projectId)
    if (!entry || entry.status !== 'running') {
      return socket.destroy()
    }

    proxy.ws(req, socket, head, {
      target: `ws://localhost:${entry.vitePort}`,
    })
  })
}
