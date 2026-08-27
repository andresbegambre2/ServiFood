export const toMinorUnits = (value: number) => Math.round(value * 100)
export const fromMinorUnits = (value: number) => value / 100
export const formatMoney = (minorUnits: number, currency = 'COP') => new Intl.NumberFormat('es-CO', { style: 'currency', currency, maximumFractionDigits: currency === 'COP' ? 0 : 2 }).format(fromMinorUnits(minorUnits))
export const money = (value: number, currency = 'COP') => formatMoney(toMinorUnits(value), currency)
