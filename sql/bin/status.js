'use strict'

/* eslint-disable no-console, no-process-exit, no-process-env */

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
  console.log(`URL: ${env.DB_SHORT_URL}`);
}

run()
  .then(() => process.exit(0))
  .catch(() => process.exit(1));