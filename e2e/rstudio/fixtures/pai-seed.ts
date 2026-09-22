import * as crypto from 'crypto';
import * as fs from 'fs';
import * as path from 'path';
import { cloneTreeHardlinks } from './r-libs-setup';

/**
 * Seed a locally built Posit Assistant into a sandbox storage directory.
 *
 * RStudio installs each package version into its own slot under
 * `pai/versions/<version>` and records the active slot per protocol in
 * `pai/selected.json` (rstudio/rstudio#18658). The legacy unversioned
 * `pai/bin` is never read. So a seed cannot just be a copy of the source
 * tree: it has to be laid out the way an install leaves it, manifest and
 * selector included, or the IDE resolves nothing and downloads the official
 * package instead -- silently testing the wrong build.
 *
 * The C++ side of this lives in `src/cpp/session/modules/chat/ChatSlots.cpp`
 * and `ChatSelector.cpp`; the file names and JSON shapes below mirror
 * `ChatConstants.cpp`.
 */

// Mirrors chat::constants in src/cpp/session/modules/chat/ChatConstants.cpp.
const VERSIONS_DIR_NAME = 'versions';
const SELECTOR_FILE_NAME = 'selected.json';
const SLOT_MANIFEST_FILE_NAME = '.slot-manifest.json';
const PACKAGE_JSON_FILE_NAME = 'package.json';
const PROTOCOL_FILE_NAME = 'protocol.json';
const CLIENT_DIR_PATH = 'dist/client';
const SERVER_SCRIPT_PATH = 'dist/server/main.js';
const INDEX_FILE_NAME = 'index.html';

/**
 * The subdirectory of a PW_SEED_PAI tree holding the extracted package: the
 * assistant repo's `npm run deploy:rstudio` writes the package to `pai/bin`.
 */
const SEED_PACKAGE_DIR = 'bin';

interface ManifestEntry {
  size: number;
  sha256: string;
}

/** Every regular file under `dir`, as '/'-separated paths relative to it. */
function relativeFiles(dir: string, prefix = ''): string[] {
  const found: string[] = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const relative = prefix ? `${prefix}/${entry.name}` : entry.name;
    // Symlinks are skipped rather than followed, matching collectEntries() in
    // ChatSlotManifest.cpp: recording one would record the target's size and
    // make the slot verify against a file outside it.
    if (entry.isSymbolicLink()) continue;
    if (entry.isDirectory()) {
      found.push(...relativeFiles(path.join(dir, entry.name), relative));
    } else if (entry.isFile() && relative !== SLOT_MANIFEST_FILE_NAME) {
      found.push(relative);
    }
  }
  return found;
}

/**
 * Record the install-time manifest a slot needs to verify.
 *
 * Verification compares sizes only, but the hashes are recorded anyway so a
 * seeded slot is indistinguishable on disk from one an install produced.
 */
function writeSlotManifest(slotDir: string): void {
  const files: Record<string, ManifestEntry> = {};
  for (const relative of relativeFiles(slotDir)) {
    const absolute = path.join(slotDir, relative);
    files[relative] = {
      size: fs.statSync(absolute).size,
      sha256: crypto.createHash('sha256').update(fs.readFileSync(absolute)).digest('hex'),
    };
  }
  fs.writeFileSync(path.join(slotDir, SLOT_MANIFEST_FILE_NAME), JSON.stringify({ files }));
}

// Device names Windows resolves in any directory. Checked on every platform for
// the same reason the C++ side checks them: a sandbox seeded on one OS has to
// be a slot on any other.
const WINDOWS_RESERVED = new Set([
  'con', 'prn', 'aux', 'nul',
  'com1', 'com2', 'com3', 'com4', 'com5', 'com6', 'com7', 'com8', 'com9',
  'lpt1', 'lpt2', 'lpt3', 'lpt4', 'lpt5', 'lpt6', 'lpt7', 'lpt8', 'lpt9',
]);

/**
 * Whether a string may name a slot directory.
 *
 * Mirrors `slots::isUsableSlotName()` in ChatSlots.cpp. The version comes out
 * of a package.json, so it is not a path: `../../elsewhere` would write the
 * seed outside `versions/`, and a name RStudio rejects would produce a selector
 * entry resolution ignores -- which fails as a silent download of the official
 * package rather than as a broken seed.
 */
function isUsableSlotName(name: string): boolean {
  if (name === '' || name.startsWith('.') || name.startsWith('-')) return false;
  if (name.endsWith('.') || name.endsWith(' ')) return false;
  // Printable ASCII only.
  if (!/^[\x20-\x7e]+$/.test(name)) return false;
  if (WINDOWS_RESERVED.has(name.split('.')[0].toLowerCase())) return false;
  return !/[/\\:*?"<>|]/.test(name);
}

function readJsonField(filePath: string, field: string): string {
  let parsed: Record<string, unknown>;
  try {
    parsed = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
  } catch (err) {
    throw new Error(`Could not parse ${filePath}: ${(err as Error).message}`);
  }
  const value = parsed[field];
  if (typeof value !== 'string' || value === '') {
    throw new Error(`${filePath} declares no "${field}" string`);
  }
  return value;
}

function isNonEmptyFile(filePath: string): boolean {
  try {
    const stat = fs.statSync(filePath);
    return stat.isFile() && stat.size > 0;
  } catch {
    return false;
  }
}

/**
 * Whether a directory holds a package RStudio could launch.
 *
 * Mirrors `installation::verifyInstallDir()` in ChatInstallation.cpp: the
 * client directory exists, and the server script and index.html exist and are
 * non-empty. A seed that fails this would be written as a selected slot and
 * then rejected by RStudio -- which then downloads the official package, so
 * the failure would be silent.
 */
function isRunnablePackage(packageDir: string): boolean {
  const clientDir = path.join(packageDir, CLIENT_DIR_PATH);
  let isDir = false;
  try {
    isDir = fs.statSync(clientDir).isDirectory();
  } catch {
    return false;
  }
  return (
    isDir &&
    isNonEmptyFile(path.join(packageDir, SERVER_SCRIPT_PATH)) &&
    isNonEmptyFile(path.join(clientDir, INDEX_FILE_NAME))
  );
}

/**
 * Validate a PW_SEED_PAI tree and report the version it holds.
 *
 * Called before the sandbox is populated so a typo, a stale path or a partial
 * build fails setup with a clear message rather than a mystery download later.
 */
export function inspectSeed(seedRoot: string): { version: string; protocol: string } {
  const packageDir = path.join(seedRoot, SEED_PACKAGE_DIR);
  if (!fs.existsSync(path.join(packageDir, PACKAGE_JSON_FILE_NAME))) {
    throw new Error(
      `PW_SEED_PAI="${seedRoot}" does not look like a Posit Assistant install ` +
        `(missing ${SEED_PACKAGE_DIR}/${PACKAGE_JSON_FILE_NAME})`,
    );
  }
  if (!isRunnablePackage(packageDir)) {
    throw new Error(
      `PW_SEED_PAI="${seedRoot}" is not a complete Posit Assistant build: ` +
        `${SEED_PACKAGE_DIR}/${SERVER_SCRIPT_PATH} and ` +
        `${SEED_PACKAGE_DIR}/${CLIENT_DIR_PATH}/${INDEX_FILE_NAME} must exist and be non-empty`,
    );
  }
  const version = readJsonField(path.join(packageDir, PACKAGE_JSON_FILE_NAME), 'version');
  if (!isUsableSlotName(version)) {
    throw new Error(
      `PW_SEED_PAI="${seedRoot}" declares version "${version}", which cannot name an ` +
        `install slot -- RStudio would not resolve it`,
    );
  }
  return {
    version,
    protocol: readJsonField(path.join(packageDir, PROTOCOL_FILE_NAME), 'protocol'),
  };
}

/**
 * Lay out `seedRoot` as an installed slot in `storageDir`.
 *
 * The package becomes `versions/<version>` with its manifest, selected for the
 * protocol it declares. Everything else in the seed (paconfig.json,
 * manifest-check.json, ...) is copied across as-is, since it is shared state
 * that lives beside the slots. The seed's `bin` is deliberately not copied: a
 * versioned-aware RStudio never reads it, so copying it would only add 18 MB
 * per sandbox and make a resolver regression harder to notice. A `versions`
 * directory or `selected.json` already in the seed (a real `pai` that this
 * RStudio has installed into) is not copied either, so the slot under test is
 * exactly the one `bin` holds.
 *
 * @returns the version that was seeded.
 */
export function seedPaiSlot(seedRoot: string, storageDir: string): string {
  const { version, protocol } = inspectSeed(seedRoot);

  fs.mkdirSync(storageDir, { recursive: true });
  for (const entry of fs.readdirSync(seedRoot)) {
    if (entry === SEED_PACKAGE_DIR || entry === VERSIONS_DIR_NAME || entry === SELECTOR_FILE_NAME) {
      continue;
    }
    fs.cpSync(path.join(seedRoot, entry), path.join(storageDir, entry), { recursive: true });
  }

  const slotDir = path.join(storageDir, VERSIONS_DIR_NAME, version);
  fs.cpSync(path.join(seedRoot, SEED_PACKAGE_DIR), slotDir, { recursive: true });
  writeSlotManifest(slotDir);

  fs.writeFileSync(
    path.join(storageDir, SELECTOR_FILE_NAME),
    JSON.stringify({ selected: { [protocol]: version } }),
  );

  return version;
}

/**
 * Give a per-spec data home its own `pai` storage directory, provisioned from
 * the sandbox seed.
 *
 * A real directory, not a link to the seed: an install run by the session
 * under test publishes a new slot and rewrites `selected.json` in the storage
 * directory it resolves, and through a link that would land in the seed every
 * other spec reads. The seed's slots are immutable once published, so each
 * one is hardlink-cloned rather than copied (18 MB per slot per spec
 * otherwise); the small files beside them (`selected.json`,
 * `manifest-check.json`, `paconfig.json`) are copied, since the session
 * writes them.
 *
 * No-op when the destination already exists (config-root reuse across a
 * restart).
 */
export function provisionPaiDataHome(seedStorageDir: string, dataHome: string): void {
  const dest = path.join(dataHome, 'pai');
  if (fs.existsSync(dest)) return;

  fs.mkdirSync(dest, { recursive: true });
  for (const entry of fs.readdirSync(seedStorageDir, { withFileTypes: true })) {
    const src = path.join(seedStorageDir, entry.name);
    const dst = path.join(dest, entry.name);
    if (entry.name === VERSIONS_DIR_NAME) {
      fs.mkdirSync(dst);
      for (const slot of fs.readdirSync(src)) {
        cloneTreeHardlinks(path.join(src, slot), path.join(dst, slot));
      }
    } else {
      fs.cpSync(src, dst, { recursive: true });
    }
  }
}

/** A protocol and the package version its selected slot holds. */
export interface SelectedInstall {
  protocol: string;
  version: string;
}

/**
 * Every selected install under `dataHome`, in protocol order.
 *
 * Follows the two steps the IDE does -- selector entry, then the slot's own
 * package.json -- rather than trusting the slot's name, which carries no
 * meaning (a reinstall of 1.1.0 is named 1.1.0-2).
 *
 * All of them are returned rather than one, because which one a session runs
 * depends on the protocol compiled into the IDE, which is not readable from
 * here. A sandbox normally holds a single selection, but a seed the IDE found
 * incompatible leaves two -- exactly the case where naming one build as "the"
 * one under test would name the wrong one.
 */
export function selectedPaiInstalls(dataHome: string): SelectedInstall[] {
  const storageDir = path.join(dataHome, 'pai');
  const selectorPath = path.join(storageDir, SELECTOR_FILE_NAME);
  if (!fs.existsSync(selectorPath)) return [];

  let selected: Record<string, unknown>;
  try {
    selected = JSON.parse(fs.readFileSync(selectorPath, 'utf-8')).selected ?? {};
  } catch {
    return [];
  }

  const installs: SelectedInstall[] = [];
  for (const protocol of Object.keys(selected).sort()) {
    const slotName = selected[protocol];
    if (typeof slotName !== 'string') continue;
    const packageJson = path.join(storageDir, VERSIONS_DIR_NAME, slotName, PACKAGE_JSON_FILE_NAME);
    if (!fs.existsSync(packageJson)) continue;
    try {
      installs.push({ protocol, version: readJsonField(packageJson, 'version') });
    } catch {
      // A selected slot with an unreadable package.json is reported as absent
      // rather than failing the run: this is a diagnostic read-back.
    }
  }

  return installs;
}
