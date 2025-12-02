import { Gender, Origin, TicketType, TicketTypeRoute } from 'lib/types'
import authenticationTest, { Employee } from 'test/auth'
import cashOutTest from 'test/cashout.spec'
import { emptySeat, random, hash } from 'lib/common'
import paymentMethod from 'api/paymentMethod'
import specialTicket from 'api/specialTicket'
import scheduleRoute from 'api/scheduleRoute'
import branchoffice from 'api/branchoffice'
import boardingPass from 'api/boardingPass'
import customer from 'api/customer'
import assert from 'assert'
import moment from 'moment'

describe('BOARDING PASS', function() {
  let init: Register
  it('Preboarding [OK]', async done => {
    try {
      const employee = await authenticationTest.employeeLogin()
      init = await initRegister(employee)
      assert.ok(init, 'Error to init preboarding')
      done()
    } catch (error) {
      done(error)
    }
  })

  it('Cancel preboarding [OK]', async done => {
    try {
      if (!init) return done()

      const { body } = await boardingPass.cancelPreboarding(init.boardingPassId)
      assert.equal(
        body.status,
        'OK',
        `Error to cancel preboarding: ${JSON.stringify(body)}`
      )
      init = null
      done()
    } catch (error) {
      done(error)
    }
  })

  let end: Register
  it('Register [OK]', async done => {
    try {
      const employee = await authenticationTest.employeeLogin()
      const cashout = await cashOutTest.getOpen(employee)
      if (!cashout) {
        const closedCashOut = await cashOutTest.getClosed(employee)
        if (!closedCashOut) {
          // TODO: Close open cash to reopen
          return done()
        }
        await cashOutTest.open(employee, closedCashOut)
      }
      end = await register(employee)

      assert.ok(end && end.reservationCode, 'Error to register boarding pass')
      done()
    } catch (error) {
      done(error)
    }
  })

  it('Cancel register [OK]', async done => {
    try {
      if (!end) return done()

      const employee = await authenticationTest.employeeLogin()
      let chasout = await cashOutTest.getOpen(employee)
      if (!chasout) {
        const closedCashOut = await cashOutTest.getClosed(employee)
        if (!closedCashOut) {
          // TODO: Close open cash to reopen
          return done()
        }
        chasout = await cashOutTest.open(employee, closedCashOut)
      }
      const { body } = await boardingPass.cancelRegister(end.reservationCode, {
        cash_out_id: chasout.id,
        customer_id: end.customerId,
        notes: 'Testing'
      })
      assert.equal(
        body.status,
        'OK',
        `Error to cancel preboarding: ${JSON.stringify(body)}`
      )
      end = null
      done()
    } catch (error) {
      done(error)
    }
  })
})

interface Register {
  customerId: number
  employeeId: number
  branchofficeId: number
  reservationCode: string
  boardingPassId: number
  passengers: [{ tickets: [{ total_amount: number }] }]
}

async function initRegister(employee: Employee): Promise<Register> {
  const { body: boBody } = await branchoffice.get({ type: 'T' })
  assert.equal(
    boBody.status,
    'OK',
    `Error to get branchoffices: ${JSON.stringify(boBody)}`
  )
  const branchoffices: any[] = boBody.data
  const boCount = branchoffices.length
  const boOriginIndex = random(0, boCount - 1)
  let boDestinyIndex = random(0, boCount - 1)

  while (boOriginIndex == boDestinyIndex) {
    boDestinyIndex = random(0, boCount - 1)
  }
  const boOrigin = branchoffices[boOriginIndex]
  const boDestiny = branchoffices[boDestinyIndex]

  assert.ok(boOrigin, 'Invalid origin terminal')
  assert.ok(boDestiny, 'Invalid destiny terminal')

  const { body: stBody } = await specialTicket.get({
    originAllowed: Origin[Origin.sucursal]
  })
  assert.equal(
    stBody.status,
    'OK',
    `Error to get special tickets: ${JSON.stringify(stBody)}`
  )
  const sTicket = stBody.data.find((t: { id: any }) => !!t.id)
  assert.ok(sTicket, 'Invalid special ticket')

  const terminalOriginId = boOrigin.id,
    terminalDestinyId = boDestiny.id

  const { body: cmBody } = await customer.get()
  assert.equal(
    cmBody.status,
    'OK',
    `Error to get customers: ${JSON.stringify(cmBody)}`
  )
  const cmCustomer = cmBody.data.results.find((p: { id: any }) => p.id)
  assert.ok(cmCustomer && cmCustomer.id, 'Invalid customer')
  const { body: routeBody } = await scheduleRoute.avalaible({
    date: moment()
      .utcOffset(0)
      .set({ hour: 0, minute: 0, second: 0, millisecond: 0 })
      .add(1, 'day')
      .toDate(),
    terminalOriginId: 1,
    terminalDestinyId: 5,
    tickets: [
      {
        id: sTicket.id,
        number: 1
      }
    ]
  })
  assert.equal(
    routeBody.status,
    'OK',
    `Error to get available routes: ${JSON.stringify(routeBody)}`
  )
  const route = routeBody.data.find(
    (r: { available_seatings: any }) => !!r.available_seatings
  )
  assert.ok(route, `Invalid schedule route ${JSON.stringify(routeBody.data)}`)

  const { body: seatsBody } = await boardingPass.seats({
    scheduleDestinationId: route.schedule_route_destination_id
  })
  assert.equal(
    seatsBody.status,
    'OK',
    `Error to get available seats: ${JSON.stringify(seatsBody)}`
  )
  const {
    data: { config, busy_seats }
  } = seatsBody
  const seat = emptySeat(config, busy_seats)
  assert.ok(seat, 'Invalid seat')

  const { body: initBody } = await boardingPass.preboarding({
    email: 'bot@testing.com',
    phone: '66812345678',
    purchase_origin: Origin.sucursal,
    seatings: 1,
    ticket_type: TicketType.sencillo,
    travel_date: moment(route.departure_date)
      .utcOffset(0)
      .format('YYYY-MM-DD hh:mm:ss'),
    routes: [
      {
        order_route: 1,
        schedule_route_destination_id: route.schedule_route_destination_id,
        ticket_type_route: TicketTypeRoute.ida
      }
    ],
    passengers: [
      {
        birthday: '1990-01-01',
        first_name: 'Bot',
        last_name: 'Test',
        gender: Gender.male,
        is_customer: false,
        need_preferential: false,
        principal_passenger: true,
        special_ticket_id: sTicket.id,
        tickets: [
          {
            schedule_route_destination_id: route.schedule_route_destination_id,
            seat: seat.seat
          }
        ],
        hash: hash()
      }
    ]
  })
  assert.equal(
    initBody.status,
    'OK',
    `Error to init boarding pass: ${JSON.stringify(initBody)}`
  )
  const {
    data: { passengers, id, reservation_code }
  } = initBody
  assert.ok(id, 'Invalid boarding pass Id')
  assert.ok(reservation_code, 'Invalid reservation code')

  return {
    reservationCode: reservation_code,
    boardingPassId: id,
    customerId: cmCustomer.id,
    employeeId: employee.employeeId,
    branchofficeId: employee.branchofficeId,
    passengers
  }
}

async function register(employee: Employee): Promise<Register> {
  const init: Register = await initRegister(employee)

  const { body: pmBody } = await paymentMethod.get({ isCash: true })
  assert.equal(
    pmBody.status,
    'OK',
    `Error to get payment methods: ${JSON.stringify(pmBody)}`
  )
  const pMethod = pmBody.data.results.find((p: { is_cash: any }) => p.is_cash)
  assert.ok(pMethod, 'Invalid payment method')

  const tickets = init.passengers.reduce((a, p) => a.concat(p.tickets), [])
  const total = tickets.reduce((p, t) => t.total_amount + p, 0)
  const { body: endBody } = await boardingPass.register({
    id: init.boardingPassId,
    customer_id: init.customerId,
    cash_change: {
      paid: total,
      paid_change: 0,
      total: total
    },
    payments: [{ amount: total, payment_method_id: pMethod.id }]
  })
  assert.equal(
    endBody.status,
    'OK',
    `Error to end boarding pass: ${JSON.stringify(endBody)}`
  )
  assert.ok(endBody.data, 'Invalid data on end boarding pass')

  return init
}
