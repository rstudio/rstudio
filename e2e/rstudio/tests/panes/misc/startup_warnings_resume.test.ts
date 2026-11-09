import { test, expect } from '@fixtures/rstudio.fixture';
import { clearConsole, executeInConsole, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { executeCommand, getPref, setPref } from '@utils/commands';
import { dismissBlockingModals } from '@pages/modals.page';
import {
  closeProjectIfOpen,
  restartSessionWithSentinel,
  waitForSessionRestart,
} from '@utils/project';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';
import { TIMEOUTS } from '@utils/constants';
import type { Page } from 'playwright';

const WARNING = 'WARNING: RStudio could not start this session in ';

async function runCheckedR(page: Page, code: string): Promise<void> {
  const stamp = `${Date.now()}_${Math.random().toString(36).slice(2)}`;
  const marker = `[startup-warning-ready ${stamp}]`;
  // Split the marker so an echoed command cannot satisfy the assertion.
  await executeInConsole(
    page,
    `{ ${code}; cat("[startup-warning-", "ready ${stamp}]", sep = "") }`,
    { wait: true },
  );
  await expect(page.locator(CONSOLE_OUTPUT)).toContainText(marker);
}

async function makeWorkingDirectoryInaccessible(page: Page, directory: string): Promise<void> {
  const dir = rPathLiteral(directory);
  // The old R tempdir is removed on shutdown, so startup falls back to the
  // configured default directory. Removing search permission from that default
  // exercises the real working-directory warning without making the live
  // session's current directory inaccessible (which would move it elsewhere
  // before the restart and bypass the startup warning).
  await setPref(page, 'initial_working_directory', directory);
  await runCheckedR(page, `setwd(tempdir()); Sys.chmod(${dir}, "0600"); stopifnot(file.access(${dir}, 1) == -1)`);
}

async function expectWarningPaths(page: Page, paths: string[]): Promise<void> {
  // Reconnect after deferred init so the console includes both the persisted
  // actions and anything the new startup hook printed. This catches duplicate
  // output even if the client briefly renders only the replayed copy.
  await page.reload();
  await page.waitForFunction(() => window.rstudio?.ready === true, null, {
    timeout: TIMEOUTS.sessionRestart,
    polling: 50,
  });
  await expect.poll(async () => {
    const output = await page.locator(CONSOLE_OUTPUT).innerText();
    return output.split('\n').filter(line => line.startsWith(WARNING));
  }).toEqual(paths.map(directory => expect.stringContaining(`${WARNING}${directory} (`)));
}

// Suspend/resume is Server-only; Restart R runs in both modes. The fixture
// needs POSIX directory search permissions, so the Desktop case is skipped on
// Windows (Server runs on Linux). All filesystem changes go through R so this
// also works when the test runner and session are on different hosts.
const OPERATIONS = [
  { operation: 'Restart R', prefix: 'restart', tag: [] },
  { operation: 'suspend/resume', prefix: 'resume', tag: ['@server_only'] },
] as const;

test.describe('Startup warnings across session resume', () => {
  const sandbox = useSuiteSandbox();

  for (const { operation, prefix, tag } of OPERATIONS) {
    test(`keeps unchanged warnings once and announces changed paths after ${operation} (#19246)`, { tag: [...tag] }, async ({ rstudioPage: page, mode }) => {
      test.skip(mode === 'desktop' && process.platform === 'win32', 'needs POSIX directory search permissions');
      test.setTimeout(240000);
      await closeProjectIfOpen(page);
      const previousDefault = await getPref(page, 'initial_working_directory');
      const directories = ['first', 'second'].map(name => `${sandbox.dir}/${prefix}-${name}`);
      const dirs = `c(${directories.map(rPathLiteral).join(', ')})`;
      try {
        await setPref(page, 'initial_working_directory', sandbox.dir);
        await runCheckedR(page, `invisible(lapply(${dirs}, dir.create))`);
        await clearConsole(page);

        const cycle = async (directory: string): Promise<void> => {
          await makeWorkingDirectoryInaccessible(page, directory);
          if (operation === 'Restart R') {
            await restartSessionWithSentinel(page);
          } else {
            await executeCommand(page, 'suspendSession');
            await waitForSessionRestart(page);
          }
          await dismissBlockingModals(page);
          await runCheckedR(page, `Sys.chmod(${rPathLiteral(directory)}, "0700")`);
        };

        await cycle(directories[0]);
        await expectWarningPaths(page, [directories[0]]);

        // The unfixed build adds a second copy of the same warning here.
        await cycle(directories[0]);
        await expectWarningPaths(page, [directories[0]]);

        await cycle(directories[1]);
        await expectWarningPaths(page, directories);

        // Once cleared from the console, a warning whose condition persists
        // is printed again rather than lost.
        await clearConsole(page);
        await cycle(directories[1]);
        await expectWarningPaths(page, [directories[1]]);
      } finally {
        // Restore the pref first: the R cleanup can fail (e.g. while a failed
        // cycle is still restarting), and must not hide the test's own error.
        await setPref(page, 'initial_working_directory', previousDefault ?? '~');
        await runCheckedR(page, `Sys.chmod(${dirs}, "0700"); setwd(${rPathLiteral(sandbox.dir)})`)
          .catch((err: unknown) => console.warn(`[startup-warnings] cleanup failed: ${String(err)}`));
      }
    });
  }
});
