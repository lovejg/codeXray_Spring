import client from './client'
import type { PageResponse, Problem, ProblemSource } from '../types'

export interface ProblemQuery {
  keyword?: string
  source?: ProblemSource
  tierMin?: number
  tierMax?: number
  tagId?: number
  page?: number
  size?: number
  sort?: string // 예: "level,desc"
}

export const problemsApi = {
  list: (q: ProblemQuery) =>
    client
      .get<PageResponse<Problem>>('/problems', { params: q })
      .then((r) => r.data),

  get: (id: number) => client.get<Problem>(`/problems/${id}`).then((r) => r.data),

  // 붙여넣은 프로그래머스 URL로 문제 하나를 찾음 (없으면 404)
  lookupByUrl: (url: string) =>
    client.get<Problem>('/problems/lookup', { params: { url } }).then((r) => r.data),
}
