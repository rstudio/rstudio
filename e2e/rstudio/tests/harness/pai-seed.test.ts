import { test, expect } from '@playwright/test';
import { provisionPaiDataHome, seedPaiSlot, selectedPaiInstalls } from '@fixtures/pai-seed';
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

/**
 * A minimal stand-in for a `npm run deploy:rstudio` tree: one slot, selected
 * for its protocol. The slot's manifest is left empty, so a seeder that
 * trusted it instead of recording its own would produce a slot that fails to
 * verify.
 */
function writeFakeSeed(root: string, version: string, protocol = '11.0', slotName = version): string {
  const slot = path.join(root, 'versions', slotName);
  fs.mkdirSync(path.join(slot, 'dist', 'server'), { recursive: true });
  fs.mkdirSync(path.join(slot, 'dist', 'client'), { recursive: true });
  fs.writeFileSync(path.join(slot, 'dist', 'server', 'main.js'), "console.log('hi');");
  fs.writeFileSync(path.join(slot, 'dist', 'client', 'index.html'), '<html></html>');
  fs.writeFileSync(path.join(slot, 'package.json'), JSON.stringify({ version }));
  fs.writeFileSync(path.join(slot, 'protocol.json'), JSON.stringify({ protocol }));
  fs.writeFileSync(path.join(slot, '.slot-manifest.json'), JSON.stringify({ files: {} }));
  fs.writeFileSync(path.join(root, 'selected.json'), JSON.stringify({ selected: { [protocol]: slotName } }));
  // Shared backend state that lives beside the slots, not inside one.
  fs.writeFileSync(path.join(root, 'manifest-check.json'), '{}');
  fs.mkdirSync(path.join(root, 'ai-logs'));
  return root;
}

function writeSelector(seed: string, selected: unknown): void {
  fs.writeFileSync(path.join(seed, 'selected.json'), JSON.stringify({ selected }));
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

    // Shared state travels with it.
    expect(fs.existsSync(path.join(storage, 'manifest-check.json'))).toBe(true);
    expect(fs.existsSync(path.join(storage, 'ai-logs'))).toBe(true);

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

  test('seeds only the selected slot', () => {
    // A slot the selector does not name is not the build under test, and
    // neither is a legacy bin/ left beside the slots. The seed machine's
    // live lock entries would make sandbox installs refuse.
    const seed = writeFakeSeed(path.join(root, 'seed'), '1.2.2');
    fs.mkdirSync(path.join(seed, 'versions', '9.9.9'), { recursive: true });
    fs.mkdirSync(path.join(seed, 'bin'));
    fs.writeFileSync(path.join(seed, 'bin', 'package.json'), JSON.stringify({ version: '0.9.0' }));
    fs.mkdirSync(path.join(seed, 'locks'));
    fs.writeFileSync(path.join(seed, 'locks', 'install.lock'), '');
    const storage = path.join(root, 'data-home', 'pai');

    seedPaiSlot(seed, storage);

    expect(fs.readdirSync(path.join(storage, 'versions'))).toEqual(['1.2.2']);
    expect(fs.existsSync(path.join(storage, 'bin'))).toBe(false);
    expect(fs.existsSync(path.join(storage, 'locks'))).toBe(false);
  });

  test('names the sandbox slot by its package version, not the seed\'s slot name', () => {
    // Slot names carry no meaning: a reinstall of 1.2.2 is named 1.2.2-2.
    const seed = writeFakeSeed(path.join(root, 'seed'), '1.2.2', '11.0', '1.2.2-2');
    const dataHome = path.join(root, 'data-home');

    expect(seedPaiSlot(seed, path.join(dataHome, 'pai'))).toBe('1.2.2');

    expect(fs.readdirSync(path.join(dataHome, 'pai', 'versions'))).toEqual(['1.2.2']);
    expect(selectedPaiInstalls(dataHome)).toEqual([{ protocol: '11.0', version: '1.2.2' }]);
  });

  test('refuses a version that cannot name a slot', () => {
    // A version is not a path. Left unchecked this writes the tree outside
    // versions/ and records a selector entry RStudio ignores.
    const seed = writeFakeSeed(path.join(root, 'seed'), '../escaped');
    const storage = path.join(root, 'data-home', 'pai');

    expect(() => seedPaiSlot(seed, storage)).toThrow(/cannot name an install slot/);
    expect(fs.existsSync(path.join(root, 'data-home', 'escaped'))).toBe(false);
  });

  test('refuses a legacy bin-only tree', () => {
    // What deploy:rstudio:legacy, or an assistant checkout older than the
    // slot deploy, produces. RStudio never reads bin/, so neither does the
    // seeder.
    const seed = path.join(root, 'legacy');
    fs.mkdirSync(path.join(seed, 'bin'), { recursive: true });
    fs.writeFileSync(path.join(seed, 'bin', 'package.json'), JSON.stringify({ version: '1.2.2' }));

    expect(() => seedPaiSlot(seed, path.join(root, 'data-home', 'pai')))
      .toThrow(/does not look like a Posit Assistant install \(missing selected\.json\)/);
  });

  test('refuses a selector that cannot be read', () => {
    const seed = writeFakeSeed(path.join(root, 'seed'), '1.2.2');
    const storage = path.join(root, 'data-home', 'pai');

    fs.writeFileSync(path.join(seed, 'selected.json'), '{ not json');
    expect(() => seedPaiSlot(seed, storage)).toThrow(/Could not parse .*selected\.json/);

    fs.writeFileSync(path.join(seed, 'selected.json'), JSON.stringify({ selected: ['1.2.2'] }));
    expect(() => seedPaiSlot(seed, storage)).toThrow(/has no "selected" object/);

    expect(fs.existsSync(storage)).toBe(false);
  });

  test('refuses a selector that does not name exactly one protocol', () => {
    // More than one entry means RStudio has installed into the tree since it
    // was deployed; picking one would pick a build nobody deployed.
    const seed = writeFakeSeed(path.join(root, 'seed'), '1.2.2');
    const storage = path.join(root, 'data-home', 'pai');

    writeSelector(seed, { '11.0': '1.2.2', '12.0': '2.0.0' });
    expect(() => seedPaiSlot(seed, storage)).toThrow(/selects 2 protocols/);

    writeSelector(seed, {});
    expect(() => seedPaiSlot(seed, storage)).toThrow(/selects 0 protocols/);
  });

  test('refuses a selection that is not a slot name', () => {
    const seed = writeFakeSeed(path.join(root, 'seed'), '1.2.2');
    const storage = path.join(root, 'data-home', 'pai');

    writeSelector(seed, { '11.0': '../1.2.2' });
    expect(() => seedPaiSlot(seed, storage)).toThrow(/cannot name an install slot/);

    writeSelector(seed, { '11.0': 7 });
    expect(() => seedPaiSlot(seed, storage)).toThrow(/cannot name an install slot/);
  });

  test('refuses a selection naming a slot that does not exist', () => {
    const seed = writeFakeSeed(path.join(root, 'seed'), '1.2.2');
    writeSelector(seed, { '11.0': '1.3.0' });

    expect(() => seedPaiSlot(seed, path.join(root, 'data-home', 'pai')))
      .toThrow(/selects slot "1\.3\.0", but versions\/1\.3\.0\/package\.json does not exist/);
  });

  test('refuses a build missing a file RStudio requires', () => {
    // RStudio's verifyInstallDir() would reject the slot, and the seeded run
    // would then download the official package without saying so.
    const seed = writeFakeSeed(path.join(root, 'seed'), '1.2.2');
    fs.rmSync(path.join(seed, 'versions', '1.2.2', 'dist', 'server', 'main.js'));
    const storage = path.join(root, 'data-home', 'pai');

    expect(() => seedPaiSlot(seed, storage)).toThrow(/not a complete Posit Assistant build/);
    expect(fs.existsSync(storage)).toBe(false);
  });

  test('refuses a build whose required files are empty', () => {
    // A truncated build leaves zero-byte files in place; existence alone is
    // not what RStudio checks.
    const seed = writeFakeSeed(path.join(root, 'seed'), '1.2.2');
    fs.writeFileSync(path.join(seed, 'versions', '1.2.2', 'dist', 'client', 'index.html'), '');

    expect(() => seedPaiSlot(seed, path.join(root, 'data-home', 'pai')))
      .toThrow(/not a complete Posit Assistant build/);
  });

  test('provisions a per-spec storage directory that installs cannot leak out of', () => {
    const seed = writeFakeSeed(path.join(root, 'seed'), '1.2.2');
    const seedStorage = path.join(root, 'data-home', 'pai');
    seedPaiSlot(seed, seedStorage);

    const specDataHome = path.join(root, 'config_x', 'data-home');
    fs.mkdirSync(specDataHome, { recursive: true });
    provisionPaiDataHome(seedStorage, specDataHome);

    // A directory of its own, not a link to the seed, holding the same
    // selection and the same slot.
    const specStorage = path.join(specDataHome, 'pai');
    expect(fs.lstatSync(specStorage).isSymbolicLink()).toBe(false);
    expect(fs.lstatSync(path.join(specStorage, 'versions', '1.2.2')).isSymbolicLink()).toBe(false);
    expect(selectedPaiInstalls(specDataHome)).toEqual([{ protocol: '11.0', version: '1.2.2' }]);
    expect(fs.existsSync(path.join(specStorage, 'manifest-check.json'))).toBe(true);

    // The slot's files are hardlinks of the seed's, so the clone costs
    // nothing -- safe because a published slot is never modified.
    const seedMain = path.join(seedStorage, 'versions', '1.2.2', 'dist', 'server', 'main.js');
    const specMain = path.join(specStorage, 'versions', '1.2.2', 'dist', 'server', 'main.js');
    expect(fs.statSync(specMain).ino).toBe(fs.statSync(seedMain).ino);

    // What an install run by the session under test does: a new slot and a
    // rewritten selector. Neither may reach the seed every other spec reads.
    fs.mkdirSync(path.join(specStorage, 'versions', '1.3.0'));
    fs.writeFileSync(path.join(specStorage, 'selected.json'), JSON.stringify({ selected: { '11.0': '1.3.0' } }));
    expect(fs.readdirSync(path.join(seedStorage, 'versions'))).toEqual(['1.2.2']);
    expect(selectedPaiInstalls(path.join(root, 'data-home'))).toEqual([{ protocol: '11.0', version: '1.2.2' }]);
  });

  test('provisioning is a no-op when the storage directory already exists', () => {
    const seed = writeFakeSeed(path.join(root, 'seed'), '1.2.2');
    const seedStorage = path.join(root, 'data-home', 'pai');
    seedPaiSlot(seed, seedStorage);

    const specDataHome = path.join(root, 'config_x', 'data-home');
    fs.mkdirSync(path.join(specDataHome, 'pai'), { recursive: true });
    fs.writeFileSync(path.join(specDataHome, 'pai', 'marker'), '');

    provisionPaiDataHome(seedStorage, specDataHome);

    expect(fs.readdirSync(path.join(specDataHome, 'pai'))).toEqual(['marker']);
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
