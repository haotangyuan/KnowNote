import express from 'express'
import cors from 'cors'
import { Pool } from 'pg'

const app = express()
app.use(cors())
app.use(express.json())

const pool = process.env.DATABASE_URL
  ? new Pool({ connectionString: process.env.DATABASE_URL })
  : null

app.get('/api/health', (_req, res) => {
  res.json({ ok: true, timestamp: new Date().toISOString() })
})

app.get('/api/db/status', async (_req, res) => {
  if (!pool) {
    return res.json({ connected: false, message: 'No DATABASE_URL configured' })
  }
  try {
    await pool.query('SELECT 1')
    res.json({ connected: true })
  } catch (err: any) {
    res.json({ connected: false, message: err.message })
  }
})

const port = process.env.API_PORT || 4000
app.listen(port, () => {
  console.log(`API server running on port ${port}`)
})
