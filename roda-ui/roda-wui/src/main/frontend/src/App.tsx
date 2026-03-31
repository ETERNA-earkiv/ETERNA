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
import { UserDetail } from './pages/management/UserDetail'
import { GroupDetail } from './pages/management/GroupDetail'
import { IngestPage } from './pages/IngestPage'
import { JobsPage } from './pages/JobsPage'
import { JobDetail } from './pages/JobDetail'
import { AuditLogPage } from './pages/AuditLogPage'
import { StatisticsPage } from './pages/StatisticsPage'
import { SearchPage } from './pages/SearchPage'
import { ProfilePage } from './pages/ProfilePage'
import { PlanningPage } from './pages/planning/PlanningPage'
import { RiskDetail } from './pages/planning/RiskDetail'
import { RepInfoDetail } from './pages/planning/RepInfoDetail'
import { PreservationAgentDetail } from './pages/planning/PreservationAgentDetail'
import { DisposalPage } from './pages/disposal/DisposalPage'
import { DisposalScheduleDetail } from './pages/disposal/DisposalScheduleDetail'
import { DisposalHoldDetail } from './pages/disposal/DisposalHoldDetail'
import { DisposalConfirmationDetail } from './pages/disposal/DisposalConfirmationDetail'
import { DisposalRuleDetail } from './pages/disposal/DisposalRuleDetail'

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
            <Route index element={<Navigate to="/statistics" replace />} />
            <Route path="statistics" element={<StatisticsPage />} />
            <Route path="browse" element={<BrowseAIPs />} />
            <Route path="browse/dip/:dipId" element={<DIPDetail />} />
            <Route path="browse/:aipId" element={<AIPDetail />} />
            <Route path="browse/:aipId/representation/:repId" element={<RepresentationDetail />} />
            <Route path="browse/:aipId/representation/:repId/file/:fileId" element={<FileDetail />} />
            <Route path="ingest" element={<IngestPage />} />
            <Route path="jobs" element={<JobsPage />} />
            <Route path="jobs/:jobId" element={<JobDetail />} />
            <Route path="audit" element={<AuditLogPage />} />
            <Route path="management/users" element={<UserManagement />} />
            <Route path="management/users/:username" element={<UserDetail />} />
            <Route path="management/groups/:groupName" element={<GroupDetail />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="search" element={<SearchPage />} />
            <Route path="planning" element={<PlanningPage />} />
            <Route path="planning/risks/:riskId" element={<RiskDetail />} />
            <Route path="planning/repinfo/:repInfoId" element={<RepInfoDetail />} />
            <Route path="planning/agents/:agentId" element={<PreservationAgentDetail />} />
            <Route path="disposal" element={<DisposalPage />} />
            <Route path="disposal/schedules/:scheduleId" element={<DisposalScheduleDetail />} />
            <Route path="disposal/holds/:holdId" element={<DisposalHoldDetail />} />
            <Route path="disposal/confirmations/:confirmationId" element={<DisposalConfirmationDetail />} />
            <Route path="disposal/rules/:ruleId" element={<DisposalRuleDetail />} />
          </Route>
          <Route path="*" element={<Navigate to="/browse" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
