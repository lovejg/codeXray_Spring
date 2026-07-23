import type { Tier } from '../lib/tier'

// ─── 공통 페이지 응답 (백엔드 PageResponse) ───
export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  total: number
  totalPages: number
}

// ─── 문제 ───
export type ProblemSource =
  | 'PRACTICE' | 'KAKAO_BLIND' | 'KAKAO_INTERNSHIP' | 'KAKAO_CODE'
  | 'MONTHLY_CHALLENGE' | 'WEEKLY_CHALLENGE' | 'SUMMER_WINTER'
  | 'PCCE' | 'PCCP' | 'SQL' | 'OTHER'

export const SOURCE_LABEL: Record<ProblemSource, string> = {
  PRACTICE: '연습문제',
  KAKAO_BLIND: '카카오 공채',
  KAKAO_INTERNSHIP: '카카오 인턴',
  KAKAO_CODE: '카카오코드',
  MONTHLY_CHALLENGE: '월간 챌린지',
  WEEKLY_CHALLENGE: '위클리 챌린지',
  SUMMER_WINTER: 'Summer/Winter',
  PCCE: 'PCCE',
  PCCP: 'PCCP',
  SQL: 'SQL',
  OTHER: '기타',
}

export const LEVEL_COLOR: Record<number, string> = {
  0: '#9ca3af',
  1: '#10b981',
  2: '#3b82f6',
  3: '#f59e0b',
  4: '#ef4444',
  5: '#7c3aed',
}

export interface Tag {
  id: number
  name: string
}

export interface Problem {
  id: number
  title: string
  source: ProblemSource
  level: number
  acceptanceRate?: number | null
  adjustedLevel?: number | null
  tier?: Tier | null
  link: string
  tags: Tag[]
}

// ─── 풀이 / 메모 ───
export interface Memo {
  wrongReason?: string | null
  logic?: string | null
  keyFunctions?: string | null
  freeNote?: string | null
}

export interface Solution {
  id: number
  problem: Problem
  code: string
  language: string
  starred: boolean
  createdAt: string
  updatedAt: string
  memo?: Memo | null
}

// ─── 레이팅(난이도 피드백) ───
export interface Feedback {
  problemId: number
  level: number
}

// ─── 노트 ───
export type NoteType = 'CODE' | 'PATTERN' | 'MISTAKE' | 'OTHER'

export const NOTE_TYPE_LABEL: Record<NoteType, string> = {
  CODE: '코드',
  PATTERN: '접근 패턴',
  MISTAKE: '오답 노트',
  OTHER: '기타',
}

export const NOTE_TYPE_COLOR: Record<NoteType, { bg: string; text: string }> = {
  CODE: { bg: '#1e293b', text: '#93c5fd' },
  PATTERN: { bg: '#1a2a1a', text: '#86efac' },
  MISTAKE: { bg: '#2a1212', text: '#fca5a5' },
  OTHER: { bg: '#1a1a2a', text: '#c4b5fd' },
}

export interface Note {
  id: number
  type: NoteType
  title: string
  body: string
  language?: string | null
  tags: string[]
  createdAt: string
  updatedAt: string
}

// ─── 커뮤니티 ───
export type PostType =
  | 'QUESTION' | 'SOLUTION_SHARE' | 'FEEDBACK' | 'BUG_REPORT' | 'FEATURE_REQUEST'

export const POST_TYPE_LABEL: Record<PostType, string> = {
  QUESTION: '질문',
  SOLUTION_SHARE: '풀이 공유',
  FEEDBACK: '레벨/태그 의견',
  BUG_REPORT: '버그 제보',
  FEATURE_REQUEST: '기능 요청',
}

export const COMMUNITY_POST_TYPES: PostType[] = ['QUESTION', 'SOLUTION_SHARE']
export const SUGGESTION_POST_TYPES: PostType[] = ['FEEDBACK', 'BUG_REPORT', 'FEATURE_REQUEST']
export const VOTABLE_POST_TYPES: PostType[] = ['QUESTION', 'SOLUTION_SHARE']

export const POST_TYPE_STYLE: Record<PostType, { bg: string; text: string }> = {
  QUESTION: { bg: '#1e3a5f', text: '#60a5fa' },
  SOLUTION_SHARE: { bg: '#064e3b', text: '#10b981' },
  FEEDBACK: { bg: '#451a03', text: '#f59e0b' },
  BUG_REPORT: { bg: '#2a1212', text: '#fca5a5' },
  FEATURE_REQUEST: { bg: '#1a1a2a', text: '#c4b5fd' },
}

export type SuggestionStatus = 'IN_PROGRESS' | 'RESOLVED'

export const STATUS_LABEL: Record<SuggestionStatus, string> = {
  IN_PROGRESS: '처리 중',
  RESOLVED: '해결됨',
}

export const STATUS_COLOR: Record<SuggestionStatus, { bg: string; text: string }> = {
  IN_PROGRESS: { bg: '#451a03', text: '#f59e0b' },
  RESOLVED: { bg: '#064e3b', text: '#10b981' },
}

// 백엔드 AuthorResponse (탈퇴 시 id=0, "탈퇴한 사용자")
export interface Author {
  id: number
  nickname: string
}

export interface VoteSummary {
  upvotes: number
  downvotes: number
  score: number
  myVote: number // 1 | -1 | 0
}

export interface ProblemBrief {
  id: number
  title: string
}

// 목록 아이템 (본문 없음, 댓글 수 + 투표 집계)
export interface PostSummary {
  id: number
  author: Author
  problem?: ProblemBrief | null
  type: PostType
  title: string
  isPrivate: boolean
  hidden: boolean
  status?: SuggestionStatus | null
  commentCount: number
  votes: VoteSummary
  createdAt: string
  updatedAt: string
}

export interface Comment {
  id: number
  author: Author
  content: string
  createdAt: string
}

// 상세 (본문 + 관리자답변 + 댓글)
export interface PostDetail {
  id: number
  author: Author
  problem?: ProblemBrief | null
  type: PostType
  title: string
  content: string
  isPrivate: boolean
  hidden: boolean
  status?: SuggestionStatus | null
  adminReply?: string | null
  adminReplyAt?: string | null
  votes: VoteSummary
  comments: Comment[]
  createdAt: string
  updatedAt: string
}

// ─── 신고 (관리자) ───
export type ReportStatus = 'OPEN' | 'HANDLED' | 'DISMISSED'

export const REPORT_STATUS_LABEL: Record<ReportStatus, string> = {
  OPEN: '미처리',
  HANDLED: '처리 완료',
  DISMISSED: '기각',
}

export const REPORT_STATUS_COLOR: Record<ReportStatus, { bg: string; text: string }> = {
  OPEN: { bg: '#451a03', text: '#f59e0b' },
  HANDLED: { bg: '#064e3b', text: '#10b981' },
  DISMISSED: { bg: '#1a1d24', text: '#9ca3af' },
}

export interface ReportedPost {
  id: number
  title: string
  type: PostType
  hidden: boolean
  author: Author
}

export interface Report {
  id: number
  reason: string
  status: ReportStatus
  adminNote?: string | null
  reporter: Author
  post: ReportedPost
  createdAt: string
}

// ─── 유저 ───
export type UserRole = 'USER' | 'ADMIN'
export type AuthProvider = 'LOCAL' | 'GOOGLE' | 'NAVER'

export interface User {
  id: number
  email: string
  nickname: string
  role: UserRole
  emailVerified: boolean
}

// ─── 알림 ───
export type NotificationType =
  | 'COMMENT' | 'ADMIN_REPLY' | 'STATUS_CHANGE' | 'POST_HIDDEN'
  | 'REPORT_RESOLVED' | 'NEW_REPORT' | 'TIER_UP' | 'STALE_SUGGESTION'

export interface NotificationPayload {
  postId?: number
  postTitle?: string
  postType?: PostType
  commenterNickname?: string
  contentPreview?: string
  replyPreview?: string
  oldStatus?: SuggestionStatus | null
  newStatus?: SuggestionStatus
  resolution?: ReportStatus
  autoResolved?: boolean
  reportId?: number
  reason?: string
  adminNote?: string | null
  family?: string
  problemId?: number
  problemTitle?: string
  count?: number
  oldestPostId?: number
  oldestTitle?: string
  oldestDays?: number
}

export interface AppNotification {
  id: number
  type: NotificationType
  payload: NotificationPayload
  isRead: boolean
  readAt?: string | null
  createdAt: string
}

export interface NotificationList {
  items: AppNotification[]
  nextCursor: number | null
}
