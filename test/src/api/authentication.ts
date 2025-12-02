import { doRequest, doAsync } from 'lib/common'

export type AppType =
  | 'branchoffice'
  | 'admin'
  | 'operation'
  | 'app_driver'
  | 'app_operation'
export interface LoginPayload {
  email: string
  pass: string
  module: AppType
}
export interface AccessToken {
  token: string
  expirationDate: Date
}
export interface LoginResponse {
  id: number
  name: string
  phone: string
  email: string
  profile_id: number
  user_type: string
  employee_id: number | null
  branchoffice_id: number | null
  branch_office_type: string
  accessToken: AccessToken
  refreshToken: string
  permissions: Permissions
}

export async function login(payload: LoginPayload) {
  return doAsync<LoginResponse>(doRequest.post('/auth/login').send(payload))
}

export default { login }
