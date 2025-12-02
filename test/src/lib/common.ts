import supertest, { Test, Response } from 'supertest'
import environment from 'lib/environment'
import crypto from 'crypto'
export const doRequest = supertest(environment.apiHostUrl)

let token: string
export function authorize(_token: string) {
  token = _token
}

export function random(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1) + min)
}

export function hash() {
  return crypto
    .createHash('sha1')
    .digest()
    .toString('hex')
}

export interface ApiBody<T = any> {
  status: ApiResponseStatus
  message?: string
  devMessage?: string
  data: T
}

export type ApiResponseStatus = 'OK' | 'WARNING' | 'ERROR' | 'INVALID_TOKEN'
export interface ApiResponse<T> extends Response {
  body: ApiBody<T>
}

export const doAsync = async <T = any>(test: Test): Promise<ApiResponse<T>> => {
  return new Promise((resolve, reject) => {
    if (token) {
      test.set('Authorization', token)
    }
    test
      .set('Accept', 'application/json')
      .expect('Content-Type', 'application/json')
      .expect(200)
      .end((err, res) => {
        if (err) {
          return reject(err)
        }

        resolve(res)
      })
  })
}

export const emptySeat = (config: BusConfig, busySeats: string[]): BusSeat => {
  return makeMap(config)
    .find(r => r.find(s => s.seat && busySeats.indexOf(s.seat) < 0))
    .find(f => f.seat && busySeats.indexOf(f.seat) < 0)
}

export const makeMap = (config: BusConfig): Bus => {
  const map = []
  const total_col = config.total_col + 1
  const total_row = config.total_row + 1
  const div_col = parseInt(config.division_by_col, 10) + 1
  const getSeatName = seatName(parseInt(config.enum_type, 10), total_col)
  for (let row = 1, rowLen = total_row; row <= rowLen; row += 1) {
    const mapRow = []
    for (let col = 1, colLen = total_col; col <= colLen; col += 1) {
      const seat = {
        position: `${row},${col}`,
        seat: getSeatName(row, col)
      }
      mapRow.push(setState(seat))
      if (col === div_col) {
        mapRow.push({
          no_seat: true
        })
      }
    }
    map.push(mapRow)
  }

  return map

  function seatName(type: number, cols: number) {
    if (!type) {
      return (row: number, col: number) => ((row - 1) * cols + col).toString()
    }

    if (type === 1) {
      return (row: number, col: number) => String.fromCharCode(64 + row) + col
    }

    return (row: number, col: number) => String.fromCharCode(64 + col) + row
  }

  function setState(seat: BusSeat) {
    if (seat.seat && seat.position) {
      if (config.busy_seats && config.busy_seats.includes(seat.seat)) {
        seat.type = SeatType.busy
      } else if (
        config.special_seats_women &&
        config.special_seats_women.includes(seat.position)
      ) {
        seat.type = SeatType.women
      } else if (
        config.special_seats_handicapped &&
        config.special_seats_handicapped.includes(seat.position)
      ) {
        seat.type = SeatType.handicapped
      } else if (config.not_seats && config.not_seats.includes(seat.position)) {
        seat.no_seat = true
      }
    }
    return seat
  }
}

export interface BusStatus {
  busy_seats: string[]
  config: BusConfig
}

export interface BusConfig {
  id: number
  seatings: number
  busy_seats: string[]
  total_row: number
  total_col: number
  seat_by_row: number
  seat_by_col: number
  division_by_row: string
  division_by_col: string
  enumeration: string
  enum_type: string
  not_seats: string
  emergency_exit: string
  total_special_seats_handicapped: number
  special_seats_handicapped: string
  status: number
  created_at: string
  created_by: number
  updated_at?: any
  updated_by?: any
  name: string
  base: boolean
  is_base: boolean
  allow_frozen: boolean
  allow_pets: boolean
  total_special_seats_women: number
  special_seats_women: string
  schedule_route_id: number
  order_origin: number
  order_destiny: number
}

export interface BusSeat {
  seat?: string
  position?: string
  no_seat?: boolean
  type?: SeatType
  selected?: boolean
}

export enum SeatType {
  busy = 'not_available_seat',
  women = 'available_women_seat',
  handicapped = 'available_handicapped_seat',
  available = 'available_seat'
}

type BusRow = BusSeat[]

export type Bus = BusRow[]
