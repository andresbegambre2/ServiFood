import { Link } from 'react-router-dom'
export function NotFoundPage() { return <main className="page"><section className="state-panel"><span>404</span><h1>Esta mesa no existe.</h1><p>Volvamos a lo importante: comer bien.</p><Link className="button button--acid" to="/">Ir al inicio</Link></section></main> }
