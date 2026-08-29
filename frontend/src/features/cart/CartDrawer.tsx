import { useCallback, useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { useCart } from './cart-context'
import { formatMoney } from '../../utils/money'
import { ProductImage } from '../../components/ProductImage'
import { QuantityControl } from '../../components/QuantityControl'

export function CartDrawer() {
  const cart = useCart(); const drawerRef = useRef<HTMLElement>(null); const closeButtonRef = useRef<HTMLButtonElement>(null); const openerRef = useRef<HTMLElement | null>(null); const closeRef = useRef(cart.close)
  const closeDrawer = useCallback(() => { closeRef.current() }, [])
  useEffect(() => { closeRef.current = cart.close }, [cart.close])
  useEffect(() => {
    if (!cart.isOpen) return
    openerRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null
    closeButtonRef.current?.focus()
    const drawer = drawerRef.current
    const siblings = drawer?.parentElement ? [...drawer.parentElement.children].filter(element => element !== drawer && !element.classList.contains('drawer-backdrop')) as HTMLElement[] : []
    const previousInert = siblings.map(element => element.inert)
    siblings.forEach(element => { element.inert = true })
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') { event.preventDefault(); closeDrawer(); return }
      if (event.key !== 'Tab' || !drawer) return
      const focusable = [...drawer.querySelectorAll<HTMLElement>('a[href], button:not([disabled]), input:not([disabled]), [tabindex]:not([tabindex="-1"])')]
      if (!focusable.length) { event.preventDefault(); drawer.focus(); return }
      const first = focusable[0]; const last = focusable[focusable.length - 1]
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      siblings.forEach((element, index) => { element.inert = previousInert[index] })
      if (openerRef.current?.isConnected) openerRef.current.focus()
    }
  }, [cart.isOpen, closeDrawer])

  return <>
    {cart.isOpen && <button className="drawer-backdrop" tabIndex={-1} aria-label="Cerrar carrito" onClick={closeDrawer} />}
    <aside ref={drawerRef} className={`cart-drawer ${cart.isOpen ? 'is-open' : ''}`} inert={!cart.isOpen} role="dialog" aria-modal={cart.isOpen ? true : undefined} aria-labelledby="cart-drawer-title" tabIndex={-1}>
      <div className="drawer-head"><div><p className="eyebrow">Tu selección</p><h2 id="cart-drawer-title">Tu pedido <sup>{cart.units}</sup></h2></div><button ref={closeButtonRef} className="icon-button" onClick={closeDrawer} aria-label="Cerrar">×</button></div>
      {cart.lines.length === 0 ? <div className="cart-empty"><span>+</span><h3>Empieza con algo brutal</h3><p>Explora el menú y personaliza tu primera elección.</p><Link className="button button--acid" to="/menu" onClick={closeDrawer}>Ver menú</Link></div> : <><div className="cart-lines">{cart.lines.map((line) => <article className="cart-line" key={line.id}><ProductImage src={line.imagePath} alt={line.name} /><div><div className="cart-line__title"><h3>{line.name}</h3><button onClick={() => cart.remove(line.id)} aria-label={`Eliminar ${line.name}`}>×</button></div>{line.extras.length > 0 && <p>+ {line.extras.map((extra) => extra.name).join(', ')}</p>}{line.notes && <p>“{line.notes}”</p>}<div className="cart-line__bottom"><QuantityControl value={line.quantity} onChange={(quantity) => cart.setQuantity(line.id, quantity)} /><strong>{formatMoney((line.unitPriceMinor + line.extras.reduce((sum, extra) => sum + extra.unitPriceMinor, 0)) * line.quantity)}</strong></div></div></article>)}</div><div className="drawer-total"><button className="text-button" onClick={cart.clear}>Vaciar pedido</button><div><span>Subtotal</span><strong>{formatMoney(cart.subtotal)}</strong></div><p>Los precios serán verificados al continuar.</p><Link className="button button--acid button--wide" to="/checkout" onClick={closeDrawer}>Continuar pedido <span>→</span></Link></div></>}
    </aside>
  </>
}
