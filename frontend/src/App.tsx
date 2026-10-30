import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useBootstrapAuth } from './hooks/useBootstrapAuth'
import Spinner from './components/common/Spinner'
import Layout from './components/layout/Layout'
import ProtectedRoute from './components/common/ProtectedRoute'

// 처음 들어오는 곳이 대부분 메인이라 메인만 같이 싣는다. 나머지 화면은 들어갈 때 받는다.
// 코드 편집기(CodeMirror), 코드 강조(highlight.js), 마크다운이 그 화면들에만 있어 첫 화면 JS 가 크게 준다
import LandingPage from './pages/LandingPage'
const LoginPage = lazy(() => import('./pages/LoginPage'))
const RegisterPage = lazy(() => import('./pages/RegisterPage'))
const VerifyEmailPage = lazy(() => import('./pages/VerifyEmailPage'))
const OAuthCallbackPage = lazy(() => import('./pages/OAuthCallbackPage'))
const ProblemsPage = lazy(() => import('./pages/ProblemsPage'))
const ProblemDetailPage = lazy(() => import('./pages/ProblemDetailPage'))
const SolutionsPage = lazy(() => import('./pages/SolutionsPage'))
const SolutionDetailPage = lazy(() => import('./pages/SolutionDetailPage'))
const SolutionFormPage = lazy(() => import('./pages/SolutionFormPage'))
const NotesPage = lazy(() => import('./pages/NotesPage'))
const NoteDetailPage = lazy(() => import('./pages/NoteDetailPage'))
const CommunityPage = lazy(() => import('./pages/CommunityPage'))
const CommunityPostPage = lazy(() => import('./pages/CommunityPostPage'))
const CommunityFormPage = lazy(() => import('./pages/CommunityFormPage'))
const SuggestionsPage = lazy(() => import('./pages/SuggestionsPage'))
const SuggestionFormPage = lazy(() => import('./pages/SuggestionFormPage'))
const SuggestionPostPage = lazy(() => import('./pages/SuggestionPostPage'))
const ProfilePage = lazy(() => import('./pages/ProfilePage'))
const NotificationsPage = lazy(() => import('./pages/NotificationsPage'))
const AdminReportsPage = lazy(() => import('./pages/AdminReportsPage'))
const PrivacyPage = lazy(() => import('./pages/PrivacyPage'))

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 1000 * 60 },
  },
})

function AppRoutes() {
  const ready = useBootstrapAuth()
  if (!ready) return <Spinner label="불러오는 중…" />

  // 레이아웃 밖 화면(로그인, 가입 등)이 받는 동안 보일 자리. 레이아웃 안 화면은 Layout 이 따로 잡는다
  return (
    <Suspense fallback={<Spinner />}>
      <Routes>
        {/* 메인 랜딩: 로그인 여부와 무관하게 누구나 접근 (로고 → 메인) */}
        <Route path="/" element={<LandingPage />} />

        {/* 공개(비로그인) */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/oauth/callback/:provider" element={<OAuthCallbackPage />} />

        {/* 레이아웃 아래: 문제 조회는 공개, 나머지는 보호 */}
        <Route element={<Layout />}>
          <Route path="/problems" element={<ProblemsPage />} />
          <Route path="/problems/:id" element={<ProblemDetailPage />} />
          <Route path="/community" element={<CommunityPage />} />
          <Route path="/community/new" element={<ProtectedRoute><CommunityFormPage /></ProtectedRoute>} />
          <Route path="/community/:id" element={<CommunityPostPage />} />
          <Route path="/community/:id/edit" element={<ProtectedRoute><CommunityFormPage /></ProtectedRoute>} />
          <Route path="/suggestions" element={<SuggestionsPage />} />
          <Route path="/suggestions/new" element={<ProtectedRoute><SuggestionFormPage /></ProtectedRoute>} />
          <Route path="/suggestions/:id" element={<SuggestionPostPage />} />
          <Route path="/privacy" element={<PrivacyPage />} />

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
    </Suspense>
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
