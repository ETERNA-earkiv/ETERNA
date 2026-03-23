import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import { getCurrentUser } from '../api/auth'
import { useAuth } from '../components/AuthProvider'

export function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { setUser } = useAuth()

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login({ username, password })
      const user = await getCurrentUser()
      setUser(user)
      navigate('/browse')
    } catch {
      setError('Inloggningen misslyckades. Kontrollera användarnamn och lösenord.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'var(--digi-color-background, #f5f5f5)',
        padding: '1rem',
      }}
    >
      <div
        style={{
          background: 'white',
          borderRadius: '4px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
          padding: '2.5rem',
          width: '100%',
          maxWidth: '400px',
        }}
      >
        <h1 style={{ marginBottom: '0.25rem', fontSize: '1.5rem' }}>ETERNA</h1>
        <p style={{ marginBottom: '2rem', color: '#555' }}>
          Digitalt bevaringsarkiv
        </p>

        <form onSubmit={handleSubmit} noValidate>
          <div style={{ marginBottom: '1.25rem' }}>
            <digi-form-input
              af-label="Användarnamn"
              af-id="username"
              af-type="text"
              af-required={true}
              af-value={username}
              onAfOnInputChange={(e: CustomEvent) =>
                setUsername((e.detail as { value: string }).value)
              }
            ></digi-form-input>
          </div>

          <div style={{ marginBottom: '1.5rem' }}>
            <digi-form-input
              af-label="Lösenord"
              af-id="password"
              af-type="password"
              af-required={true}
              af-value={password}
              onAfOnInputChange={(e: CustomEvent) =>
                setPassword((e.detail as { value: string }).value)
              }
            ></digi-form-input>
          </div>

          {error && (
            <digi-message
              af-type="error"
              style={{ marginBottom: '1rem', display: 'block' }}
            >
              {error}
            </digi-message>
          )}

          <digi-button
            af-variation="primary"
            af-type="submit"
            af-disabled={loading}
            style={{ width: '100%' }}
          >
            {loading ? 'Loggar in…' : 'Logga in'}
          </digi-button>
        </form>
      </div>
    </div>
  )
}
