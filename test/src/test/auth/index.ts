import assert from 'assert'
import environment from 'lib/environment'
import { authorize } from 'lib/common'
import authentication, { AppType } from 'api/authentication'

export async function employeeLogin() {
  const user = await login(
    environment.abordoPosUser,
    environment.abordoPosPassword,
    'branchoffice'
  )
  authorize(user.token)
  return user
}

export const adminLogin = () => {
  return login(
    environment.abordoAdminUser,
    environment.abordoAdminPassword,
    'admin'
  )
}

export const login = async (email: string, password: string, app: AppType) => {
  const { body } = await authentication.login({
    email,
    module: app,
    pass: password
  })
  assert.equal(body.status, 'OK', `Error on login: ${JSON.stringify(body)}`)
  assert.ok(
    body.data.accessToken,
    `Error on login: ${JSON.stringify(body.data)}`
  )
  const { id, employee_id, branchoffice_id, accessToken } = body.data

  return {
    id,
    employeeId: employee_id,
    branchofficeId: branchoffice_id,
    token: accessToken.token
  }
}

export type Employee = PromiseValue<typeof login>

export default { employeeLogin, adminLogin }
