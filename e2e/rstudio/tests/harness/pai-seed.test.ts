import { test, expect } from '@playwright/test';
import { seedPaiSlot, selectedPaiInstalls } from '@fixtures/pai-seed';
import * as crypto from 'crypto';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

/**
 * Harness self-test for the PW_SEED_PAI provisioning in fixtures/pai-seed.ts.
 *
 * The seeder writes a version slot, its install manifest and the selector in
 * TypeScript, because there is no way to call RStudio's install path from the
 * test side. Nothing else fails when it drifts: RStudio simply resolves no
 * install and downloads the official package, and the @ai suite passes green
 * having exercised a build nobody asked for. The C++ side pins the shape it
 * accepts (ChatSlots.VerifiesASlotWhoseManifestWasWrittenExternally); this
 * pins the shape produced.
 *
 * Pure filesystem work -- no IDE, no seeded assistant needed.
 */

/** A minimal stand-in for a `npm run deploy:rstudio` tree. */
function writeFakeSeed(root: string, version: string, protocol = '11.0'): string {
  const bin = path.join(root, 'bin');
  fs.mkdirSync(path.join(bin, 'dist', 'server'), { recursive: true });
  fs.mkdirSync(path.join(bin, 'dist', 'client'), { recursive: true });
  fs.writeFileSync(path.join(bin, 'dist', 'server', 'main.js'), "console.log('hi');");
  fs.writeFileSync(path.join(bin, 'dist', 'client', 'index.html'), '<html></html>');
  fs.writeFileSync(path.join(bin, 'package.json'), JSON.stringify({ version }));
  fs.writeFileSync(path.join(bin, 'protocol.json'), JSON.stringify({ protocol }));
  // Shared backend state that lives beside the slots, not inside one.
  fs.writeFileSync(path.join(root, 'manifest-check.json'), '{}');
  return root;
}

test.describe('PW_SEED_PAI slot provisioning', () => {
  let root: string;

  test.beforeEach(() => {
    root = fs.mkdtempSync(path.join(os.tmpdir(), 'pai_seed_test_'));
  });

  test.afterEach(() => {
    fs.rmSync(root, { recursive: true, force: true });
  });

  test('lays the package out as a selected version slot', () => {
    const seed = writeFakeSeed(path.join(root, 'seed'), '1.2.2');
    const dataHome = path.join(root, 'data-home');
    const storage = path.join(dataHome, 'pai');

    expect(seedPaiSlot(seed, storage)).toBe('1.2.2');

    // The selector names the slot under the protocol the package declares.
    // Recorded under any other, resolution would reject the slot it was
    // handed and fall back.
    expect(JSON.parse(fs.readFileSync(path.join(storage, 'selected.json'), 'utf-8')))
      .toEqual({ selected: { '11.0': '1.2.2' } });

    // Shared state travels; the unversioned install does not, because a
    // versioned-aware RStudio never reads it.
    expect(fs.existsSync(path.join(storage, 'manifest-check.json'))).toBe(true);
    expect(fs.existsSync(path.join(storage, 'bin'))).toBe(false);

    expect(selectedPaiInstalls(dataHome)).toEqual([{ protocol: '11.0', version: '1.2.2' }]);
  });

  test('records every file at its real size and hash', () => {
    const seed = writeFakeSeed(path.join(root, 'seed'), '1.2.2');
    const storage = path.join(root, 'data-home', 'pai');
    seedPaiSlot(seed, storage);

    const slot = path.join(storage, 'versions', '1.2.2');
    const { files } = JSON.parse(fs.readFileSync(path.join(slot, '.slot-manifest.json'), 'utf-8'));

    // Every file in the slot except the manifest itself, keyed by a
    // '/'-separated relative path -- the form matchesSlotManifest() reads.
    expect(Object.keys(files).sort()).toEqual([
      'dist/client/index.html',
      'dist/server/main.js',
      'package.json',
      'protocol.json',
    ]);

    for (const [relative, entry] of Object.entries(files) as [string, { size: number; sha256: string }][]) {
      const contents = fs.readFileSync(path.join(slot, relative));
      expect(entry.size, `size recorded for ${relative}`).toBe(contents.length);
      expect(entry.sha256, `hash recorded for ${relative}`)
        .toBe(crypto.createHash('sha256').update(contents).digest('hex'));
    }
  });

  test('refuses a version that cannot name a slot', () => {
    // A version is not a path. Left unchecked this writes the tree outside
    // versions/ and records a selector entry RStudio ignores.
    const seed = writeFakeSeed(path.join(root, 'seed'), '../escaped');
    const storage = path.join(root, 'data-home', 'pai');

    expect(() => seedPaiSlot(seed, storage)).toThrow(/cannot name an install slot/);
    expect(fs.existsSync(path.join(root, 'data-home', 'escaped'))).toBe(false);
  });

  test('refuses a tree that is not a Posit Assistant install', () => {
    const seed = path.join(root, 'empty');
    fs.mkdirSync(seed, { recursive: true });

    expect(() => seedPaiSlot(seed, path.join(root, 'data-home', 'pai')))
      .toThrow(/does not look like a Posit Assistant install/);
  });

  test('reports every selected install, not just the first', () => {
    // What a run looks like when the IDE rejected the seed and installed a
    // build for its own protocol: naming one version here would name the
    // wrong one, since which protocol the session ran is not on disk.
    const dataHome = path.join(root, 'data-home');
    const storage = path.join(dataHome, 'pai');
    seedPaiSlot(writeFakeSeed(path.join(root, 'seed-a'), '1.2.2', '11.0'), storage);
    seedPaiSlot(writeFakeSeed(path.join(root, 'seed-b'), '2.0.0', '12.0'), storage);

    // The second seed replaces selected.json wholesale, so put both back the
    // way two installs would leave it.
    fs.writeFileSync(
      path.join(storage, 'selected.json'),
      JSON.stringify({ selected: { '11.0': '1.2.2', '12.0': '2.0.0' } }),
    );

    expect(selectedPaiInstalls(dataHome)).toEqual([
      { protocol: '11.0', version: '1.2.2' },
      { protocol: '12.0', version: '2.0.0' },
    ]);
  });

  test('reports nothing when no install is selected', () => {
    expect(selectedPaiInstalls(path.join(root, 'data-home'))).toEqual([]);
  });
});
