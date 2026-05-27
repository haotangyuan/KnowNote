import fs from 'fs'
import path from 'path'
import { config } from './config.js'

function resolvePath(projectId: string, filePath: string): string {
  const wsBase = path.resolve(config.workspaceBase)
  const base = path.resolve(path.join(wsBase, projectId))
  if (!base.startsWith(wsBase + path.sep)) {
    throw new Error('Invalid projectId: outside workspace')
  }
  const full = path.resolve(path.join(base, filePath))
  if (!full.startsWith(base + path.sep)) {
    throw new Error('Invalid path: outside workspace')
  }
  return full
}

export function readFile(projectId: string, filePath: string): string {
  return fs.readFileSync(resolvePath(projectId, filePath), 'utf-8')
}

export function writeFile(projectId: string, filePath: string, content: string): void {
  const full = resolvePath(projectId, filePath)
  fs.mkdirSync(path.dirname(full), { recursive: true })
  fs.writeFileSync(full, content, 'utf-8')
}

export function listFiles(projectId: string, dir = ''): string[] {
  const wsBase = path.resolve(config.workspaceBase)
  const base = path.resolve(path.join(wsBase, projectId))
  if (!base.startsWith(wsBase + path.sep)) {
    throw new Error('Invalid projectId: outside workspace')
  }
  const target = dir ? path.resolve(path.join(base, dir)) : base
  if (target !== base && !target.startsWith(base + path.sep)) {
    throw new Error('Invalid dir: outside project')
  }
  if (!fs.existsSync(target)) return []
  const results: string[] = []
  function walk(current: string) {
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      if (entry.name === 'node_modules' || entry.name === '.git') continue
      const full = path.join(current, entry.name)
      if (entry.isDirectory()) walk(full)
      else results.push(path.relative(base, full))
    }
  }
  walk(target)
  return results
}
