import { doRequest, doAsync } from 'lib/common'
import { Response } from 'supertest'

export async function get(): Promise<Response> {
  return doAsync(doRequest.get(`/customers/v2?page=1&limit=1`))
}

export default { get }
