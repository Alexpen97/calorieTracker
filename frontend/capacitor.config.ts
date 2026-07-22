import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.nutritrack.app',
  appName: 'NutriTrack',
  webDir: 'dist',
  server: {
    // https://localhost origin for CORS + secure cookies-compatible scheme
    androidScheme: 'https',
  },
  android: {
    allowMixedContent: false,
  },
  plugins: {
    SocialLogin: {
      providers: {
        google: true,
        facebook: false,
        apple: false,
        twitter: false,
      },
    },
  },
}

export default config
