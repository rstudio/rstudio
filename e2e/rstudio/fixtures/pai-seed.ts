import * as crypto from 'crypto';
import * as fs from 'fs';
import * as path from 'path';

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

/** The subdirectory of a PW_SEED_PAI tree holding the extracted package. */
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

/**
 * Validate a PW_SEED_PAI tree and report the version it holds.
 *
 * Called before the sandbox is populated so a typo or a stale path fails setup
 * with a clear message rather than a mystery download later.
 */
export function inspectSeed(seedRoot: string): { version: string; protocol: string } {
  const packageDir = path.join(seedRoot, SEED_PACKAGE_DIR);
  if (!fs.existsSync(path.join(packageDir, PACKAGE_JSON_FILE_NAME))) {
    throw new Error(
      `PW_SEED_PAI="${seedRoot}" does not look like a Posit Assistant install ` +
        `(missing ${SEED_PACKAGE_DIR}/${PACKAGE_JSON_FILE_NAME})`,
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
 * manifest-check.json, ...) is copied across as-is, since it is the backend's
 * own shared state and lives beside the slots. The seed's `bin` is
 * deliberately not copied: a versioned-aware RStudio never reads it, so
 * copying it would only add 18 MB per sandbox and make a resolver regression
 * harder to notice.
 *
 * @returns the version that was seeded.
 */
export function seedPaiSlot(seedRoot: string, storageDir: string): string {
  const { version, protocol } = inspectSeed(seedRoot);

  fs.mkdirSync(storageDir, { recursive: true });
  for (const entry of fs.readdirSync(seedRoot)) {
    if (entry === SEED_PACKAGE_DIR) continue;
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
