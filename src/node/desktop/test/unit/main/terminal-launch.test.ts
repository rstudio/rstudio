/*
 * terminal-launch.test.ts
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

import { assert } from 'chai';
import { spawnSync } from 'child_process';
import { app } from 'electron';
import { mkdirSync, mkdtempSync, realpathSync, rmSync, symlinkSync, writeFileSync } from 'fs';
import { tmpdir } from 'os';
import path from 'path';
import { forwardTerminalCommand } from '../../../src/main/terminal-launch';

describe('TerminalLaunch', () => {
  it('leaves normal Desktop launches and Windows unchanged', () => {
    const terminal = { RSTUDIO_TERM: 'terminal-id', RS_RPOSTBACK_PATH: '/bin/rpostback' };
    for (const env of [{}, { ...terminal, RSTUDIO_TERM: '' }, { ...terminal, RS_RPOSTBACK_PATH: '' }]) {
      assert.isUndefined(forwardTerminalCommand(['file.R'], env, 'linux'));
    }
    assert.isUndefined(forwardTerminalCommand(['file.R'], terminal, 'win32'));
  });

  it('leaves bare and switch-only launches from a terminal to Desktop', () => {
    const terminal = { RSTUDIO_TERM: 'terminal-id', RS_RPOSTBACK_PATH: '/bin/rpostback' };
    for (const args of [[], ['--'], ['--no-sandbox'], ['--version'], ['--help'], ['-h'], ['--unknown', '--']]) {
      assert.isUndefined(forwardTerminalCommand(args, terminal, 'linux'), JSON.stringify(args));
    }
  });

  it('leaves project files from a terminal (e.g. xdg-open foo.Rproj) to Desktop', () => {
    const terminal = { RSTUDIO_TERM: 'terminal-id', RS_RPOSTBACK_PATH: '/bin/rpostback' };
    for (const args of [['foo.Rproj'], ['/abs/foo.RPROJ'], ['--', 'foo.Rproj'], ['a.R', 'foo.Rproj']]) {
      assert.isUndefined(forwardTerminalCommand(args, terminal, 'linux'), JSON.stringify(args));
    }
  });

  describe('postback integration', function () {
    this.timeout(10000);
    let dir: string;
    let env: NodeJS.ProcessEnv;
    const helper = path.resolve(__dirname, '../../../../../cpp/session/postback/rstudio');
    const modulePath = path.resolve(__dirname, '../../../src/main/terminal-launch.ts');

    before(function () {
      if (process.platform === 'win32') {
        this.skip();
      }
    });

    beforeEach(() => {
      dir = realpathSync(mkdtempSync(path.join(tmpdir(), 'rstudio terminal ')));
      const postback = path.join(dir, 'fake rpostback');
      writeFileSync(
        postback,
        `#!/bin/sh
printf '<%s>\\n' "$@"
case "$2" in
  */fail.R) exit 7 ;;
esac
`,
        { mode: 0o755 },
      );
      env = {
        ...process.env,
        ELECTRON_RUN_AS_NODE: '1',
        RSTUDIO_TERM: 'terminal-id',
        RS_RPOSTBACK_PATH: postback,
        // Deliberately omit both the helper directory and our fake rpostback.
        PATH: '/usr/bin:/bin',
        TS_NODE_PROJECT: path.resolve(__dirname, '../../../tsconfig.json'),
      };
    });

    afterEach(() => {
      rmSync(dir, { recursive: true, force: true });
    });

    function launch(args: string[], cwd = dir, launchEnv = env) {
      const script = `
        const { forwardTerminalCommand } = require(${JSON.stringify(modulePath)});
        const status = forwardTerminalCommand(${JSON.stringify(args)});
        process.exit(status === undefined ? 99 : status);
      `;
      return spawnSync(process.execPath, ['-r', require.resolve('ts-node/register'), '-e', script], {
        cwd,
        env: launchEnv,
        encoding: 'utf8',
      });
    }

    function launchHelper(args: string[], cwd = dir, launchEnv = env) {
      return spawnSync('/bin/bash', [helper, ...args], { cwd, env: launchEnv, encoding: 'utf8' });
    }

    function assertMatchesHelper(args: string[], cwd = dir, launchEnv = env) {
      const expected = launchHelper(args, cwd, launchEnv);
      const actual = launch(args, cwd, launchEnv);
      assert.equal(actual.status, expected.status, actual.stderr);
      assert.equal(actual.stdout, expected.stdout);
      assert.equal(actual.stderr, expected.stderr);
    }

    it('forwards multiple files and positions verbatim with a reset PATH', () => {
      const files = ['relative file.R:2:3', '/absolute/file.R:4', 'a/../file.R', '$(echo unsafe).R', '-dash.R'];
      const result = launch(files);
      assert.equal(result.status, 0, result.stderr);
      assert.equal(
        result.stdout,
        files.map((file) => `<openfile>\n<${file.startsWith('/') ? file : `${dir}/${file}`}>\n`).join(''),
      );
      assert.isEmpty(result.stderr);
    });

    for (const args of [
      ['--', '-dash.R'],
      ['file.R', '--help'],
      ['--help', 'file.R'],
      ['-h', 'file.R'],
      ['--unknown', 'file.R'],
      ['--', '--help'],
    ]) {
      it(`matches the shell helper for ${JSON.stringify(args)}`, () => {
        assertMatchesHelper(args);
      });
    }

    it('matches the shell helper in a working directory reached through a symlink', () => {
      const real = path.join(dir, 'real');
      const link = path.join(dir, 'link');
      mkdirSync(real);
      symlinkSync('real', link);
      // bash keeps an inherited PWD that names the current directory
      const linkEnv = { ...env, PWD: link };
      assertMatchesHelper(['file.R'], link, linkEnv);
      const result = launch(['file.R'], link, linkEnv);
      assert.equal(result.status, 0, result.stderr);
      assert.equal(result.stdout, `<openfile>\n<${link}/file.R>\n`);
    });

    it('ignores a PWD that names a different directory', () => {
      const real = path.join(dir, 'real');
      mkdirSync(real);
      for (const PWD of [dir, path.join(dir, 'missing'), 'relative']) {
        const result = launch(['file.R'], real, { ...env, PWD });
        assert.equal(result.status, 0, result.stderr);
        assert.equal(result.stdout, `<openfile>\n<${real}/file.R>\n`, PWD);
      }
    });

    it('reports postback errors, continues with later files, and exits unsuccessfully', () => {
      const result = launch(['fail.R', 'success.R']);
      assert.equal(result.status, 1);
      assert.equal(result.stderr, `<openfile>\n<${dir}/fail.R>\n`);
      assert.equal(result.stdout, `<openfile>\n<${dir}/success.R>\n`);
    });

    it('exits before Desktop startup when invoked through the Electron entry point', function () {
      if (!process.versions.electron) {
        this.skip();
      }
      const bootstrap = path.join(dir, 'desktop-entry.cjs');
      const main = path.resolve(__dirname, '../../../src/main/main.ts');
      const detectR = path.resolve(__dirname, '../../../src/main/detect-r.ts');
      writeFileSync(
        bootstrap,
        `
          require(${JSON.stringify(require.resolve('ts-node/register'))});
          const { app } = require('electron');
          require(${JSON.stringify(detectR)}).startRDetection = () => app.exit(98);
          app.on('browser-window-created', () => app.exit(97));
          process.argv = process.argv.filter((arg) => arg !== '--no-sandbox');
          require(${JSON.stringify(main)});
        `,
      );
      delete env.ELECTRON_RUN_AS_NODE;
      // Linux CI may need the runner's sandbox setting to start Electron.
      // Keep that runtime switch out of the file-opening command above.
      const args = [bootstrap, 'file.R:2:3'];
      if (app.commandLine.hasSwitch('no-sandbox')) {
        args.splice(1, 0, '--no-sandbox');
      }
      const result = spawnSync(process.execPath, args, {
        cwd: dir,
        env,
        encoding: 'utf8',
        timeout: 10000,
      });
      assert.equal(result.status, 0, result.stderr);
      assert.equal(result.stdout, `<openfile>\n<${dir}/file.R:2:3>\n`);
    });

    it('fails instead of launching Desktop when the postback executable is missing', () => {
      env.RS_RPOSTBACK_PATH = path.join(dir, 'missing');
      const result = launch(['file.R']);
      assert.equal(result.status, 1);
      assert.include(result.stderr, 'ENOENT');
    });

    it('does not resolve a relative postback executable through PATH', () => {
      env.RS_RPOSTBACK_PATH = 'rpostback';
      const result = launch(['file.R']);
      assert.equal(result.status, 1);
      assert.include(result.stderr, 'must be an absolute path');
    });
  });
});
