import dotenv from 'dotenv'
import path from 'path'
import argparse from 'argparse'

const parser = new argparse.ArgumentParser({
  version: '1.0.0',
  addHelp: true,
  description: 'Abordo API testing'
})

parser.addArgument(['-e', '--env'], {
  help: 'Environment',
  type: 'string',
  dest: 'DB_ENV',
  defaultValue: process.env.NODE_ENV || 'development'
})

const [args] = parser.parseKnownArgs()

process.env = { ...process.env, ...args }

const variables = [
  'ABORDO_POS_USER',
  'ABORDO_POS_PASSWORD',
  'API_HOST_URL',
  'ABORDO_ADMIN_USER',
  'ABORDO_ADMIN_PASSWORD'
]

const NODE_ENV = process.env.NODE_ENV || 'development'
let file: string

switch (NODE_ENV) {
  case 'test':
  case 'staging':
  case 'development':
  case 'production':
    file = `.env.${NODE_ENV}`
    break
  default:
    file = '.env'
}

dotenv.config({
  path: path.join(process.env.PWD, file)
})
const notSet = variables.find(v => !process.env[v])
if (notSet) {
  console.error(`${notSet} is required`)
}

const appArguments = {
  abordoPosUser: process.env.ABORDO_POS_USER,
  abordoPosPassword: process.env.ABORDO_POS_PASSWORD,
  apiHostUrl: process.env.API_HOST_URL,
  abordoAdminUser: process.env.ABORDO_ADMIN_USER,
  abordoAdminPassword: process.env.ABORDO_ADMIN_PASSWORD
}

export type AppArguments = typeof appArguments

export default appArguments
