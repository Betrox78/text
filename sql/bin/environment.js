'use strict'

/* eslint-disable no-console, no-process-exit, no-process-env */

const dotenv = require('dotenv');
const path = require('path');
const argparse = require('argparse');
const project = require(path.join('..', 'package.json'));

const parser = new argparse.ArgumentParser({
  version: project.version,
  addHelp: true,
  description: project.description,
});

parser.addArgument(['-e', '--env'], {
  help: 'Environment',
  type: 'string',
  dest: 'DB_ENV',
  defaultValue: process.env.NODE_ENV || 'development',
});

parser.addArgument(['-t', '--target'], {
  help: 'Version target',
  type: 'int',
  dest: 'DB_TARGET',
  defaultValue: process.env.NODE_TARGET || -1,
});

process.env = { ...process.env, ...parser.parseArgs() };

const variables = [
  "DB_DRIVER",
  "DB_HOST",
  "DB_PORT",
  "DB_USER",
  "DB_PASSWORD",
  "DB_NAME",
]

const NODE_ENV = process.env.NODE_ENV || 'development';

let file;

switch (NODE_ENV) {
  case 'staging':
  case 'production':
    file = `.env.${NODE_ENV}`;
    break;
  default:
    file = '.env';
}

dotenv.config({
  path: path.join(__dirname, '..', file),
});

const notSet = variables.find(v => !process.env[v]);
if (notSet) {
  console.error(`${notSet} is required`);
  process.exit(1);
}

const env = process.env;

let DB_URL = `${env.DB_DRIVER}://${env.DB_USER}:${env.DB_PASSWORD}@${env.DB_HOST}:${env.DB_PORT}/${env.DB_NAME}`;
let DB_SHORT_URL = `${env.DB_DRIVER}://${env.DB_HOST}:${env.DB_PORT}/${env.DB_NAME}`;

if (env.DB_SSL) {
  DB_URL = `${DB_URL}?ssl=true`;
}

const DB_DRIVER = `${env.DB_DRIVER}`;
const DB_TARGET = env.DB_TARGET;

module.exports = {
  DB_URL,
  DB_SHORT_URL,
  DB_DRIVER,
  DB_TARGET
};