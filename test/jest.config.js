module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  rootDir: 'src',
  testRunner: 'jest-circus/runner',
  moduleNameMapper: {
    '^api/(.*)$': '<rootDir>/api/$1',
    '^lib/(.*)$': '<rootDir>/lib/$1',
    '^test/(.*)$': '<rootDir>/test/$1'
  }
}
