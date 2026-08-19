import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/layout/AppShell'
import { FrameDetailPage } from './pages/FrameDetailPage'
import { FrameInventoryPage } from './pages/FrameInventoryPage'
import './App.css'

function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate to="/frames" replace />} />
        <Route path="/frames" element={<FrameInventoryPage />} />
        <Route path="/frames/:frameId" element={<FrameDetailPage />} />
        <Route path="*" element={<Navigate to="/frames" replace />} />
      </Route>
    </Routes>
  )
}

export default App
