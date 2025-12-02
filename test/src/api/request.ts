import { doRequest, doAsync } from 'lib/common'
import { Test } from 'supertest'

export interface BaseConfig {
  path?: string
  headers?: any
  query?: any
}

interface BodylessConfig extends BaseConfig {
  method: 'GET' | 'DELETE'
}

interface BodyConfig extends BaseConfig {
  method: 'POST' | 'PUT'
  data?: any
}

export type RequestConfig = BodylessConfig | BodyConfig

const request = (config: RequestConfig) => {
  const { method, path, query, headers } = config

  let req: Test

  const url = `/${path || ''}`

  switch (method) {
    case 'GET':
      req = doRequest.get(url)
      break
    case 'PUT':
      req = doRequest.put(url)
      break
    case 'POST':
      req = doRequest.post(url)
      break
    case 'DELETE':
      req = doRequest.del(url)
      break
    default:
      req = doRequest.get(url)
  }

  if ((config.method === 'POST' || config.method === 'PUT') && config.data) {
    req.send(config.data)
  }

  if (query) {
    req.query(query)
  }

  if (headers) {
    Object.keys(headers).forEach(key => {
      req.set(key, headers[key])
    })
  }

  return req
}

const service = () => {
  const get = <T = any>(path: string, config?: BaseConfig) => {
    return doAsync<T>(
      request({
        path,
        method: 'GET',
        ...config
      })
    )
  }

  const post = <T = any, P extends object = any>(
    path: string,
    payload: P,
    config?: BaseConfig
  ) => {
    return doAsync<T>(
      request({
        path,
        method: 'POST',
        data: payload,
        ...config
      })
    )
  }

  const put = <T = any, P extends object = any>(
    path: string,
    payload: Partial<P>,
    config?: BaseConfig
  ) => {
    return doAsync<T>(
      request({
        path,
        method: 'PUT',
        data: payload,
        ...config
      })
    )
  }

  const deleteFn = <T = any>(path: string, config?: BaseConfig) => {
    return doAsync<T>(
      request({
        path,
        method: 'DELETE',
        ...config
      })
    )
  }

  return {
    post,
    get,
    put,
    delete: deleteFn,
    request
  }
}

export default service
