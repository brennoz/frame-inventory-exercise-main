import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/layout/AppShell'
import { CreateFramePage } from './pages/CreateFramePage'
import { EditFramePage } from './pages/EditFramePage'
import { FrameDetailPage } from './pages/FrameDetailPage'
import { FrameHistoryPage } from './pages/FrameHistoryPage'
import { FrameImportPage } from './pages/FrameImportPage'
import { FrameInventoryPage } from './pages/FrameInventoryPage'
import './App.css'

function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate to="/frames" replace />} />
        <Route path="/frames" element={<FrameInventoryPage />} />
        <Route path="/frames/new" element={<CreateFramePage />} />
        <Route path="/frames/import" element={<FrameImportPage />} />
        <Route path="/frames/:frameId/edit" element={<EditFramePage />} />
        <Route path="/frames/:frameId/history" element={<FrameHistoryPage />} />
        <Route path="/frames/:frameId" element={<FrameDetailPage />} />
        <Route path="*" element={<Navigate to="/frames" replace />} />
      </Route>
    </Routes>
  )
}

export default App
