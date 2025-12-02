import generic, { QueryParams, Model } from './generic'

export interface Job {
  name: string
  description: string
  file_description: string
  salary: number
}

export interface JobProps {
  token: string
}

type JobModel = Job & Model

const JobService = (props: JobProps) => {
  const MODEL = 'jobs'

  const client = generic(MODEL)

  const config = {
    headers: {
      Authorization: props.token,
      'Content-Type': 'application/json'
    }
  }

  const list = (query?: QueryParams) => {
    return client.list<JobModel>(query, config)
  }

  const get = (id: number) => {
    return client.get<JobModel>(id, config)
  }

  const post = (payload: Job) => {
    return client.post<{ id: number }, Job>(payload, config)
  }

  const put = (id: number, payload: Partial<Job>) => {
    return client.put<any, Job>(id, payload, config)
  }

  const deleteFn = (id: number) => {
    return client.delete<any>(id, config)
  }

  return { post, list, get, put, delete: deleteFn }
}
export default JobService
