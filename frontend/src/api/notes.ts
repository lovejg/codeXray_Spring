import client from './client'
import type { Note, NoteType, PageResponse } from '../types'

export interface NoteBody {
  type: NoteType
  title: string
  body: string
  language?: string
  tags?: string[]
}

export const notesApi = {
  // 내 노트 목록(페이지). page 는 0부터
  list: (params: { type?: NoteType; search?: string; page?: number; size?: number } = {}) =>
    client
      .get<PageResponse<Note>>('/notes', { params })
      .then((r) => r.data),

  get: (id: number) => client.get<Note>(`/notes/${id}`).then((r) => r.data),

  create: (body: NoteBody) => client.post<Note>('/notes', body).then((r) => r.data),

  update: (id: number, body: NoteBody) =>
    client.put<Note>(`/notes/${id}`, body).then((r) => r.data),

  remove: (id: number) => client.delete(`/notes/${id}`),
}
