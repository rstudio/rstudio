/**
 * New Connection wizard, fed by a snippet file in the `connections-path`
 * folder rather than an ODBC driver, so it runs without a registered driver
 * or a database. Server and Port share order 1, which renders them side by
 * side in the first row; every field, including Port, must be reachable by
 * its label (#19381).
 */

import * as path from 'path';
import { test, expect } from '@fixtures/rstudio.fixture';
import { ConnectionsPaneActions } from '@actions/connections_pane.actions';
import { executeInConsole } from '@pages/console_pane.page';
import { seedSandboxFile } from '@utils/files';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';

const SNIPPET_NAME = 'Playwright Snippet';

const SNIPPET = [
  'library(DBI)',
  'con <- dbConnect(',
  '  odbc::odbc(),',
  '  Server   = "${1:Server=localhost}",',
  '  Port     = ${1:Port=5432},',
  '  Database = "${2:Database=mydb}"',
  ')',
  '',
].join('\n');

test.describe.serial('New Connection wizard (snippet file)', () => {
  const sandbox = useSuiteSandbox();
  let actions: ConnectionsPaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    actions = new ConnectionsPaneActions(page);
    const snippetPath = await seedSandboxFile(
      page, sandbox.dir, `connections/${SNIPPET_NAME}.R`, SNIPPET);
    await executeInConsole(
      page, `options(\`connections-path\` = ${rPathLiteral(path.dirname(snippetPath))})`);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    if (await actions.wizard.dialog.isVisible()) {
      await actions.wizard.cancelBtn.click();
      await actions.wizard.dialog.waitFor({ state: 'hidden', timeout: 5000 });
    }
    await executeInConsole(page, 'options(`connections-path` = NULL)');
  });

  test('labels every field, including the second one in the first row', async () => {
    await actions.activatePane();
    await actions.openWizard();
    const entry = actions.wizard.typeEntry(SNIPPET_NAME);
    await expect(entry).toBeVisible({ timeout: 10000 });
    await entry.click();

    await expect(actions.wizard.field('Server')).toHaveValue('localhost');
    await expect(actions.wizard.field('Port')).toHaveValue('5432');
    await expect(actions.wizard.field('Database')).toHaveValue('mydb');
  });
});
