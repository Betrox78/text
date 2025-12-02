import {
  JobsContext,
  setUpJobs,
  clearJobs,
  jobBuilder,
  jobErrorBuilder
} from '.'
import { GenericStatus } from 'api/generic'

let ctx: JobsContext

describe('JOBS', () => {
  beforeAll(async () => {
    ctx = await setUpJobs()
    return ctx
  })

  it('list jobs', async () => {
    const { jobService } = ctx
    const LIMIT = 5
    const { body } = await jobService.list({ limit: LIMIT, page: 1 })
    expect(body.status).toBe('OK')

    const jobsResult = body.data

    expect(jobsResult.items).toBeLessThanOrEqual(LIMIT)
    expect(jobsResult.results.length).toBeLessThanOrEqual(LIMIT)
  })

  it('register job', async () => {
    const { jobService, user } = ctx
    const newJob = jobBuilder()

    const { body: registerBody } = await jobService.post(newJob)

    expect(registerBody.status).toBe('OK')

    const { body: getBody } = await jobService.get(registerBody.data.id)

    expect(getBody.status).toBe('OK')
    expect(getBody.data).not.toBeNull()
    expect(getBody.data.id).toBe(registerBody.data.id)
    expect(getBody.data.created_by).toBe(user.id)
  })

  it('failed registering too long data', async () => {
    const { jobService } = ctx

    const newJob = jobErrorBuilder()

    const { body: registerBody } = await jobService.post(newJob)

    expect(registerBody.status).toBe('WARNING')
    expect(registerBody.devMessage).not.toBeNull()
  })

  it('update job', async () => {
    const { jobService, jobs } = ctx
    const job = jobs.pop()

    const NEW_FIELD = 'TestName'

    const { body: updateBody } = await jobService.put(job.id, {
      name: NEW_FIELD
    })

    expect(updateBody.status).toBe('OK')

    const { body: getBody } = await jobService.get(job.id)

    expect(getBody.status).toBe('OK')
    expect(getBody.data).not.toBeNull()
    expect(getBody.data.id).toBe(job.id)
    expect(getBody.data.name).toBe(NEW_FIELD)
  })

  it('delete job', async () => {
    const { jobService, jobs } = ctx
    const job = jobs.pop()

    const { body: deleteeBody } = await jobService.delete(job.id)

    expect(deleteeBody.status).toBe('OK')

    const { body: getBody } = await jobService.get(job.id)

    expect(getBody.status).toBe('OK')
    expect(getBody.data).not.toBeNull()
    expect(getBody.data.status).toBe(GenericStatus.DELETED)
  })

  afterAll(async () => {
    await clearJobs(ctx)
  })
})
