import client from './client'
import type { Problem } from '../types'

export interface Recommendation {
  problem: Problem
  reason: string // '약점 보강' | '적정 난이도'
  tag: string | null // 매칭된 약점 태그(있으면)
}

export const recommendApi = {
  list: () => client.get<Recommendation[]>('/recommendations').then((r) => r.data),
}
