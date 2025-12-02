import { doRequest, doAsync } from 'lib/common'
import { Gender } from 'lib/types'
import { Response } from 'supertest'

export interface AvalaiblePayload {
  scheduleDestinationId: number
}

export async function seats(payload: AvalaiblePayload): Promise<Response> {
  return doAsync(
    doRequest.get(`/boardingPasses/seats/${payload.scheduleDestinationId}`)
  )
}

export interface InitPassenger {
  birthday: string
  first_name: string
  last_name: string
  is_customer: boolean
  gender: Gender
  hash: string
  principal_passenger: boolean
  need_preferential: boolean
  special_ticket_id: number
  tickets: InitPassengerTicket[]
}

export interface InitPassengerTicket {
  schedule_route_destination_id: number
  seat: string
}

export interface InitRoute {
  order_route: number
  schedule_route_destination_id: number
  ticket_type_route: number
}

export interface InitPayload {
  email: string
  phone: string
  purchase_origin: number
  passengers: InitPassenger[]
  routes: InitRoute[]
  seatings: number
  ticket_type: number
  travel_date: string
}

export async function preboarding(payload: InitPayload): Promise<Response> {
  return doAsync(doRequest.post(`/boardingPasses/register/init`).send(payload))
}

export async function cancelPreboarding(
  boardingPassId: number
): Promise<Response> {
  return doAsync(doRequest.get(`/boardingPasses/cancel/${boardingPassId}`))
}

export interface EndPayment {
  amount: number
  payment_method_id: number
}

export interface EndPayload {
  id: number
  customer_id: number
  payments: EndPayment[]
  cash_change: {
    paid: number
    paid_change: number
    total: number
  }
}

export async function register(payload: EndPayload): Promise<Response> {
  return doAsync(doRequest.post(`/boardingPasses/register/end`).send(payload))
}

export interface CancelRegisterPayload {
  cash_out_id: number
  customer_id: number
  notes: string
}

export async function cancelRegister(
  reservationCode: string,
  payload: CancelRegisterPayload
): Promise<Response> {
  return doAsync(
    doRequest
      .put(`/boardingPasses/register/cancel/${reservationCode}`)
      .send(payload)
  )
}

export default {
  seats,
  preboarding,
  register,
  cancelPreboarding,
  cancelRegister
}
