import { create } from 'zustand'

// ── Types ─────────────────────────────────────────────────────────────────────

export type ContainerStatus = 'idle' | 'starting' | 'running' | 'stopped' | 'error'

export type GenerationPhase =
  | 'idle'
  | 'architect'
  | 'coding'
  | 'done'
  | 'error'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface ProjectFile {
  path: string
  content: string
  /** True once the SSE file_done event has been received for this file */
  finalized: boolean
}

// ── Store Interface ───────────────────────────────────────────────────────────

export interface StudioState {
  // Identity
  projectId: string | null

  // Chat
  messages: ChatMessage[]
  inputDraft: string

  // Generation
  phase: GenerationPhase
  plannedFiles: string[]      // paths returned by architect phase
  generatedFiles: ProjectFile[]

  // Editor
  activeFile: string | null   // currently open file path

  // Container
  containerStatus: ContainerStatus
  previewUrl: string | null

  // Actions
  setProjectId: (id: string) => void

  addMessage: (msg: ChatMessage) => void
  setInputDraft: (draft: string) => void

  setPhase: (phase: GenerationPhase) => void
  setPlannedFiles: (paths: string[]) => void

  /** Append a token chunk to an in-progress file's content */
  appendFileChunk: (path: string, chunk: string) => void
  /** Mark a file as fully received (content already accumulated via appendFileChunk) */
  finalizeFile: (path: string) => void

  setActiveFile: (path: string | null) => void
  /** Update the content of an already-known file (e.g. from Monaco edits) */
  setFileContent: (path: string, content: string) => void
  setContainerStatus: (status: ContainerStatus, previewUrl?: string) => void
  reset: () => void
}

// ── Initial State ─────────────────────────────────────────────────────────────

// Factory function so each reset() gets fresh array instances
const getInitialState = () => ({
  projectId: null as string | null,
  messages: [] as ChatMessage[],
  inputDraft: '',
  phase: 'idle' as GenerationPhase,
  plannedFiles: [] as string[],
  generatedFiles: [] as ProjectFile[],
  activeFile: null as string | null,
  containerStatus: 'idle' as ContainerStatus,
  previewUrl: null as string | null,
})

// ── Store ─────────────────────────────────────────────────────────────────────

export const useStudioStore = create<StudioState>((set) => ({
  ...getInitialState(),

  setProjectId: (id) => set({ projectId: id }),

  addMessage: (msg) =>
    set((state) => ({ messages: [...state.messages, msg] })),

  setInputDraft: (draft) => set({ inputDraft: draft }),

  setPhase: (phase) => set({ phase }),

  setPlannedFiles: (paths) => set({ plannedFiles: paths }),

  appendFileChunk: (path, chunk) =>
    set((state) => {
      const existing = state.generatedFiles.find((f) => f.path === path)
      if (existing) {
        return {
          generatedFiles: state.generatedFiles.map((f) =>
            f.path === path ? { ...f, content: f.content + chunk } : f
          ),
        }
      }
      // First chunk for this file — create the entry with finalized: false
      return {
        generatedFiles: [...state.generatedFiles, { path, content: chunk, finalized: false }],
      }
    }),

  finalizeFile: (path) =>
    set((state) => ({
      // If no activeFile yet, auto-select the first completed file
      activeFile: state.activeFile ?? path,
      generatedFiles: state.generatedFiles.map((f) =>
        f.path === path ? { ...f, finalized: true } : f
      ),
    })),

  setActiveFile: (path) => set({ activeFile: path }),

  setFileContent: (path, content) =>
    set((state) => ({
      generatedFiles: state.generatedFiles.map((f) =>
        f.path === path ? { ...f, content } : f
      ),
    })),

  setContainerStatus: (status, previewUrl) =>
    set({ containerStatus: status, previewUrl: previewUrl ?? null }),

  reset: () => set(getInitialState()),
}))
