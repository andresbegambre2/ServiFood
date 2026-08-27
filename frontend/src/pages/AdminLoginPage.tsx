import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAdminAuth } from '../features/admin/admin-auth-context'

export function AdminLoginPage() {
  const { user, loading, login, message } = useAdminAuth()
  const navigate = useNavigate(); const location = useLocation()
  const [email, setEmail] = useState(''); const [password, setPassword] = useState(''); const [error, setError] = useState(''); const [sending, setSending] = useState(false)
  if (!loading && user) return <Navigate to={user.role === 'KITCHEN' ? '/kitchen' : '/admin'} replace />
  async function submit(event: FormEvent) {
    event.preventDefault(); setSending(true); setError('')
    try { const loggedUser = await login(email, password); const from = (location.state as { from?: string } | null)?.from; const destination = loggedUser.role === 'KITCHEN' ? '/kitchen' : from?.startsWith('/kitchen') || from?.startsWith('/admin') ? from : '/admin'; navigate(destination, { replace: true }) }
    catch (cause) { setError(cause instanceof ApiError && cause.status === 401 ? 'Correo o contraseña incorrectos.' : 'No pudimos iniciar sesión. Intenta de nuevo.') }
    finally { setSending(false) }
  }
  return <main className="admin-login">
    <section className="login-card"><div className="login-mark">SF</div><p className="eyebrow">SERVIFOOD · OPERACIONES</p><h1>Bienvenido de vuelta</h1><p>Ingresa con tu cuenta interna para administrar el restaurante.</p>
      <form onSubmit={submit}><label>Correo electrónico<input type="email" value={email} onChange={e => setEmail(e.target.value)} autoComplete="username" required /></label><label>Contraseña<input type="password" value={password} onChange={e => setPassword(e.target.value)} autoComplete="current-password" required /></label>
        {(error || message) && <div className="admin-alert" role="alert">{error || message}</div>}<button className="primary" disabled={sending}>{sending ? 'Ingresando…' : 'Ingresar al panel'}</button></form>
      <small>Acceso exclusivo para personal autorizado</small></section>
  </main>
}
