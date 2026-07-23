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
}
