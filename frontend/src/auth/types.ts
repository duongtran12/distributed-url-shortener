export interface UserProfile {
  id: number
  email: string
  displayName: string
  role: 'USER' | 'ADMIN'
  createdAt: string
}

export interface ApiFieldError {
  field: string
  message: string
}

export interface ApiErrorPayload {
  code?: string
  message?: string
  fieldErrors?: ApiFieldError[]
}
