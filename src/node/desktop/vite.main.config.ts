import { defineConfig } from 'vite';
import path from 'path';
import fs from 'fs';
import type { Plugin } from 'vite';

// Plugin to handle native .node addon files.
// Marks .node imports as external (rewriting to a local path) and copies
// the binary files to the build output directory so they sit alongside main.js.
function nativeNodePlugin(): Plugin {
  const nodeFiles = new Map<string, string>();

  return {
    name: 'native-node',
    enforce: 'pre',
    resolveId(source, importer) {
      if (source.endsWith('.node') && importer) {
        const resolved = path.resolve(path.dirname(importer), source);
        const basename = path.basename(resolved);
        nodeFiles.set(basename, resolved);
        return { id: `./${basename}`, external: true };
      }
    },
    generateBundle() {
      for (const [basename, filePath] of nodeFiles) {
        this.emitFile({
          type: 'asset',
          fileName: basename,
          source: fs.readFileSync(filePath),
        });
      }
    },
  };
}

export default defineConfig({
  plugins: [nativeNodePlugin()],
  resolve: {
    extensions: ['.js', '.ts', '.json'],
  },
});
