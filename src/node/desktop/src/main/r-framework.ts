/*
 * r-framework.ts
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

// Every version of R the CRAN installer puts into the macOS R framework names
// the framework's shared Resources directory in its launcher script and a
// few other files. That directory follows whichever version is the
// framework's current one, so starting a version that isn't current -- or
// any R process it starts, such as R CMD INSTALL -- runs the current version
// instead. rig makes a version "orthogonal" by pointing those files at the
// version's own directory ('rig system make-orthogonal', make_orthogonal_ in
// rig's src/macos.rs); this does the same.

import { execFile } from 'child_process';
import {
  accessSync,
  chmodSync,
  constants,
  existsSync,
  lstatSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  symlinkSync,
  writeFileSync,
} from 'fs';
import { tmpdir } from 'os';
import path from 'path';
import { promisify } from 'util';

export const kRFrameworkRoot = '/Library/Frameworks/R.framework';

// the shared home, as named in the launcher, Renviron and fonts.conf
const kSharedHome = 'R.framework/Resources';

// the framework search path the version's Makeconf links packages with
const kSharedFrameworkFlag = '-F/Library/Frameworks/R.framework/..';

// the files naming the shared home, relative to the version's home
const kPatchedFiles = ['bin/R', 'etc/Renviron', 'fontconfig/fonts/fonts.conf'];

// a framework inside the version's directory, for its Makeconf to link against
const kFrameworkLinks: [string, string][] = [
  ['Headers', '../Headers'],
  ['Libraries', '../Resources/lib'],
  ['PrivateHeaders', '../PrivateHeaders'],
  ['R', '../R'],
  ['Resources', '../Resources'],
];

/** The user declined to authorize the change. */
export class AuthorizationCancelledError extends Error {}

// The CRAN installer leaves the framework's directories writable by the
// admin group (the group new entries inherit from their parent); the
// directory holding the links follows suit, whoever creates it.
const kLinkDirMode = 0o775;

/** A version of R in the framework. */
export interface FrameworkVersion {
  // the version's directory name, e.g. "4.4-arm64"
  name: string;
  // Versions/<name>
  dir: string;
  // Versions/<name>/Resources, the version's R home
  home: string;
}

/** The framework version with the given R home, or null for other homes. */
export function frameworkVersion(home: string, root = kRFrameworkRoot): FrameworkVersion | null {
  const prefix = `${root}/Versions/`;
  const suffix = '/Resources';
  if (!home.startsWith(prefix) || !home.endsWith(suffix)) {
    return null;
  }

  // "Current" is a link to the default version, which runs as itself
  const name = home.substring(prefix.length, home.length - suffix.length);
  if (!/^[A-Za-z0-9._-]+$/.test(name) || name === 'Current' || name === '.' || name === '..') {
    return null;
  }

  const dir = `${root}/Versions/${name}`;
  return { name, dir, home: `${dir}/Resources` };
}

/** Whether the framework version runs as itself when it isn't the default. */
export function isOrthogonal(version: FrameworkVersion): boolean {
  try {
    return !readFileSync(`${version.home}/bin/R`, 'utf-8').includes(kSharedHome);
  } catch {
    // an unreadable launcher can't be judged (nor fixed); running it fails on its own
    return true;
  }
}

export interface OrthogonalEdits {
  files: { path: string; contents: string }[];
  // the directory holding the links, and the links it lacks
  linkDir: string;
  links: { path: string; target: string }[];
}

/** The changes that make a framework version orthogonal; empty once it is. */
export function orthogonalEdits(version: FrameworkVersion): OrthogonalEdits {
  const edits: OrthogonalEdits = { files: [], linkDir: `${version.dir}/R.framework`, links: [] };

  const replace = (file: string, from: string, to: string) => {
    if (!existsSync(file)) {
      return;
    }

    const contents = readFileSync(file, 'utf-8');
    const patched = contents.replaceAll(from, to);
    if (patched !== contents) {
      edits.files.push({ path: file, contents: patched });
    }
  };

  for (const file of kPatchedFiles) {
    replace(`${version.home}/${file}`, kSharedHome, `R.framework/Versions/${version.name}/Resources`);
  }

  replace(
    `${version.home}/etc/Makeconf`,
    kSharedFrameworkFlag,
    `-F/Library/Frameworks/R.framework/Versions/${version.name}`,
  );

  for (const [name, target] of kFrameworkLinks) {
    const link = `${edits.linkDir}/${name}`;
    if (!pathExists(link)) {
      edits.links.push({ path: link, target });
    }
  }

  return edits;
}

/**
 * Make a framework version orthogonal. The files belong to root, but the
 * CRAN installer makes them writable by the admin group; when they aren't
 * writable, the change is made with administrator privileges, which macOS
 * asks the user to authorize (showing the given prompt).
 */
export async function makeOrthogonal(version: FrameworkVersion, authorizationPrompt: string): Promise<void> {
  const edits = orthogonalEdits(version);
  if (edits.files.length === 0 && edits.links.length === 0) {
    return;
  }

  if (canApply(edits, version)) {
    applyEdits(edits);
  } else {
    await applyEditsAsAdministrator(edits, authorizationPrompt);
  }

  if (!isOrthogonal(version)) {
    throw new Error(`${version.home}/bin/R still names the framework's shared home`);
  }
}

function pathExists(file: string): boolean {
  try {
    lstatSync(file);
    return true;
  } catch {
    return false;
  }
}

function isWritable(file: string): boolean {
  try {
    accessSync(file, constants.W_OK);
    return true;
  } catch {
    return false;
  }
}

function canApply(edits: OrthogonalEdits, version: FrameworkVersion): boolean {
  if (!edits.files.every((file) => isWritable(file.path))) {
    return false;
  }

  if (edits.links.length === 0) {
    return true;
  }

  return existsSync(edits.linkDir) ? isWritable(edits.linkDir) : isWritable(version.dir);
}

function applyEdits(edits: OrthogonalEdits): void {
  // writing into the existing files keeps their owner and permissions
  for (const file of edits.files) {
    writeFileSync(file.path, file.contents);
  }

  if (edits.links.length > 0) {
    if (!existsSync(edits.linkDir)) {
      mkdirSync(edits.linkDir, { recursive: true });
      chmodSync(edits.linkDir, kLinkDirMode);
    }
    for (const link of edits.links) {
      symlinkSync(link.target, link.path);
    }
  }
}

// Paths are only ever framework paths and a temporary directory; anything
// else is refused rather than quoted for the shell.
const kShellSafePath = /^[A-Za-z0-9/._+-]+$/;

function shellPath(file: string): string {
  if (!kShellSafePath.test(file)) {
    throw new Error(`unexpected characters in path: ${file}`);
  }
  return `'${file}'`;
}

function appleScriptString(text: string): string {
  return `"${text.replaceAll('\\', '\\\\').replaceAll('"', '\\"')}"`;
}

async function applyEditsAsAdministrator(edits: OrthogonalEdits, authorizationPrompt: string): Promise<void> {
  // the patched contents are written out first, so the privileged part only
  // copies files and makes links
  const staging = mkdtempSync(path.join(tmpdir(), 'rstudio-r-framework-'));
  try {
    const commands: string[] = [];
    edits.files.forEach((file, index) => {
      const staged = path.join(staging, `${index}`);
      writeFileSync(staged, file.contents);

      // copying onto the existing file keeps its owner and permissions
      commands.push(`/bin/cp ${shellPath(staged)} ${shellPath(file.path)}`);
    });

    if (edits.links.length > 0) {
      if (!existsSync(edits.linkDir)) {
        const mode = kLinkDirMode.toString(8);
        commands.push(`/bin/mkdir -p ${shellPath(edits.linkDir)}`, `/bin/chmod ${mode} ${shellPath(edits.linkDir)}`);
      }
      for (const link of edits.links) {
        commands.push(`/bin/ln -s ${shellPath(link.target)} ${shellPath(link.path)}`);
      }
    }

    const script =
      `do shell script ${appleScriptString(commands.join(' && '))} ` +
      `with prompt ${appleScriptString(authorizationPrompt)} with administrator privileges`;

    try {
      await promisify(execFile)('/usr/bin/osascript', ['-e', script]);
    } catch (error: unknown) {
      // osascript reports a dismissed authorization dialog as error -128
      const stderr = (error as { stderr?: string }).stderr ?? '';
      if (stderr.includes('(-128)')) {
        throw new AuthorizationCancelledError('the change was not authorized');
      }
      throw error;
    }
  } finally {
    rmSync(staging, { recursive: true, force: true });
  }
}
