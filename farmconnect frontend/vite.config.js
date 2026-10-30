import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const apiBaseUrl = process.env.VITE_API_BASE_URL ?? '/api'
let apiConnectOrigin = "'self'"
try {
  apiConnectOrigin = new URL(apiBaseUrl).origin
} catch {
  // Relative API URLs use the frontend's own origin.
}

const securityHeaders = {
  'X-Content-Type-Options': 'nosniff',
  'X-Frame-Options': 'DENY',
  'Referrer-Policy': 'strict-origin-when-cross-origin',
  'Permissions-Policy': 'camera=(), microphone=(), geolocation=(self)',
  'Content-Security-Policy': `default-src 'self'; base-uri 'self'; object-src 'none'; form-action 'self' https://api.razorpay.com; script-src 'self' https://checkout.razorpay.com; style-src 'self' 'unsafe-inline'; img-src 'self' data: https://images.unsplash.com https://media.assettype.com https://images.squarespace-cdn.com https://cdn.shopify.com https://hariomatta.com https://dgikh81ssvyrj.cloudfront.net https://images.livemint.com; connect-src 'self' ws: ${apiConnectOrigin} https://api.razorpay.com https://*.razorpay.com; frame-src https://api.razorpay.com https://checkout.razorpay.com;`,
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    headers: securityHeaders,
    // React Fast Refresh relies on an inline preamble that the app's CSP blocks.
    hmr: false,
    proxy: { '/api': 'http://localhost:8081' },
  },
  preview: { headers: securityHeaders },
})
