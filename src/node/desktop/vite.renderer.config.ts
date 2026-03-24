import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  build: {
    rollupOptions: {
      input: {
        loading: resolve(__dirname, 'src/ui/loading/loading.html'),
        error: resolve(__dirname, 'src/ui/error/error.html'),
        connect: resolve(__dirname, 'src/ui/connect/connect.html'),
        splash: resolve(__dirname, 'src/ui/splash/splash.html'),
        'choose-r': resolve(__dirname, 'src/ui/widgets/choose-r/ui.html'),
      },
    },
  },
});
