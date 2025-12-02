'use strict'

/* eslint-disable no-console, no-process-exit, no-process-env */

const migrate = require('db-migrator/lib/migrate');
const status = require('db-migrator/lib/status');
const env = require('./environment');

const options = {
  connectionString: env.DB_URL,
  path: './migrations',
  tableName: 'migrations',
  dbDriver: env.DB_DRIVER,
};

async function run() {
  await status(options);
  await migrate(options);
  await status(options);
}

run()
  .then(() => process.exit(0))
  .catch(() => process.exit(1));