import client from './client'

export interface Dashboard {
  summary: {
    totalSolved: number
    starred: number
    notes: number
    currentStreak: number
    longestStreak: number
  }
  tiers: { tier: string; count: number }[]
  languages: { label: string; count: number }[]
  topTags: { label: string; count: number }[]
  weakTags: { tag: string; avgLevel: number; count: number }[]
  heatmap: { date: string; count: number }[] // date = 'yyyy-MM-dd'
}

export const statsApi = {
  dashboard: () => client.get<Dashboard>('/stats/me').then((r) => r.data),
}
