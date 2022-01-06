/*
 * console.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { Page } from 'playwright';

export const CONSOLE_TAB_SEL = '#rstudio_workbench_tab_console';
export const CONSOLE_INPUT_SEL = '#rstudio_console_input';
export const CONSOLE_OUTPUT_SEL = '#rstudio_workbench_panel_console';

export async function waitForConsoleReady(page: Page): Promise<void> {
  await page.waitForSelector(CONSOLE_TAB_SEL);
  await page.waitForSelector(CONSOLE_INPUT_SEL);
}

export async function clickConsoleTab(page: Page): Promise<void> {
  await page.click(CONSOLE_TAB_SEL);
}

export async function typeConsoleCommand(page: Page, command: string): Promise<void> {
  await clickConsoleTab(page);
  await clearConsoleInputLine(page);
  await page.type(CONSOLE_INPUT_SEL, command);
  await page.press(CONSOLE_INPUT_SEL, 'Enter');
}

export async function clearConsoleInputLine(page: Page): Promise<void> {
  await waitForConsoleReady(page);
  await page.press(CONSOLE_INPUT_SEL, 'Control+A'); // cursor to start
  await page.press(CONSOLE_INPUT_SEL, 'Control+K'); // clear to end of line
}

export async function clearConsole(page: Page): Promise<void> {
  await waitForConsoleReady(page);
  await page.press(CONSOLE_INPUT_SEL, 'Control+L'); // clean console
  await clearConsoleInputLine(page);
}
