import { useEffect, useRef, type MouseEvent, type ReactNode } from 'react'

const focusableSelector = [
  'a[href]', 'button:not([disabled])', 'input:not([disabled])',
  'select:not([disabled])', 'textarea:not([disabled])', '[tabindex]:not([tabindex="-1"])',
].join(',')

interface Props {
  children: ReactNode
  labelledBy?: string
  label?: string
  onClose: () => void
  variant?: 'form' | 'receipt'
}

export function AccessibleModal({ children, labelledBy, label, onClose, variant = 'form' }: Props) {
  const dialogRef = useRef<HTMLDivElement>(null)
  const openerRef = useRef<HTMLElement | null>(null)
  const closeRef = useRef(onClose)

  useEffect(() => { closeRef.current = onClose }, [onClose])

  useEffect(() => {
    openerRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const dialog = dialogRef.current
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const focusable = dialog?.querySelectorAll<HTMLElement>(focusableSelector)
    const preferred = dialog?.querySelector<HTMLElement>('[autofocus]')
    if (preferred) preferred.focus(); else focusable?.[0]?.focus()

    const keydown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') { event.preventDefault(); closeRef.current(); return }
      if (event.key !== 'Tab' || !dialog) return
      const items = [...dialog.querySelectorAll<HTMLElement>(focusableSelector)]
      if (!items.length) { event.preventDefault(); dialog.focus(); return }
      const first = items[0]; const last = items[items.length - 1]
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
    }
    document.addEventListener('keydown', keydown)
    return () => {
      document.removeEventListener('keydown', keydown)
      document.body.style.overflow = previousOverflow
      if (openerRef.current?.isConnected) openerRef.current.focus()
    }
  }, [])

  const closeBackdrop = (event: MouseEvent<HTMLDivElement>) => { if (event.target === event.currentTarget) onClose() }
  return <div ref={dialogRef} className={variant === 'receipt' ? 'receipt-modal' : 'admin-modal'} role="dialog" aria-modal="true"
    aria-labelledby={labelledBy} aria-label={labelledBy ? undefined : label} tabIndex={-1} onMouseDown={closeBackdrop}>
    {children}
  </div>
}
