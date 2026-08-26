import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { CartProvider } from './features/cart/CartProvider'
import { StorefrontProvider } from './features/storefront/StorefrontProvider'
import { PublicLayout } from './layouts/PublicLayout'
import { HomePage } from './pages/HomePage'
import { MenuPage } from './pages/MenuPage'
import { ProductPage } from './pages/ProductPage'
import { CartPage } from './pages/CartPage'
import { CheckoutPage } from './pages/CheckoutPage'
import { OrderConfirmationPage } from './pages/OrderConfirmationPage'
import { NotFoundPage } from './pages/NotFoundPage'
import './styles/storefront.css'

export default function App() { return <BrowserRouter><StorefrontProvider><CartProvider><Routes><Route element={<PublicLayout />}><Route index element={<HomePage />} /><Route path="menu" element={<MenuPage />} /><Route path="menu/:slug" element={<ProductPage />} /><Route path="cart" element={<CartPage />} /><Route path="checkout" element={<CheckoutPage />} /><Route path="order/:publicNumber" element={<OrderConfirmationPage />} /><Route path="*" element={<NotFoundPage />} /></Route></Routes></CartProvider></StorefrontProvider></BrowserRouter> }
