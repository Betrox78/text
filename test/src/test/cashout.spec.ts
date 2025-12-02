import authenticationTest, { Employee } from 'test/auth'
import assert from 'assert'
import cashOut from 'api/cashOut'
import { hash } from 'lib/common'

describe('CASHOUT', function() {
  it('Open [OK]', async done => {
    try {
      const employee = await authenticationTest.employeeLogin()
      const openCashout = await getOpen(employee)
      if (openCashout) {
        // TODO: Close open cash out to reopen
        return done()
      }

      const closedCashOut = await getClosed(employee)
      if (!closedCashOut) {
        // TODO: Close open cash to reopen
        return done()
      }
      const cashout = await open(employee, closedCashOut)
      assert.ok(cashout, 'Invalid cash out')
      done()
    } catch (error) {
      done(error)
    }
  })

  it('Close [OK]', async done => {
    try {
      // TODO: Implement test
      done()
    } catch (error) {
      done(error)
    }
  })
})


export interface CashOut {
  id: number
}

export interface ClosedChasOut extends CashOut {
  finalFund: number
}

async function getOpen(payload: Employee) {
  const { body: cashOutBody } = await cashOut.getOpen(payload.employeeId)
  assert.equal(
    cashOutBody.status,
    'OK',
    `Error to get open cash out: ${JSON.stringify(cashOutBody)}`
  )
  const [openCashOut] = cashOutBody.data

  if (openCashOut) {
    return { id: openCashOut.id }
  }
}


export async function getClosed(payload: Employee): Promise<ClosedChasOut> {
  const { body: closedCashOutBody } = await cashOut.getClosed(
    payload.branchofficeId
  )
  assert.equal(
    closedCashOutBody.status,
    'OK',
    `Error to get closed cash out: ${JSON.stringify(closedCashOutBody)}`
  )
  const [closedChasOut] = closedCashOutBody.data

  if (closedChasOut) {
    return { id: closedChasOut.id, finalFund: closedChasOut.final_fund }
  }
}

export async function open(
         payload: Employee,
         closedCashOut: ClosedChasOut
       ): Promise<CashOut> {


         const { body: openCashOutBody } = await cashOut.open({
           branchoffice_id: payload.branchofficeId,
           cash_out_status: 1,
           cash_register_id: closedCashOut.id,
           employee_id: payload.employeeId,
           initial_fund: closedCashOut.finalFund,
           ip: '127.0.0.1',
           token: hash()
         })
         assert.equal(
           openCashOutBody.status,
           'OK',
           `Error to open cash out: ${JSON.stringify(openCashOutBody)}`
         )
         assert.ok(
           openCashOutBody.data && openCashOutBody.data.id,
           'Invalid open cash out id'
         )

         return { id: openCashOutBody.data.id }
       }

export default { open, getOpen, getClosed }
