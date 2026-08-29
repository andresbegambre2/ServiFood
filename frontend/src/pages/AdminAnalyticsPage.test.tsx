import { renderToString } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { AnalyticsOverview } from '../types/admin'
import { AdminAnalyticsPage, AnalyticsView, BarChart } from './AdminAnalyticsPage'
import { AdminReportsPage } from './AdminReportsPage'

const period = (label: string, sales: number, changePercentage: number) => ({ label, from: '2026-08-27', to: '2026-08-27', sales, previousSales: 50000, changePercentage, orders: 2 })
const data: AnalyticsOverview = {
  today: period('Hoy', 30000, -40), yesterday: period('Ayer', 50000, 25), week: period('Esta semana', 120000, 20), month: period('Este mes', 450000, 12),
  totalOrders: 12, cancelledOrders: 2, averageTicket: 37500, discountsApplied: 18000, couponUses: 3, pointsEarned: 90, pointsRedeemed: 25,
  lowStockIngredients: 2, outOfStockIngredients: 1,
  topProducts: [{ label: 'Doble Bacon', quantity: 8, amount: 256000 }], topCategories: [{ label: 'Hamburguesas', quantity: 10, amount: 300000 }],
  peakHours: [{ label: '19:00', quantity: 5, amount: 180000 }], paymentMethods: [{ label: 'Transferencia', quantity: 7, amount: 250000 }],
  deliveryTypes: [{ label: 'Domicilio', quantity: 9, amount: 330000 }], topCustomers: [{ customerId: 1, name: 'Ana Cliente', orders: 4, amount: 160000 }],
  salesEvolution: [{ date: '2026-08-26', sales: 50000, orders: 2 }, { date: '2026-08-27', sales: 70000, orders: 3 }],
}

describe('business analytics views', () => {
  it('renders comparable metrics and only data-driven charts', () => {
    const html = renderToString(<AnalyticsView data={data} />)
    expect(html).toContain('Evolución de ventas')
    expect(html).toContain('vs. período anterior')
    expect(html).toContain('Evolución de ventas por día')
    expect(html).toContain('Doble Bacon')
    expect(html).toContain('Transferencia')
    expect(html).toContain('Ana Cliente')
    expect(html).toContain('ingredientes agotados')
  })

  it('shows an explicit empty state instead of decorative bars without data', () => {
    expect(renderToString(<BarChart values={[]} value="quantity" />)).toContain('Sin datos para graficar')
  })

  it('exposes date filters, report types and CSV export in Spanish', () => {
    const analytics = renderToString(<AdminAnalyticsPage />); const reports = renderToString(<AdminReportsPage />)
    expect(analytics).toContain('Desde'); expect(analytics).toContain('Hasta'); expect(analytics).toContain('Aplicar rango')
    expect(reports).toContain('Ventas'); expect(reports).toContain('Pedidos'); expect(reports).toContain('Cupones'); expect(reports).toContain('Exportar CSV')
  })
})
