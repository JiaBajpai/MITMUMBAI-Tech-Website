const API_BASE_URL = "http://localhost:8080/api/v1"

export async function apiClient(
  endpoint: string,
  options: RequestInit = {}
) {
  const response = await fetch(
    `${API_BASE_URL}${endpoint}`,
    {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...options.headers,
      },
    }
  )

  if (!response.ok) {
    throw new Error(`API error: ${response.status}`)
  }

  return response.json()
}