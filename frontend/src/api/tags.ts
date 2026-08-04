import client from './client'
import type { Tag } from '../types'

// 백엔드 GET /api/tags 는 {"tags":[...]} 엔벨로프로 응답 → tags 배열만 꺼내 반환
export const tagsApi = {
  list: () =>
    client.get<{ tags: Tag[] }>('/tags').then((r) => r.data.tags),
}
