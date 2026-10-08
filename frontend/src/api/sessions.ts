import { apiClient } from "./client"

export function getSessions() {
  return apiClient("/sessions")
}