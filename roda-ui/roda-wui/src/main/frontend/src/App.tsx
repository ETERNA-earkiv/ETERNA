import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './components/AuthProvider'
import { PrivateRoute } from './components/PrivateRoute'
import { AppShell } from './components/AppShell'
import { Login } from './pages/Login'
import { BrowseAIPs } from './pages/BrowseAIPs'
import { AIPDetail } from './pages/AIPDetail'
import { RepresentationDetail } from './pages/browse/RepresentationDetail'
import { FileDetail } from './pages/browse/FileDetail'
import { DIPDetail } from './pages/browse/DIPDetail'
import { UserManagement } from './pages/UserManagement'
import { IngestPage } from './pages/IngestPage'
import { JobsPage } from './pages/JobsPage'
import { AuditLogPage } from './pages/AuditLogPage'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/"
            element={
              <PrivateRoute>
                <AppShell />
              </PrivateRoute>
            }
          >
            <Route index element={<Navigate to="/browse" replace />} />
            <Route path="browse" element={<BrowseAIPs />} />
            <Route path="browse/dip/:dipId" element={<DIPDetail />} />
            <Route path="browse/:aipId" element={<AIPDetail />} />
            <Route path="browse/:aipId/representation/:repId" element={<RepresentationDetail />} />
            <Route path="browse/:aipId/representation/:repId/file/:fileId" element={<FileDetail />} />
            <Route path="ingest" element={<IngestPage />} />
            <Route path="jobs" element={<JobsPage />} />
            <Route path="audit" element={<AuditLogPage />} />
            <Route path="management/users" element={<UserManagement />} />
          </Route>
          <Route path="*" element={<Navigate to="/browse" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
