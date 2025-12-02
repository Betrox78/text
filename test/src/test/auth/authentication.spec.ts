import assert from 'assert'
import authentication from 'api/authentication'
import { adminLogin } from '.'

describe('AUTH', function() {
  it('Login [OK]', async done => {
    try {
      await adminLogin()
      done()
    } catch (error) {
      done(error)
    }
  })

  it('Login [ERROR]', async done => {
    try {
      const { body } = await authentication.login({
        email: 'random@domain.com',
        pass: '12345678',
        module: 'admin'
      })
      assert.equal(
        body.status,
        'ERROR',
        `Login successful: ${JSON.stringify(body)}`
      )
      done()
    } catch (error) {
      done(error)
    }
  })
})
