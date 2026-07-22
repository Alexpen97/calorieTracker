import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(() => {
  cleanup()
})

Object.defineProperty(window, 'crypto', {
  value: {
    getRandomValues: (arr: Uint8Array) => {
      for (let i = 0; i < arr.length; i += 1) arr[i] = i % 255
      return arr
    },
    subtle: {
      digest: async () => new Uint8Array(32).buffer,
    },
  },
})
