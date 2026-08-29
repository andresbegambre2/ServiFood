import type { ReportType } from '../types/admin'
import { money } from '../utils/money'

const labels: Record<string, string> = { NEW: 'Nuevo', CONFIRMED: 'Confirmado', PREPARING: 'Preparando', READY: 'Listo', ON_THE_WAY: 'En camino', DELIVERED: 'Entregado', CANCELLED: 'Cancelado', PENDING: 'Pendiente', UNDER_REVIEW: 'Por revisar', APPROVED: 'Aprobado', REJECTED: 'Rechazado', DELIVERY: 'Domicilio', PICKUP: 'Recoger', TRANSFER: 'Transferencia', CASH: 'Efectivo', PAY_ON_PICKUP: 'Pago al recoger', PERCENTAGE: 'Porcentaje', FIXED_AMOUNT: 'Valor fijo', true: 'Sí', false: 'No', ADMIN: 'Administración', CASHIER: 'Caja', KITCHEN: 'Cocina' }
export const label = (value?: string) => value ? labels[value] ?? value : 'Sin registrar'
export const time = (value: string) => new Intl.DateTimeFormat('es-CO', { hour: 'numeric', minute: '2-digit' }).format(new Date(value))
export const dateTime = (value?: string) => value ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : 'Pendiente'
export const quantityLabel = (value: number, singular: string, plural: string) => `${value} ${value === 1 ? singular : plural}`
export function formatReportCell(value: unknown, column = '', type?: ReportType) {
  if (value === null || value === undefined) return '—'
  const text = String(value); const translated = labels[text]
  if (translated) return translated
  if (['Fecha', 'Inicio', 'Fin'].includes(column)) {
    const date = new Date(text)
    if (!Number.isNaN(date.getTime())) return new Intl.DateTimeFormat('es-CO', type === 'SALES' ? { dateStyle: 'medium', timeZone: 'America/Bogota' } : { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Bogota' }).format(date)
  }
  if (['Ventas', 'Ticket promedio', 'Descuentos', 'Total', 'Descuento', 'Gasto total', 'Descuento total'].includes(column) || (column === 'Valor' && type === 'PAYMENTS')) return money(Number(value))
  return text
}
