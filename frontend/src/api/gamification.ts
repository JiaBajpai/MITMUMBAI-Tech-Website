import { apiClient } from './client'
import type { LeaderboardPage } from './types'

export interface XpSummary { totalXp: number; breakdown: Record<string, number> }
export const gamificationApi = {
  xp: (signal?: AbortSignal) => apiClient<XpSummary>('/me/xp', { signal }),
  leaderboard: (program = 'TECHNICAL', page = 0, size = 10, signal?: AbortSignal) => apiClient<LeaderboardPage>(`/leaderboard?${new URLSearchParams({ program, page: String(page), size: String(size) })}`, { signal }),
  domainLeaderboard: (domainId: number, page = 0, size = 10, signal?: AbortSignal) => apiClient<LeaderboardPage>(`/leaderboard/domains/${domainId}?${new URLSearchParams({ page: String(page), size: String(size) })}`, { signal }),
}
