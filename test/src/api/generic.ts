import request, { BaseConfig } from './request'

export interface QueryParams {
  where?: string
  select?: string
  from?: number
  to?: number
  orderBy?: string
  search?: string
  searchKeys?: string
  specialJoin?: string
  joinType?: 'inner' | 'left' | 'right'
  page?: number
  limit?: number
}

export interface Model {
  id: number
  status: number
  created_at: Date
  created_by: number
  updated_at: Date | null
  updated_by: number | null
}

export interface PaginatedResult<T> {
  count: number
  items: number
  results: T[]
}

export const GenericStatus = {
  ACTIVE: 1,
  INACTIVE: 0,
  DELETED: 3
}

const service = (model: string) => {
  const client = request()

  const list = <T = any>(query?: QueryParams, config?: BaseConfig) => {
    return client.get<PaginatedResult<T>>(`${model}/v2`, { ...config, query })
  }

  const get = <T = any>(id: number, config?: BaseConfig) => {
    return client.get<T>(`${model}/${id}`, config)
  }

  const post = <T = any, P extends object = any>(
    payload: P,
    config?: BaseConfig
  ) => {
    return client.post<T, P>(`${model}`, payload, config)
  }

  const put = <T = any, P extends object = any>(
    id: number,
    payload: Partial<P>,
    config?: BaseConfig
  ) => {
    return client.put<T, P>(`${model}`, { id, ...payload }, config)
  }

  const deleteFn = <T = any>(id: number, config?: BaseConfig) => {
    return client.delete<T>(`${model}/${id}`, config)
  }

  return {
    model,
    post,
    list,
    get,
    put,
    delete: deleteFn
  }
}

export default service
