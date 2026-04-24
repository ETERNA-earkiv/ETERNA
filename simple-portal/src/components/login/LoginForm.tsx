import { useRef, useState } from 'react';
import { login } from '@lib/api/auth';
import { PortalInput } from '../portal-ui/PortalInput';
import { PortalButton } from '../portal-ui/PortalButton';
import { PortalAlert } from '../portal-ui/PortalAlert';
import { PortalLink } from '../portal-ui/PortalLink';
import './LoginForm.css';

export function LoginForm() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const formRef = useRef<HTMLFormElement>(null);

  const params = new URLSearchParams(window.location.search);
  const sessionExpired = params.get('expired') === '1';
  const returnUrl = params.get('returnUrl') || '/sok';

  async function handleLogin() {
    if (!username || !password) {
      setErrorMessage('Fyll i både användarnamn och lösenord.');
      return;
    }

    setLoading(true);
    setErrorMessage('');

    try {
      const success = await login(username, password);
      setPassword('');
      if (success) {
        const safe = returnUrl.startsWith('/') && !returnUrl.startsWith('//') ? returnUrl : '/sok';
        window.location.href = safe;
      } else {
        setErrorMessage('Fel användarnamn eller lösenord.');
      }
    } catch {
      setPassword('');
      setErrorMessage('Kunde inte ansluta till servern.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-header">
          <svg className="login-lock-icon" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect width="18" height="11" x="3" y="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
          <h1>Logga in</h1>
          <p>Logga in med ditt RODA-konto.</p>
        </div>

        <form
          ref={formRef}
          onSubmit={(e) => { e.preventDefault(); handleLogin(); }}
          className="login-form"
        >
          {sessionExpired && (
            <PortalAlert variant="warning" size="small">
              Din session har gått ut. Logga in igen för att fortsätta.
            </PortalAlert>
          )}

          {errorMessage && (
            <PortalAlert variant="danger" size="small">
              {errorMessage}
            </PortalAlert>
          )}

          <PortalInput
            label="Användarnamn"
            type="text"
            value={username}
            onChange={(val) => setUsername(val)}
            autoComplete="username"
            required
          />

          <PortalInput
            label="Lösenord"
            type="password"
            value={password}
            onChange={(val) => setPassword(val)}
            autoComplete="current-password"
            required
          />

          <PortalButton
            onClick={handleLogin}
            disabled={loading}
            loading={loading}
            fullWidth
          >
            Logga in
          </PortalButton>
        </form>

        <div className="login-back">
          <PortalLink href="/sok">Tillbaka till portalen</PortalLink>
        </div>
      </div>
    </div>
  );
}
