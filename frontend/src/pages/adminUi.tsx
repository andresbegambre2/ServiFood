import { label } from './adminFormat'
export function StatusBadge({ value }: { value?: string }) { return <span className={`status status-${value?.toLowerCase() ?? 'none'}`}><i />{label(value)}</span> }
export function AdminLoading({ label: text }: { label: string }) { return <div className="admin-state"><div className="admin-loader" /><p>{text}</p></div> }
export function AdminError({ message, retry }: { message: string; retry(): void }) { return <div className="admin-state"><span className="state-icon">!</span><h2>No pudimos cargar esta sección</h2><p>{message}</p><button className="secondary" onClick={retry}>Reintentar</button></div> }
export function EmptyState({ text }: { text: string }) { return <div className="empty-state"><span>◇</span><p>{text}</p></div> }
