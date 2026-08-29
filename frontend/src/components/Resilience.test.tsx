import { renderToString } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { AccessibleModal } from './AccessibleModal'
import { GlobalErrorFallback } from './GlobalErrorBoundary'

describe('global interface resilience', () => {
  it('renders a recoverable Spanish fallback for unexpected errors', () => {
    const html = renderToString(<GlobalErrorFallback />)
    expect(html).toContain('Algo salió mal')
    expect(html).toContain('Intentar de nuevo')
    expect(html).toContain('Volver al inicio')
  })

  it('exposes administrative overlays as labelled modal dialogs', () => {
    const html = renderToString(<AccessibleModal labelledBy="dialog-title" onClose={() => undefined}><section><h2 id="dialog-title">Editar</h2><button>Cerrar</button></section></AccessibleModal>)
    expect(html).toContain('role="dialog"')
    expect(html).toContain('aria-modal="true"')
    expect(html).toContain('aria-labelledby="dialog-title"')
  })
})
