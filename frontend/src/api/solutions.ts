import client from './client'
import type { Memo, PageResponse, Solution } from '../types'

export interface UpsertMemoBody {
  wrongReason?: string
  logic?: string
  keyFunctions?: string
  freeNote?: string
}

export const solutionsApi = {
  // 내 풀이 목록(페이지). page 는 0부터, size 는 서버에서 최대 50
  list: (params: { starred?: boolean; page?: number; size?: number } = {}) =>
    client
      .get<PageResponse<Solution>>('/solutions', { params })
      .then((r) => r.data),

  // 이 문제에 대한 내 풀이. 아직 없으면 서버가 204 → null
  byProblem: (problemId: number) =>
    client
      .get<Solution | ''>(`/solutions/by-problem/${problemId}`)
      .then((r) => (r.status === 204 || !r.data ? null : r.data)),

  get: (id: number) => client.get<Solution>(`/solutions/${id}`).then((r) => r.data),

  // upsert: 같은 문제면 코드 갱신, 없으면 새 풀이
  create: (problemId: number, code: string, language?: string) =>
    client.post<Solution>('/solutions', { problemId, code, language }).then((r) => r.data),

  update: (id: number, code: string, language?: string) =>
    client.put<Solution>(`/solutions/${id}`, { code, language }).then((r) => r.data),

  toggleStar: (id: number) =>
    client.patch<Solution>(`/solutions/${id}/star`).then((r) => r.data),

  upsertMemo: (id: number, body: UpsertMemoBody) =>
    client.put<Memo>(`/solutions/${id}/memo`, body).then((r) => r.data),

  remove: (id: number) => client.delete(`/solutions/${id}`),
}
