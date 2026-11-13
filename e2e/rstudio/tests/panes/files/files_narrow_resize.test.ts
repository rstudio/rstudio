// Files pane: the file list has to fit the pane while the pane is narrowed
// (#19593).
//
// Below 500px the file list gives its Size and Modified columns pixel widths
// that it recomputes on every resize, a frame after the pane itself has
// resized. With the Name column sized in pixels too, the table was wider than
// the pane for that frame, and the grid's scroll panel answered by flashing a
// horizontal scrollbar. The Name column now takes whatever width is left over,
// so the table fits at every frame.

import type { Page } from '@playwright/test';

import { test, expect } from '@fixtures/rstudio.fixture';

import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';

const FILES_PANEL = '#rstudio_workbench_panel_files';
const MIDDLE_COLUMN_SPLITTER = '#rstudio_middle_column_splitter';

// FilesList.BOUNDARY_WIDTH_PIXELS: below this the Size and Modified widths are
// recomputed on every resize.
const BOUNDARY_WIDTH = 500;

// Where the drag ends. Below 260px (the Name column's 204px minimum, plus the
// check and icon columns) the Size and Modified columns are both gone, so the
// drag crosses every layout FilesList computes below the boundary.
const NARROW_WIDTH = 220;

// The drag moves the splitter this far per mouse event, as a real drag does.
const DRAG_STEP = 3;

// One sample per animation frame: the grid's width, and how far the table of
// files extends past it.
interface FrameSample {
  width: number;
  overflow: number;
}

interface OverflowProbe {
  running: boolean;
  frames: FrameSample[];
}

type ProbeWindow = Window & { __pwFilesOverflow?: OverflowProbe };

async function startOverflowProbe(page: Page): Promise<void> {
  await page.evaluate((panel) => {
    const grid = document.querySelector<HTMLElement>(`${panel} [role="table"]`);

    // the grid holds a header, a body and a footer table; the body has the rows
    const table = Array.from(grid?.querySelectorAll('table') ?? [])
      .find((candidate) => candidate.querySelector('tbody td') !== null);
    if (!grid || !table)
      throw new Error('The Files pane grid has no table of files; is the pane showing a folder?');

    const probe: OverflowProbe = { running: true, frames: [] };
    (window as ProbeWindow).__pwFilesOverflow = probe;

    const sample = () => {
      if (!probe.running)
        return;

      const width = grid.getBoundingClientRect().width;
      probe.frames.push({ width, overflow: table.getBoundingClientRect().width - width });
      requestAnimationFrame(sample);
    };
    requestAnimationFrame(sample);
  }, FILES_PANEL);
}

async function stopOverflowProbe(page: Page): Promise<FrameSample[]> {
  return page.evaluate(() => {
    const probe = (window as ProbeWindow).__pwFilesOverflow;
    if (!probe)
      throw new Error('The overflow probe was never started.');

    probe.running = false;
    return probe.frames;
  });
}

interface SplitterPosition {
  left: number;
  x: number;
  y: number;
}

async function splitterCenter(page: Page): Promise<SplitterPosition> {
  const box = await page.locator(MIDDLE_COLUMN_SPLITTER).boundingBox();
  if (!box)
    throw new Error('Middle column splitter has no bounding box; is it rendered?');
  return { left: box.x, x: box.x + box.width / 2, y: box.y + box.height / 2 };
}

async function filesPanelWidth(page: Page): Promise<number> {
  return (await page.locator(FILES_PANEL).boundingBox())?.width ?? 0;
}

// A custom layout outlives the test, so put the splitter back, and leave the
// Files pane on the home folder.
async function restoreLayout(
  page: Page,
  consoleActions: ConsolePaneActions,
  home: SplitterPosition,
  dir: string,
): Promise<void> {
  // the probe is still running if the test failed before it was stopped
  await page.evaluate(() => {
    const probe = (window as ProbeWindow).__pwFilesOverflow;
    if (probe)
      probe.running = false;
  });

  await page.mouse.up();
  const narrowed = await splitterCenter(page);
  await page.mouse.move(narrowed.x, narrowed.y);
  await page.mouse.down();
  await page.mouse.move(home.x, home.y, { steps: 10 });
  await page.mouse.up();
  await expect.poll(async () => Math.abs((await splitterCenter(page)).left - home.left))
    .toBeLessThanOrEqual(2);

  await consoleActions.executeInConsole(
    `unlink(${dir}, recursive = TRUE); .rs.api.filesPaneNavigate(path.expand("~"))`,
    { wait: true },
  );
}

test.describe('Files pane narrow resize (#19593)', () => {
  test('the file list fits the pane at every frame while the pane is narrowed', async ({ rstudioPage: page }) => {
    const consoleActions = new ConsolePaneActions(page);
    const dir = 'file.path(tempdir(), "narrow-resize")';
    const home = await splitterCenter(page);

    try {
      // A folder of our own: the grid only has a table of files when the
      // folder shown has entries.
      await consoleActions.executeInConsole(
        `local({ d <- ${dir}; dir.create(d, showWarnings = FALSE); ` +
        'invisible(file.create(file.path(d, sprintf("file-%d.txt", 1:5)))); ' +
        '.rs.api.filesPaneNavigate(d) })',
        { wait: true },
      );
      await executeCommand(page, 'activateFiles');
      await expect(page.locator(`${FILES_PANEL} div[title="file-1.txt"]`)).toBeVisible();

      const startWidth = await filesPanelWidth(page);
      expect(startWidth, 'the Files pane has to start wide enough to be narrowed').toBeGreaterThan(NARROW_WIDTH + 100);

      // Dragging the splitter right narrows the column the Files pane is in.
      const end = home.x + (startWidth - NARROW_WIDTH);
      await startOverflowProbe(page);
      await page.mouse.move(home.x, home.y);
      await page.mouse.down();
      for (let x = home.x + DRAG_STEP; x < end; x += DRAG_STEP)
        await page.mouse.move(x, home.y);
      await page.mouse.move(end, home.y);
      await page.mouse.up();

      // Let the last step's resize land before reading the samples.
      await expect.poll(() => filesPanelWidth(page)).toBeLessThanOrEqual(NARROW_WIDTH + 2);
      await page.evaluate(() => new Promise<void>((resolve) => {
        requestAnimationFrame(() => requestAnimationFrame(() => resolve()));
      }));
      const frames = await stopOverflowProbe(page);

      // The samples only mean something if the drag narrowed the pane step by
      // step through the widths where the column widths are recomputed.
      const narrowing = frames.filter((frame, i) =>
        i > 0 && frame.width < frames[i - 1].width && frame.width < BOUNDARY_WIDTH);
      expect(narrowing.length, 'frames in which the pane got narrower').toBeGreaterThan(10);
      expect(Math.min(...frames.map((frame) => frame.width))).toBeLessThanOrEqual(NARROW_WIDTH + 2);

      const overflowing = frames.filter((frame) => frame.overflow > 0.5);
      expect(overflowing, 'frames in which the table of files was wider than the pane').toEqual([]);
    } catch (err) {
      // Cleanup can fail for the reason the test did (a wedged session, say).
      // Log that, so the test's own error is the one reported.
      await restoreLayout(page, consoleActions, home, dir)
        .catch((cleanupErr) => console.warn('files_narrow_resize cleanup failed:', cleanupErr));
      throw err;
    }

    await restoreLayout(page, consoleActions, home, dir);
  });
});
