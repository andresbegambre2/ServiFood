import { useState } from 'react'
interface Props { src: string | null; alt: string; className?: string }
export function ProductImage({ src, alt, className }: Props) { const [failed, setFailed] = useState(!src); return failed ? <div className={`image-fallback ${className ?? ''}`} role="img" aria-label={`Imagen no disponible para ${alt}`}><span>DS</span><small>Hecho al fuego</small></div> : <img className={className} src={src ?? ''} alt={alt} loading="lazy" onError={() => setFailed(true)} /> }
