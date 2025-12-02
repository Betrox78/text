import { doRequest, doAsync } from 'lib/common'
import { Response } from 'supertest'

export interface GetBranchofficePayload {
  type: 'T'
}
export async function get(payload: GetBranchofficePayload): Promise<Response> {
  return doAsync(
    doRequest.get(
      `/branchoffices?query=branch_office_type=%27${payload.type}%27,status=%271%27,city.name,*`
    )
  )
}

export default { get }
