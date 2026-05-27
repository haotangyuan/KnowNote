export const config = {
  port: parseInt(process.env.PORT || '3001'),
  workspaceBase: process.env.WORKSPACE_BASE || '/tmp/knownote-studio',
  dockerSocket: process.env.DOCKER_SOCKET || '/var/run/docker.sock',
  sandboxImage: process.env.SANDBOX_IMAGE || 'knownote-studio-sandbox:latest',
  hostName: process.env.HOST_NAME || 'localhost',
}
