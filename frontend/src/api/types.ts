export interface Domain { id: number; name: string; description: string; displayOrder: number }
export interface UserProfile { userId: number; name: string; email: string; program: string; roles: string[]; bio: string | null; avatarUrl: string | null; githubUrl: string | null; linkedinUrl: string | null; phone: string | null }
export interface DashboardData {
  profile: { userId: number; name: string; program: string; bio: string | null; avatarUrl: string | null; githubUrl: string | null }
  xp: { totalXp: number; breakdown: Record<string, number> }
  tasks: { assignedOpen: number; assignedCompleted: number; assignedVerified: number; submittedCompletions: number; verifiedCompletions: number; unverifiedCompletions: number }
  projects: { requestedMemberships: number; activeMemberships: number; rejectedMemberships: number; recentMemberships: ProjectMembership[] }
  attendance: { total: number; present: number; absent: number; late: number; excused: number }
  githubContributions: { total: number; verified: number; unverified: number }
  leaderboardRank: number | null
}
export interface Session { id: number; domainId: number; program: string; topic: string; type: string; date: string; time: string; description: string; objectives: string[]; instructor: string; leadId: number }
export interface Attendance { id: number; sessionId: number; userId: number; status: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED'; markedBy: number; createdAt: string }
export interface Resource { id: number; sessionId: number | null; domainId: number; title: string; url: string; type: string; difficulty: string; topics: string[]; quality: number | null }
export interface Project {
  id: number; name: string; domainId: number; status: string; program: string; problem: string; solution: string; technologies: string[]; requiredSkills: string[]; expectedMembers: number; outcome: string | null; createdBy: number; approvedBy: number | null; reviewComment: string | null; createdAt: string; updatedAt: string; members: ProjectMembership[]
}
export interface ProjectMembership { id: number; projectId: number; userId: number; role: string; status: string; reviewedBy: number | null; createdAt: string; projectName?: string; projectStatus?: string; program?: string; domainId?: number; domainName?: string; membershipRole?: string; membershipStatus?: string; joinedAt?: string }
export interface ProjectRequest { name: string; domainId: number; program: 'TECHNICAL' | 'FOUNDATION'; problem: string; solution: string; technologies: string[]; requiredSkills: string[]; expectedMembers: number; outcome?: string; status?: string; reviewComment?: string }
export interface Task { id: number; sessionId: number; projectId: number | null; title: string; requirements: string[]; deadline: string | null; verificationRequired: boolean; assigneeId: number | null; status: 'OPEN' | 'COMPLETED' | 'VERIFIED' }
export interface TaskRequest { sessionId: number; projectId?: number; title: string; requirements: string[]; deadline?: string; verificationRequired?: boolean; assigneeId?: number; status?: string }
export interface LeaderboardPage { entries: { rank: number; userId: number; name: string; program: string; totalXp: number }[]; page: number; size: number; totalElements: number; totalPages: number }
export interface GitHubStatus { provider: string; connected: boolean; username: string | null }
export interface GitHubAuthStart { provider: string; state: string; authorizationUrl: string }
export interface GitHubRepository { id: number | null; projectId: number | null; githubRepoId: number; ownerLogin: string; repoName: string; fullName: string; htmlUrl: string; visibility: string; linkedBy: number | null; linkedAt: string | null }
export interface AdminAccount { id: number; email: string; name: string; program: string; active: boolean; roles: string[]; domainIds: number[]; passwordSetupRequired: boolean }
export interface CreateAccountRequest { email: string; name: string; program: string; roles: string[]; domainIds: number[] }
export interface GitHubContribution { id: number; sha: string; message: string; url: string; authorLogin: string; committedAt: string; kernelUserId: number | null; verified: boolean; verifiedAt: string | null }
