import { useEffect, useMemo, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { createOrder, quoteOrder } from '../api/publicOrders'
import { ProductImage } from '../components/ProductImage'
import { useCart } from '../features/cart/cart-context'
import { buildCheckoutLines, completeOrderCreation, paymentMethodsFor, trackingStorageKey, validateDraft, validateReceipt, type CheckoutDraft } from '../features/checkout/checkout'
import { useStorefront } from '../features/storefront/storefront-context'
import type { CheckoutQuote, CreateOrderRequest, DeliveryType, PaymentMethod } from '../types/public'
import { formatMoney, toMinorUnits } from '../utils/money'

const paymentCopy: Record<PaymentMethod, { title: string; detail: string }> = {
  CASH: { title: 'Efectivo', detail: 'Pagas cuando llegue tu domicilio.' },
  PAY_ON_PICKUP: { title: 'Pago al recoger', detail: 'Pagas directamente en el restaurante.' },
  TRANSFER: { title: 'Transferencia', detail: 'Adjunta el comprobante para revisión.' },
}

export function CheckoutPage() {
  const cart = useCart(); const { data, loading, error: storefrontError, retry } = useStorefront(); const navigate = useNavigate()
  const [draft, setDraft] = useState<CheckoutDraft>({ name: '', phone: '', email: '', deliveryType: 'DELIVERY', address: '', neighborhood: '', reference: '', paymentMethod: 'CASH', cashTendered: '' })
  const [quote, setQuote] = useState<CheckoutQuote | null>(null); const [quoteError, setQuoteError] = useState(''); const [quoteAttempt, setQuoteAttempt] = useState(0)
  const [receipt, setReceipt] = useState<File | null>(null); const [error, setError] = useState(''); const [submitting, setSubmitting] = useState(false)
  const [acceptedPriceUpdate, setAcceptedPriceUpdate] = useState(false); const requestId = useRef(crypto.randomUUID())
  const receiptPreview = useMemo(() => receipt ? URL.createObjectURL(receipt) : null, [receipt])
  const currency = data?.business.currency ?? 'COP'

  useEffect(() => () => { if (receiptPreview) URL.revokeObjectURL(receiptPreview) }, [receiptPreview])
  useEffect(() => {
    if (cart.lines.length === 0) return
    const controller = new AbortController(); const timer = window.setTimeout(() => {
      setQuote(null); setQuoteError('')
      quoteOrder({ deliveryType: draft.deliveryType, lines: buildCheckoutLines(cart.lines, false) }, controller.signal)
        .then(setQuote).catch((reason: unknown) => { if (!(reason instanceof DOMException && reason.name === 'AbortError')) setQuoteError(reason instanceof Error ? reason.message : 'No pudimos actualizar el total.') })
    }, 180)
    return () => { window.clearTimeout(timer); controller.abort() }
  }, [cart.lines, draft.deliveryType, quoteAttempt])

  if (cart.lines.length === 0) return <main className="checkout-page"><section className="state-panel"><span>✓</span><p className="eyebrow">Checkout</p><h1>Tu carrito está vacío.</h1><p>Elige algo del menú antes de continuar.</p><Link className="button button--acid" to="/menu">Ver menú</Link></section></main>
  if (loading && !data) return <main className="checkout-page"><section className="state-panel" aria-live="polite"><span>•••</span><p className="eyebrow">Checkout</p><h1>Preparando tu pedido.</h1><p>Estamos cargando las formas de entrega y pago del restaurante.</p></section></main>
  if (!data) return <main className="checkout-page"><section className="state-panel" role="alert"><span>!</span><p className="eyebrow">Checkout</p><h1>No pudimos cargar la configuración.</h1><p>{storefrontError ?? 'Intenta nuevamente para continuar con tu pedido.'}</p><button className="button button--acid" type="button" onClick={retry}>Intentar de nuevo</button></section></main>

  const transfer = data.business.transfer ?? null
  const methods = paymentMethodsFor(draft.deliveryType, transfer?.configured === true)
  const setField = (field: keyof CheckoutDraft, value: string) => setDraft((current) => ({ ...current, [field]: value }))
  const selectDelivery = (type: DeliveryType) => { const paymentMethod = type === 'DELIVERY' ? 'CASH' : 'PAY_ON_PICKUP'; setDraft((current) => ({ ...current, deliveryType: type, paymentMethod })); setReceipt(null); setAcceptedPriceUpdate(false) }
  const selectPayment = (method: PaymentMethod) => { setDraft((current) => ({ ...current, paymentMethod: method })); if (method !== 'TRANSFER') setReceipt(null) }
  const chooseReceipt = (event: ChangeEvent<HTMLInputElement>) => { const file = event.target.files?.[0] ?? null; const issue = validateReceipt(file, false); if (issue) { setError(issue); event.target.value = ''; return } setError(''); setReceipt(file) }

  async function submit(event: FormEvent) {
    event.preventDefault(); setError('')
    const draftIssue = validateDraft(draft); if (draftIssue) { setError(draftIssue); return }
    const receiptIssue = validateReceipt(receipt, draft.paymentMethod === 'TRANSFER'); if (receiptIssue) { setError(receiptIssue); return }
    if (!quote) { setError('Espera mientras actualizamos el total.'); return }
    const cashTendered = draft.cashTendered ? Number(draft.cashTendered) : null
    if (draft.paymentMethod === 'CASH' && cashTendered !== null && cashTendered < quote.totals.total) { setError('El valor en efectivo no puede ser menor al total.'); return }
    const request: CreateOrderRequest = {
      clientRequestId: requestId.current,
      customer: { name: draft.name.trim(), phone: draft.phone.trim(), email: draft.email.trim() || null },
      delivery: { type: draft.deliveryType, address: draft.deliveryType === 'DELIVERY' ? draft.address.trim() : null, neighborhood: draft.deliveryType === 'DELIVERY' ? draft.neighborhood.trim() : null, reference: draft.deliveryType === 'DELIVERY' ? draft.reference.trim() || null : null },
      payment: { method: draft.paymentMethod, cashTendered }, lines: buildCheckoutLines(cart.lines, !acceptedPriceUpdate),
    }
    setSubmitting(true)
    try {
      await completeOrderCreation(() => createOrder(request, receipt), {
        storeTracking: (order) => sessionStorage.setItem(trackingStorageKey(order.publicNumber), order.trackingToken),
        clearCart: cart.clear,
        openConfirmation: (order) => navigate(`/order/${encodeURIComponent(order.publicNumber)}`, { replace: true }),
      })
    } catch (reason) {
      if (reason instanceof ApiError && reason.problem.code === 'PRICE_CHANGED' && reason.problem.currentQuote) {
        setQuote(reason.problem.currentQuote as CheckoutQuote); setAcceptedPriceUpdate(true); setError('Los precios cambiaron. Revisa el nuevo total y vuelve a confirmar.')
      } else setError(reason instanceof Error ? reason.message : 'No pudimos crear el pedido. Tu carrito sigue guardado.')
    } finally { setSubmitting(false) }
  }

  return <main className="checkout-page"><header className="checkout-heading"><p className="eyebrow">Paso final · Compra como invitado</p><h1>Que llegue<br /><i>lo bueno.</i></h1><div className="checkout-progress"><span className="done">1</span><b /><span className="done">2</span><b /><span>3</span></div></header><form className="checkout-grid" onSubmit={submit}><div className="checkout-form"><section className="checkout-block"><div className="block-number">01</div><div className="block-content"><p className="eyebrow">Datos del cliente</p><h2>¿A nombre de quién?</h2><div className="field-grid"><label className="field"><span>Nombre completo *</span><input value={draft.name} onChange={(event) => setField('name', event.target.value)} maxLength={120} autoComplete="name" required /></label><label className="field"><span>Teléfono *</span><input value={draft.phone} onChange={(event) => setField('phone', event.target.value)} maxLength={30} inputMode="tel" autoComplete="tel" required /></label><label className="field field--wide"><span>Correo <small>Opcional</small></span><input value={draft.email} onChange={(event) => setField('email', event.target.value)} maxLength={190} type="email" autoComplete="email" /></label></div></div></section><section className="checkout-block"><div className="block-number">02</div><div className="block-content"><p className="eyebrow">Tipo de entrega</p><h2>¿Te lo llevamos?</h2><div className="choice-grid"><button type="button" className={draft.deliveryType === 'DELIVERY' ? 'choice active' : 'choice'} onClick={() => selectDelivery('DELIVERY')}><span>↗</span><strong>Domicilio</strong><small>En aproximadamente {quote?.totals.estimatedMinutes ?? '—'} min</small></button><button type="button" className={draft.deliveryType === 'PICKUP' ? 'choice active' : 'choice'} onClick={() => selectDelivery('PICKUP')}><span>⌂</span><strong>Recoger</strong><small>En {data.business.estimatedPreparationMinutes} min</small></button></div>{draft.deliveryType === 'DELIVERY' ? <div className="field-grid conditional-fields"><label className="field field--wide"><span>Dirección *</span><input value={draft.address} onChange={(event) => setField('address', event.target.value)} maxLength={250} autoComplete="street-address" required /></label><label className="field"><span>Barrio *</span><input value={draft.neighborhood} onChange={(event) => setField('neighborhood', event.target.value)} maxLength={120} required /></label><label className="field"><span>Referencia <small>Opcional</small></span><input value={draft.reference} onChange={(event) => setField('reference', event.target.value)} maxLength={500} placeholder="Apto, torre, indicaciones…" /></label></div> : <div className="pickup-note"><span>⌖</span><div><strong>Recoge en Distrito Smash</strong><p>{data.business.address}</p></div></div>}</div></section><section className="checkout-block"><div className="block-number">03</div><div className="block-content"><p className="eyebrow">Forma de pago</p><h2>Elige cómo pagar.</h2><div className="payment-options">{methods.map((method) => <button type="button" key={method} className={draft.paymentMethod === method ? 'payment-choice active' : 'payment-choice'} onClick={() => selectPayment(method)}><span className="radio" /><div><strong>{paymentCopy[method].title}</strong><small>{paymentCopy[method].detail}</small></div></button>)}</div>{draft.paymentMethod === 'CASH' && <label className="field cash-field"><span>¿Con cuánto pagas? <small>Opcional</small></span><input value={draft.cashTendered} onChange={(event) => setField('cashTendered', event.target.value)} inputMode="numeric" type="number" min="0" step="100" placeholder="Ej. 50000" /></label>}{draft.paymentMethod === 'TRANSFER' && transfer && <div className="transfer-panel"><div className="transfer-data">{transfer.qrPath && <img src={transfer.qrPath} alt="QR configurado para transferencia" />}<div><small>Transfiere exactamente</small><strong>{quote ? formatMoney(toMinorUnits(quote.totals.total), currency) : 'Calculando…'}</strong><p>{transfer.provider}</p><p>{transfer.accountHolder}</p><p>{transfer.accountReference}</p></div></div><div className="receipt-field">{receiptPreview ? <div className="receipt-preview"><img src={receiptPreview} alt="Vista previa del comprobante" /><div><strong>{receipt?.name}</strong><button type="button" className="text-button" onClick={() => setReceipt(null)}>Eliminar</button></div></div> : <label className="receipt-drop"><input type="file" accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp" onChange={chooseReceipt} /><span>↑</span><strong>Adjunta el comprobante</strong><small>JPG, PNG o WEBP · máximo 5 MB</small></label>}</div></div>}</div></section></div><CheckoutSummary quote={quote} error={error} quoteError={quoteError} retryQuote={() => setQuoteAttempt((value) => value + 1)} submitting={submitting} currency={currency} /></form></main>
}

export function CheckoutSummary({ quote, error, quoteError, retryQuote, submitting, currency }: { quote: CheckoutQuote | null; error: string; quoteError: string; retryQuote: () => void; submitting: boolean; currency: string }) {
  const cart = useCart()
  return <aside className="checkout-summary"><p className="eyebrow">Tu pedido</p><h2>{cart.units} {cart.units === 1 ? 'producto' : 'productos'}</h2><div className="checkout-summary__lines">{cart.lines.map((line) => <article key={line.id}><ProductImage src={line.imagePath} alt="" /><div><strong>{line.quantity} × {line.name}</strong>{line.extras.length > 0 && <small>+ {line.extras.map((extra) => extra.name).join(', ')}</small>}{line.notes && <small>“{line.notes}”</small>}</div></article>)}</div><div className="totals">{quote ? <><div><span>Subtotal</span><strong>{formatMoney(toMinorUnits(quote.totals.subtotal), currency)}</strong></div>{quote.totals.discount > 0 && <div className="discount"><span>Descuento</span><strong>− {formatMoney(toMinorUnits(quote.totals.discount), currency)}</strong></div>}<div><span>Domicilio</span><strong>{quote.totals.deliveryFee === 0 ? 'Gratis' : formatMoney(toMinorUnits(quote.totals.deliveryFee), currency)}</strong></div><hr /><div className="grand-total"><span>Total</span><strong>{formatMoney(toMinorUnits(quote.totals.total), currency)}</strong></div><small>Calculado nuevamente por el restaurante.</small></> : quoteError ? <p>No pudimos actualizar los precios.</p> : <p>Actualizando precios…</p>}</div>{(error || quoteError) && <div className="checkout-error" role="alert">! {error || quoteError}</div>}{quoteError && <button className="button button--light button--wide" type="button" onClick={retryQuote}>Reintentar precios</button>}<button className="button button--acid button--wide checkout-submit" disabled={submitting || !quote}>{submitting ? 'Creando pedido…' : 'Confirmar pedido →'}</button><p className="secure-note">El pedido solo se crea una vez, incluso si reintentas.</p></aside>
}
