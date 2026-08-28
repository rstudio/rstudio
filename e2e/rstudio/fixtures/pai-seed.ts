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
  return {
    version: readJsonField(path.join(packageDir, PACKAGE_JSON_FILE_NAME), 'version'),
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

/**
 * The version held by the first selected slot under `dataHome`, or null when
 * nothing is installed there.
 *
 * Follows the two steps the IDE does -- selector entry, then the slot's own
 * package.json -- rather than trusting the slot's name, which carries no
 * meaning (a reinstall of 1.1.0 is named 1.1.0-2). A sandbox only ever holds
 * one protocol's selection, so "first" is unambiguous in practice.
 */
export function installedPaiVersion(dataHome: string): string | null {
  const storageDir = path.join(dataHome, 'pai');
  const selectorPath = path.join(storageDir, SELECTOR_FILE_NAME);
  if (!fs.existsSync(selectorPath)) return null;

  let selected: Record<string, unknown>;
  try {
    selected = JSON.parse(fs.readFileSync(selectorPath, 'utf-8')).selected ?? {};
  } catch {
    return null;
  }

  for (const slotName of Object.values(selected)) {
    if (typeof slotName !== 'string') continue;
    const packageJson = path.join(storageDir, VERSIONS_DIR_NAME, slotName, PACKAGE_JSON_FILE_NAME);
    if (!fs.existsSync(packageJson)) continue;
    try {
      return readJsonField(packageJson, 'version');
    } catch {
      return null;
    }
  }

  return null;
}
