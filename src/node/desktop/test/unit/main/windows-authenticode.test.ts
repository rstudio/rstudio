/*
 * windows-authenticode.test.ts
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

import { describe } from 'mocha';
import { assert } from 'chai';
import {
  installerSignature,
  isTrustedInstallerSignature,
  kInstallerSigner,
  kSignatureFileVariable,
  kSignatureScript,
  parseSignatureInfo,
  subjectCommonName,
} from '../../../src/main/windows-authenticode';

// the subject of RStudio's signing certificate as .NET prints it (the CN
// holds a comma, so it is quoted)
const positSubject = 'CN="Posit Software, PBC", O="Posit Software, PBC", L=Boston, S=Massachusetts, C=US';

describe('windows authenticode', () => {
  it('reads the CN out of a subject with quoted, escaped and plain values', () => {
    assert.equal(subjectCommonName(positSubject), 'Posit Software, PBC');
    assert.equal(subjectCommonName('CN=Posit Software\\, PBC, O=Posit'), 'Posit Software, PBC');
    assert.equal(subjectCommonName('O=Example, CN=Example Corp, C=US'), 'Example Corp');
    assert.equal(subjectCommonName('cn = spaced , O=x'), 'spaced');
    assert.equal(subjectCommonName('O=No Common Name'), '');
    assert.equal(subjectCommonName(''), '');
  });

  it('trusts only a valid signature from the Posit certificate', () => {
    assert.isTrue(isTrustedInstallerSignature({ status: 'Valid', subject: positSubject }, kInstallerSigner));
    assert.isFalse(isTrustedInstallerSignature({ status: 'NotSigned', subject: '' }, kInstallerSigner));
    assert.isFalse(isTrustedInstallerSignature({ status: 'HashMismatch', subject: positSubject }, kInstallerSigner));
    assert.isFalse(isTrustedInstallerSignature({ status: 'NotTrusted', subject: positSubject }, kInstallerSigner));
    // a valid signature from someone else, including a look-alike
    assert.isFalse(
      isTrustedInstallerSignature({ status: 'Valid', subject: 'CN=Posit Software PBC, O=Nobody' }, kInstallerSigner),
    );
    assert.isFalse(
      isTrustedInstallerSignature({ status: 'Valid', subject: 'O="Posit Software, PBC", CN=Other' }, kInstallerSigner),
    );
    assert.isFalse(isTrustedInstallerSignature(null, kInstallerSigner));
  });

  it('parses the script output and rejects anything else', () => {
    assert.deepEqual(parseSignatureInfo(`{"status":"Valid","subject":"${positSubject.replace(/"/g, '\\"')}"}\r\n`), {
      status: 'Valid',
      subject: positSubject,
    });
    assert.deepEqual(parseSignatureInfo('{"status":"NotSigned","subject":null}'), { status: 'NotSigned', subject: '' });
    assert.isNull(parseSignatureInfo(''));
    assert.isNull(parseSignatureInfo('not json'));
    assert.isNull(parseSignatureInfo('{"subject":"x"}'));
    assert.isNull(parseSignatureInfo('[]'));
  });

  it('hands PowerShell the file through the environment, never in the script', async () => {
    // quotes PowerShell ends a string at: ASCII, and U+2018 to U+201B
    const file = "C:\\Users\\D\u2019Arcy\\Temp\\RStudio-x'; Start-Process calc; \u2018.exe";
    const calls: [string, string[], Record<string, string> | undefined][] = [];
    const run = async (command: string, args: string[], env?: Record<string, string>) => {
      calls.push([command, args, env]);
      return '{"status":"Valid","subject":"CN=x"}';
    };
    assert.deepEqual(await installerSignature(file, run), { status: 'Valid', subject: 'CN=x' });

    const [command, args, env] = calls[0];
    assert.equal(command, 'powershell.exe');
    assert.include(args, '-NonInteractive');
    assert.equal(args[args.length - 1], kSignatureScript);
    assert.include(kSignatureScript, `-LiteralPath $env:${kSignatureFileVariable}`);
    assert.notInclude(args.join(' '), 'Start-Process');
    assert.deepEqual(env, { [kSignatureFileVariable]: file });
  });

  it('runs PowerShell and answers null when it cannot', async () => {
    assert.isNull(await installerSignature('C:\\x.exe', async () => null));
    assert.isNull(await installerSignature('C:\\x.exe', async () => 'garbage'));
  });
});
