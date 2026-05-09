import { defineConfig } from 'vite';

// Custom output name to avoid conflicting with the main preload.js
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        entryFileNames: 'whats-new-preload.js',
      },
    },
  },
});
