/*
 * windows-authenticode.ts
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

import { CommandRunner } from './running-instances';

// The Authenticode check on a downloaded Windows installer. Its SHA-256 only
// proves it is the file the builds site's API named, and the API (or the
// pipeline that publishes to it) could be made to name any executable; the
// installer then runs silently, without a UAC prompt for a per-user install.
// So before it is run it must carry a valid signature from Posit's
// certificate, as Squirrel.Mac checks the new bundle's signature on macOS.
// Windows has no command-line tool for this short of PowerShell's
// Get-AuthenticodeSignature, whose answer is read here.

/** The subject common name of the certificate RStudio's Windows builds are signed with. */
export const kInstallerSigner = 'Posit Software, PBC';

/** What Get-AuthenticodeSignature reported for a file. */
export interface SignatureInfo {
  /** 'Valid', or why not: 'NotSigned', 'HashMismatch', 'NotTrusted', 'UnknownError', ... */
  status: string;
  /** the signing certificate's X.500 subject, '' when unsigned */
  subject: string;
}

/**
 * The PowerShell that reports the signature of `file` as one JSON object:
 * `{"status":"Valid","subject":"CN=\"Posit Software, PBC\", O=..."}`.
 */
export function signatureScript(file: string): string {
  // a single-quoted PowerShell string takes everything literally except the
  // quote itself, which is doubled
  const literal = `'${file.replace(/'/g, "''")}'`;
  return [
    `$s = Get-AuthenticodeSignature -LiteralPath ${literal}`,
    '$c = $s.SignerCertificate',
    "$o = @{ status = [string]$s.Status; subject = if ($c) { [string]$c.Subject } else { '' } }",
    '[Console]::Out.Write(($o | ConvertTo-Json -Compress))',
  ].join('; ');
}

/** Read the script's answer; null when it is not the expected object. */
export function parseSignatureInfo(output: string): SignatureInfo | null {
  let parsed: unknown;
  try {
    parsed = JSON.parse(output.trim());
  } catch {
    return null;
  }
  if (typeof parsed !== 'object' || parsed === null) {
    return null;
  }
  const { status, subject } = parsed as Record<string, unknown>;
  if (typeof status !== 'string' || status.length === 0) {
    return null;
  }
  return { status, subject: typeof subject === 'string' ? subject : '' };
}

/**
 * The CN attribute of an X.500 subject as .NET prints it, e.g.
 * `CN="Posit Software, PBC", O="Posit Software, PBC", L=Boston, S=Massachusetts, C=US`:
 * attributes separated by commas, values that contain one quoted (or the
 * comma escaped with a backslash). '' when there is no CN.
 */
export function subjectCommonName(subject: string): string {
  const attributes: string[] = [];
  let current = '';
  let quoted = false;
  for (let i = 0; i < subject.length; i++) {
    const ch = subject[i];
    if (ch === '"') {
      quoted = !quoted;
    } else if (ch === '\\' && i + 1 < subject.length) {
      current += subject[++i];
    } else if (ch === ',' && !quoted) {
      attributes.push(current);
      current = '';
    } else {
      current += ch;
    }
  }
  attributes.push(current);

  for (const attribute of attributes) {
    const match = /^\s*CN\s*=\s*(.*?)\s*$/i.exec(attribute);
    if (match) {
      return match[1];
    }
  }
  return '';
}

/** True when the file carries a valid signature whose certificate is `signer`'s. */
export function isTrustedInstallerSignature(signature: SignatureInfo | null, signer: string): boolean {
  return signature !== null && signature.status === 'Valid' && subjectCommonName(signature.subject) === signer;
}

/** How long the PowerShell may take; it reads the whole file once. */
export const kSignatureCheckTimeoutMs = 2 * 60 * 1000;

/**
 * The Authenticode signature of `file`, or null when it could not be read
 * (PowerShell missing or blocked, say), which callers treat as untrusted.
 */
export async function installerSignature(file: string, run: CommandRunner): Promise<SignatureInfo | null> {
  const output = await run('powershell.exe', [
    '-NoProfile',
    '-NonInteractive',
    '-ExecutionPolicy',
    'Bypass',
    '-Command',
    signatureScript(file),
  ]);
  return output === null ? null : parseSignatureInfo(output);
}
