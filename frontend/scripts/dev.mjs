import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const frontend = fileURLToPath(new URL('../', import.meta.url))
const root = fileURLToPath(new URL('../../', import.meta.url))
const children = []
let stopping = false

function stop(code) {
  if (stopping) return
  stopping = true
  process.exitCode = code

  for (const child of children) {
    if (!child.pid) continue
    try {
      // Stop the whole server process group, including Maven's Java child.
      process.kill(-child.pid, 'SIGTERM')
    } catch (error) {
      if (error.code !== 'ESRCH') console.error(error.message)
    }
  }
}

function start(name, command, args, cwd) {
  const child = spawn(command, args, {
    cwd,
    stdio: 'inherit',
    detached: true,
  })
  children.push(child)
  child.on('error', error => {
    console.error(`${name} failed to start: ${error.message}`)
    stop(1)
  })
  child.on('exit', (code, signal) => {
    if (stopping) return
    console.error(`${name} stopped (${signal ?? code}). Stopping both servers.`)
    stop(code || 1)
  })
}

process.on('SIGINT', () => stop(130))
process.on('SIGTERM', () => stop(143))

start('Backend', './mvnw', ['spring-boot:run'], root)
start('Frontend', process.execPath, [
  'node_modules/vite/bin/vite.js',
  '--host', '127.0.0.1',
  '--strictPort',
  ...process.argv.slice(2),
], frontend)
