import { adminDownload, adminRequest } from './admin'
import type { AnalyticsOverview, ReportData, ReportType } from '../types/admin'

const query = (from: string, to: string) => `?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
export const getAnalytics = (from: string, to: string) => adminRequest<AnalyticsOverview>(`/analytics${query(from, to)}`)
export const getReport = (type: ReportType, from: string, to: string) => adminRequest<ReportData>(`/reports/${type}${query(from, to)}`)
export async function downloadReport(type: Exclude<ReportType, 'PROMOTIONS' | 'PAYMENTS'>, from: string, to: string) {
  const blob = await adminDownload(`/reports/${type}/csv${query(from, to)}`); const url = URL.createObjectURL(blob); const link = document.createElement('a')
  link.href = url; link.download = `servifood-${type.toLowerCase()}-${from}-${to}.csv`; link.click(); URL.revokeObjectURL(url)
}
