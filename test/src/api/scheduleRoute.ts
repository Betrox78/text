import { doRequest, doAsync } from 'lib/common'
import { Response } from 'supertest'
import moment from 'moment'

export interface Ticket {
  id: number
  number: number
}
export interface AvalaiblePayload {
  date: Date
  time?: string
  tickets: Ticket[]
  terminalOriginId: number
  terminalDestinyId: number
}

export interface SeatsPayload {
  quantity: number
  scheduleDestinationId: number
  terminalOriginId: number
  terminalDestinyId: number
}

export async function avalaible(payload: AvalaiblePayload): Promise<Response> {
  return doAsync(
    doRequest.post('/schedulesRoutes/availableRoutes').send({
      toid: payload.terminalOriginId,
      tdid: payload.terminalDestinyId,
      dt: moment(payload.date).format('YYYY-MM-DD'),
      stl: payload.tickets
    })
  )
}

export default { avalaible }
