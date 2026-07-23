import client from './client'
import type { Feedback } from '../types'

export const ratingsApi = {
  // 없으면 백엔드가 null 반환
  getMyFeedback: (problemId: number) =>
    client.get<Feedback | null>(`/ratings/feedback/${problemId}`).then((r) => r.data),

  submitFeedback: (problemId: number, level: number) =>
    client.post<Feedback>(`/ratings/feedback/${problemId}`, { level }).then((r) => r.data),
}
