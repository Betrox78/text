import { build, fake } from '@jackfranklin/test-data-bot'
import JobService, { Job } from 'api/job'
import auth from 'test/auth'

const SALARY_PRECISION = 2

const DESCRIPTION_LENGTH = 20

const builder = build<Job>('Job', {
  fields: {
    name: fake(f => f.name.jobTitle()),
    description: fake(
      f =>
        `${f.random.alphaNumeric(DESCRIPTION_LENGTH)}
        ${f.random.alphaNumeric(DESCRIPTION_LENGTH)}`
    ),
    file_description: fake(f => f.image.image()),
    salary: fake(f => f.random.number({ precision: SALARY_PRECISION }))
  }
})

const errorBuilder = build<Job>('Job', {
  fields: {
    name: fake(f => f.name.jobTitle()),
    description: fake(f => f.lorem.paragraph(DESCRIPTION_LENGTH)),
    file_description: fake(f => f.image.image()),
    salary: fake(f => f.random.number({ precision: SALARY_PRECISION }))
  }
})

const jobBuilder = () => builder()

const jobErrorBuilder = () => errorBuilder()

const registerJob = async (
  service: ReturnType<typeof JobService>,
  job: Job
) => {
  const result = await service.post(job)
  if (result.body.status !== 'OK') {
    throw result.body
  }
  return result
}

const createJobs = async (
  service: ReturnType<typeof JobService>,
  num: number
) => {
  const newJobs = [...Array(num).keys()].map(() => jobBuilder())
  const promises = newJobs.map(job => registerJob(service, job))

  const postResults = await Promise.all(promises)

  const ids = postResults.map(result => result.body.data.id)

  const jobsResults = await Promise.all(ids.map(id => service.get(id)))

  return jobsResults.map(res => res.body.data)
}

const setUpJobs = async () => {
  const GENERATED_JOBS = 5

  const user = await auth.adminLogin()
  if (!(user && user.token)) {
    throw 'No session'
  }
  const jobService = JobService({ token: user.token })
  const jobs = await createJobs(jobService, GENERATED_JOBS)
  return {
    user,
    jobService,
    jobs
  }
}

const clearJobs = async (ctx: JobsContext) => {
  if (!ctx) {
    throw 'no context'
  }
  const jobsIds = ctx.jobs.map(job => job.id)
  const promises = jobsIds.map(id => ctx.jobService.delete(id))
  await Promise.all(promises)
}

export type JobsContext = PromiseValue<typeof setUpJobs>

export { setUpJobs, jobBuilder, clearJobs, jobErrorBuilder }
