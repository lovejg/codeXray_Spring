import client from './client'
import type { Memo, Solution } from '../types'

export interface UpsertMemoBody {
  wrongReason?: string
  logic?: string
  keyFunctions?: string
  freeNote?: string
}

export const solutionsApi = {
  list: (starred?: boolean) =>
    client
      .get<Solution[]>('/solutions', { params: starred === undefined ? {} : { starred } })
      .then((r) => r.data),

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
