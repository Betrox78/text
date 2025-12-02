import { doRequest, doAsync } from 'lib/common'
import { Response } from 'supertest'

export interface GetSpecialticketPayload {
  originAllowed: string
}
export async function get(payload: GetSpecialticketPayload): Promise<Response> {
  return doAsync(
    doRequest.get(
      `/specialTickets/v2?search=${payload.originAllowed}&searchKeys=origin_allowed`
    )
  )
}

export default { get }
