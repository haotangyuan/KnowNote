import { ChatPanel } from './components/ChatPanel'
import EditorPanel from './components/Editor/EditorPanel'
import PreviewPanel from './components/Preview/PreviewPanel'

function App() {
  return (
    <div style={{ display: 'flex', height: '100vh', fontFamily: 'sans-serif' }}>
      <div style={{ width: 320, borderRight: '1px solid #ccc', display: 'flex', flexDirection: 'column' }}>
        <ChatPanel />
      </div>
      <div style={{ flex: 1, borderRight: '1px solid #ccc', display: 'flex', flexDirection: 'column' }}>
        <EditorPanel />
      </div>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <PreviewPanel />
      </div>
    </div>
  )
}

export default App
