import client from './client'
import type { Note, NoteType } from '../types'

export interface NoteBody {
  type: NoteType
  title: string
  body: string
  language?: string
  tags?: string[]
}

export const notesApi = {
  list: (type?: NoteType, search?: string) =>
    client
      .get<Note[]>('/notes', { params: { type, search } })
      .then((r) => r.data),

  get: (id: number) => client.get<Note>(`/notes/${id}`).then((r) => r.data),

  create: (body: NoteBody) => client.post<Note>('/notes', body).then((r) => r.data),

  update: (id: number, body: NoteBody) =>
    client.put<Note>(`/notes/${id}`, body).then((r) => r.data),

  remove: (id: number) => client.delete(`/notes/${id}`),
}
