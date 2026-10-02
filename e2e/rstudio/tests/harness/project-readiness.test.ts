import { test, expect } from '@playwright/test';
import { openProject } from '@utils/commands';
import { closeProjectIfOpen } from '@utils/project';

// Exercise the helpers against a controlled workbench lifecycle. Project
// identity and deferred initialization deliberately arrive separately, as
// they do when a desktop window switches sessions.
test.beforeEach(async ({ page }) => {
  await page.setContent(`
    <button id="rstudio_project_menubutton_toolbar">Project: example</button>
    <button id="rstudio_label_close_project_command">Close Project</button>
    <div id="rstudio_console_input"><textarea class="ace_text-input"></textarea></div>
  `);
});

test('close waits for the replacement session to be ready and idle', async ({ page }) => {
  await page.evaluate(() => {
    let active = true;
    window.rstudio = {
      ready: true,
      project: { isActive: () => active },
    } as typeof window.rstudio;
    document.getElementById('rstudio_label_close_project_command')!.onclick = () => {
      window.rstudio!.ready = false;
      active = false;
      // The old console looks idle while the new bridge is being installed.
      setTimeout(() => {
        document.getElementById('rstudio_console_input')!.classList.add('rstudio-console-busy');
        window.rstudio!.ready = true;
        setTimeout(() => {
          document.getElementById('rstudio_console_input')!.classList.remove('rstudio-console-busy');
        }, 250);
      }, 1000);
    };
  });

  expect(await closeProjectIfOpen(page)).toBe(false);
  expect(await page.evaluate(() => window.rstudio!.ready)).toBe(true);
  await expect(page.locator('#rstudio_console_input')).not.toHaveClass(/rstudio-console-busy/);
});

test('open requires ready and the requested project in the same session', async ({ page }) => {
  await page.evaluate(() => {
    let activePath = '/old/old.Rproj';
    window.rstudio = {
      ready: false,
      project: {
        path: () => activePath,
        open: (target: string) => {
          // The outgoing session finishes deferred init during the switch.
          window.rstudio!.ready = true;
          // Its replacement publishes SessionInfo before deferred init.
          setTimeout(() => {
            activePath = target;
            window.rstudio!.ready = false;
            setTimeout(() => { window.rstudio!.ready = true; }, 1000);
          }, 250);
        },
      },
    } as typeof window.rstudio;
  });

  await openProject(page, '/new/new.Rproj');
  expect(await page.evaluate(() => window.rstudio!.ready)).toBe(true);
  expect(await page.evaluate(() => window.rstudio!.project.path())).toBe('/new/new.Rproj');
});
