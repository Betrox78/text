import { doRequest, doAsync } from 'lib/common'
import { Response } from 'supertest'

export async function getOpen(employeeId: number): Promise<Response> {
  return doAsync(
    doRequest.get(
      `/cashOuts?query=*,employee_id=${employeeId},cash_out_status=1,[cash_register_id=cash_registers.id].cash_register,[branchoffice_id=branchoffice.id].branch_office_type`
    )
  )
}

export async function getClosed(branchofficeId: number): Promise<Response> {
  return doAsync(
    doRequest.get(`/cashRegisters/actions/getClosed/${branchofficeId}`)
  )
}

export interface OpenCashOut {
  branchoffice_id: number
  cash_out_status: number
  cash_register_id: number
  // created_by: number
  employee_id: number
  initial_fund: number
  ip: string
  token: string
}

export async function open(payload: OpenCashOut) {
  return doAsync(doRequest.post(`/cashOuts/actions/open`).send(payload))
}

export default { open, getOpen, getClosed }
