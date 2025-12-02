import { doRequest, doAsync } from 'lib/common'
import { Response } from 'supertest'
export interface GetPaymentMethodPayload {
  isCash: boolean
}
export async function get(payload: GetPaymentMethodPayload): Promise<Response> {
  return doAsync(
    doRequest.get(
      `/paymentMethods/v2?page=1&limit=1&where=is_cash=${payload.isCash}`
    )
  )
}

export default { get }
