import client from './client'
import type {
  Comment,
  PostDetail,
  PostSummary,
  PostType,
  Report,
  ReportStatus,
  SuggestionStatus,
  VoteSummary,
} from '../types'

export interface PostListQuery {
  type?: PostType
  types?: string // CSV
  problemId?: number
  status?: SuggestionStatus
  authorId?: number
  sort?: 'recent' | 'votes'
}

export interface CreatePostBody {
  problemId?: number
  type: PostType
  title: string
  content: string
  isPrivate?: boolean
}

export interface UpdatePostBody {
  title: string
  content: string
  isPrivate?: boolean
}

export const communityApi = {
  listPosts: (q: PostListQuery) =>
    client.get<PostSummary[]>('/community/posts', { params: q }).then((r) => r.data),

  getPost: (id: number) =>
    client.get<PostDetail>(`/community/posts/${id}`).then((r) => r.data),

  createPost: (body: CreatePostBody) =>
    client.post<PostDetail>('/community/posts', body).then((r) => r.data),

  updatePost: (id: number, body: UpdatePostBody) =>
    client.put<PostDetail>(`/community/posts/${id}`, body).then((r) => r.data),

  deletePost: (id: number) => client.delete(`/community/posts/${id}`),

  // 댓글
  addComment: (postId: number, content: string) =>
    client.post<Comment>(`/community/posts/${postId}/comments`, { content }).then((r) => r.data),

  deleteComment: (id: number) => client.delete(`/community/comments/${id}`),

  // 투표
  vote: (postId: number, value: 1 | -1) =>
    client.post<VoteSummary>(`/community/posts/${postId}/vote`, { value }).then((r) => r.data),

  removeVote: (postId: number) =>
    client.delete<VoteSummary>(`/community/posts/${postId}/vote`).then((r) => r.data),

  // 신고
  report: (postId: number, reason: string) =>
    client.post<Report>(`/community/posts/${postId}/report`, { reason }).then((r) => r.data),

  // ── 관리자 ──
  listReports: (status?: ReportStatus) =>
    client.get<Report[]>('/community/admin/reports', { params: { status } }).then((r) => r.data),

  updateReport: (id: number, status: ReportStatus, adminNote?: string) =>
    client.patch<Report>(`/community/admin/reports/${id}`, { status, adminNote }).then((r) => r.data),

  hidePost: (id: number, hidden: boolean) =>
    client.patch<PostDetail>(`/community/admin/posts/${id}/hide`, { hidden }).then((r) => r.data),

  updateStatus: (id: number, status: SuggestionStatus) =>
    client.patch<PostDetail>(`/community/posts/${id}/status`, { status }).then((r) => r.data),

  updateAdminReply: (id: number, adminReply: string) =>
    client.patch<PostDetail>(`/community/posts/${id}/admin-reply`, { adminReply }).then((r) => r.data),
}
