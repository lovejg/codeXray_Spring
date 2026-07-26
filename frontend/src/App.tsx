import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useBootstrapAuth } from './hooks/useBootstrapAuth'
import Spinner from './components/common/Spinner'
import Layout from './components/layout/Layout'
import ProtectedRoute from './components/common/ProtectedRoute'

import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import VerifyEmailPage from './pages/VerifyEmailPage'
import OAuthCallbackPage from './pages/OAuthCallbackPage'
import ProblemsPage from './pages/ProblemsPage'
import ProblemDetailPage from './pages/ProblemDetailPage'
import SolutionsPage from './pages/SolutionsPage'
import SolutionDetailPage from './pages/SolutionDetailPage'
import SolutionFormPage from './pages/SolutionFormPage'
import NotesPage from './pages/NotesPage'
import NoteDetailPage from './pages/NoteDetailPage'
import CommunityPage from './pages/CommunityPage'
import CommunityPostPage from './pages/CommunityPostPage'
import CommunityFormPage from './pages/CommunityFormPage'
import SuggestionsPage from './pages/SuggestionsPage'
import SuggestionFormPage from './pages/SuggestionFormPage'
import SuggestionPostPage from './pages/SuggestionPostPage'
import ProfilePage from './pages/ProfilePage'
import NotificationsPage from './pages/NotificationsPage'
import AdminReportsPage from './pages/AdminReportsPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 1000 * 60 },
  },
})

function AppRoutes() {
  const ready = useBootstrapAuth()
  if (!ready) return <Spinner label="불러오는 중…" />

  return (
    <Routes>
      {/* 공개(비로그인) */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/oauth/callback/:provider" element={<OAuthCallbackPage />} />

      {/* 레이아웃 아래: 문제 조회는 공개, 나머지는 보호 */}
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/problems" replace />} />
        <Route path="/problems" element={<ProblemsPage />} />
        <Route path="/problems/:id" element={<ProblemDetailPage />} />
        <Route path="/community" element={<CommunityPage />} />
        <Route path="/community/new" element={<ProtectedRoute><CommunityFormPage /></ProtectedRoute>} />
        <Route path="/community/:id" element={<CommunityPostPage />} />
        <Route path="/community/:id/edit" element={<ProtectedRoute><CommunityFormPage /></ProtectedRoute>} />
        <Route path="/suggestions" element={<SuggestionsPage />} />
        <Route path="/suggestions/new" element={<ProtectedRoute><SuggestionFormPage /></ProtectedRoute>} />
        <Route path="/suggestions/:id" element={<SuggestionPostPage />} />

        <Route path="/solutions" element={<ProtectedRoute><SolutionsPage /></ProtectedRoute>} />
        <Route path="/solutions/new" element={<ProtectedRoute><SolutionFormPage /></ProtectedRoute>} />
        <Route path="/solutions/:id" element={<ProtectedRoute><SolutionDetailPage /></ProtectedRoute>} />
        <Route path="/solutions/:id/edit" element={<ProtectedRoute><SolutionFormPage /></ProtectedRoute>} />
        <Route path="/notes" element={<ProtectedRoute><NotesPage /></ProtectedRoute>} />
        <Route path="/notes/:id" element={<ProtectedRoute><NoteDetailPage /></ProtectedRoute>} />
        <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
        <Route path="/notifications" element={<ProtectedRoute><NotificationsPage /></ProtectedRoute>} />
        <Route path="/admin/reports" element={<ProtectedRoute><AdminReportsPage /></ProtectedRoute>} />
      </Route>

      <Route path="*" element={<Navigate to="/problems" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </QueryClientProvider>
  )
}
