import { Component, type ErrorInfo, type ReactNode } from 'react'

interface Props { children: ReactNode }
interface State { failed: boolean }

export class GlobalErrorBoundary extends Component<Props, State> {
  state: State = { failed: false }

  static getDerivedStateFromError(): State { return { failed: true } }

  componentDidCatch(_error: Error, _info: ErrorInfo) {
    // The fallback is intentionally silent: production logs must not expose cart,
    // checkout or customer data through uncensored browser error payloads.
  }

  render() {
    return this.state.failed ? <GlobalErrorFallback /> : this.props.children
  }
}

export function GlobalErrorFallback() {
  return <main className="fatal-error" role="alert">
    <section className="state-panel">
      <span aria-hidden="true">!</span>
      <p className="eyebrow">ServiFood</p>
      <h1>Algo salió mal.</h1>
      <p>La aplicación encontró un problema inesperado. Tu pedido guardado no se ha eliminado.</p>
      <div className="fatal-error__actions">
        <button className="button button--acid" type="button" onClick={() => window.location.reload()}>Intentar de nuevo</button>
        <a className="button button--light" href="/">Volver al inicio</a>
      </div>
    </section>
  </main>
}
