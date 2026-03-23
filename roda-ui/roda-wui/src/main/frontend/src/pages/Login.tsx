import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import { useAuth } from '../components/AuthProvider'
import { DigiFormInput, DigiButton, DigiNotificationAlert } from '@designsystem-se/af-react'
import { FormInputType } from '@designsystem-se/af'

type InputEl = HTMLElement & { afValue: string }

export function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { setUser } = useAuth()

  async function handleLogin() {
    setError('')
    setLoading(true)
    try {
      const user = await login(username, password)
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
        <p style={{ marginBottom: '2rem', color: '#555' }}>Digitalt bevaringsarkiv</p>

        <div style={{ marginBottom: '1.25rem' }}>
          <DigiFormInput
            afLabel="Användarnamn"
            afId="username"
            afType={FormInputType.TEXT}
            afRequired={true}
            afValue={username}
            onAfOnInput={(e) => setUsername((e.target as InputEl).afValue)}
            onAfOnKeyup={(e) => {
              if ((e.detail as KeyboardEvent).key === 'Enter') handleLogin()
            }}
          />
        </div>

        <div style={{ marginBottom: '1.5rem' }}>
          <DigiFormInput
            afLabel="Lösenord"
            afId="password"
            afType={FormInputType.PASSWORD}
            afRequired={true}
            afValue={password}
            onAfOnInput={(e) => setPassword((e.target as InputEl).afValue)}
            onAfOnKeyup={(e) => {
              if ((e.detail as KeyboardEvent).key === 'Enter') handleLogin()
            }}
          />
        </div>

        {error && (
          <DigiNotificationAlert
            afVariation="danger"
            afHeading="Inloggningen misslyckades"
            style={{ marginBottom: '1rem', display: 'block' }}
          >
            {error}
          </DigiNotificationAlert>
        )}

        <DigiButton
          afVariation="primary"
          onAfOnClick={handleLogin}
          style={{ width: '100%' }}
        >
          {loading ? 'Loggar in…' : 'Logga in'}
        </DigiButton>
      </div>
    </div>
  )
}
