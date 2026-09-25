import { test, expect } from "@fixtures/rstudio.fixture";
import { executeInConsole, waitForConsoleIdle } from "@pages/console_pane.page";
import { NO_BTN, YES_BTN } from "@pages/modals.page";
import { closeProjectIfOpen, restartSessionWithSentinel, waitForSessionRestart } from "@utils/project";
import { getVersion, openProject } from "@utils/commands";
import { useSuiteSandbox } from "@utils/sandbox";
import { rStringLiteral } from "@utils/r";
import { captureResult } from "@utils/terminal";
import { execFileSync } from "child_process";
import { readFileSync } from "fs";
import type { Page } from "playwright";

const PROJECT_MENU = "#rstudio_project_menubutton_toolbar";

interface InstalledR {
  version: string;
  binary: string;
}

// The R installations rig knows about on this machine, from both of rig's
// modes (each lists only its own installations). Empty when rig is absent.
function installedRVersions(): InstalledR[] {
  const versions: InstalledR[] = [];
  for (const mode of ["--admin", "--user"]) {
    try {
      const output = execFileSync("rig", ["list", "--json", mode], {
        encoding: "utf-8",
        stdio: ["ignore", "pipe", "ignore"],
      });
      for (const entry of JSON.parse(output) as InstalledR[]) {
        if (entry.version && entry.binary)
          versions.push(entry);
      }
    } catch {
      // rig missing, or this mode has nothing to list
    }
  }
  return versions;
}

function majorMinor(version: string): string {
  return version.split(".").slice(0, 2).join(".");
}

function compareVersions(a: string, b: string): number {
  const pa = a.split(".").map(Number);
  const pb = b.split(".").map(Number);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const diff = (pa[i] ?? 0) - (pb[i] ?? 0);
    if (diff !== 0)
      return diff;
  }
  return 0;
}

// Whether an installation runs as itself when it isn't the default: false for
// a macOS framework version whose launcher still names the framework's shared
// home (see src/node/desktop/src/main/r-framework.ts), which RStudio offers
// to update before switching to it.
function isOrthogonal(binary: string): boolean {
  if (!/\/R\.framework\/Versions\/[^/]+\/Resources\/bin\/R$/.test(binary))
    return true;

  try {
    return !readFileSync(binary, "utf-8").includes("R.framework/Resources");
  } catch {
    return true;
  }
}

// The newest installed R from a major.minor series other than the current
// one, among those that do (or don't) run as themselves.
function otherR(current: string, orthogonal: boolean): InstalledR | undefined {
  return installedRVersions()
    .filter((entry) => majorMinor(entry.version) !== majorMinor(current))
    .filter((entry) => isOrthogonal(entry.binary) === orthogonal)
    .sort((a, b) => compareVersions(b.version, a.version))[0];
}

// The version of R that an R process started by the session runs (as R CMD
// INSTALL or renv::restore() would start one).
const CHILD_R_VERSION =
  'tail(system2(file.path(R.home("bin"), if (.Platform$OS.type == "windows") "R.exe" else "R"), ' +
  'c("--vanilla", "-s", "-e", shQuote("cat(format(getRversion()))")), stdout = TRUE), 1L)';

// The R executable the session runs, to switch back to afterward.
const SESSION_R = 'file.path(R.home("bin"), if (.Platform$OS.type == "windows") "R.exe" else "R")';

// Open a new project whose renv lockfile asks for the given version of R.
async function openProjectRequestingR(page: Page, dir: string, version: string): Promise<void> {
  const rprojPath = `${dir}/${dir.split("/").pop()}.Rproj`;
  const lockfile = JSON.stringify({
    R: { Version: version, Repositories: [] },
    Packages: {},
  });
  await executeInConsole(
    page,
    `{ dir.create(${rStringLiteral(dir)}); ` +
      `writeLines(c("Version: 1.0", "", "RestoreWorkspace: Default", "SaveWorkspace: Default"), ${rStringLiteral(rprojPath)}); ` +
      `writeLines(${rStringLiteral(lockfile)}, ${rStringLiteral(`${dir}/renv.lock`)}) }`,
  );
  await waitForConsoleIdle(page);

  await openProject(page, rprojPath);
}

// A project whose renv lockfile records a different (installed) version of R
// gets a warning bar offering to switch; taking the offer restarts the
// session with that R. Needs rig and a second R on the machine, so the tests
// skip elsewhere. Desktop only: switching R relaunches the session process,
// which only the desktop can do.
test.describe("renv lockfile R version", { tag: ["@desktop_only"] }, () => {
  const sandbox = useSuiteSandbox();

  // the R this worker's RStudio started with, once a test has switched away
  let originalR = "";

  // The RStudio instance is shared by later tests in the worker, and a
  // switch lasts until it quits; switch back as the project closes.
  test.afterAll(async ({ rstudioPage: page }) => {
    if (originalR) {
      const error = await page.evaluate(
        (rPath) =>
          new Promise<string>((resolve) => {
            const desktop = (window as unknown as {
              desktop: { setPendingRVersion(rPath: string, callback: (error: string) => void): void };
            }).desktop;
            desktop.setPendingRVersion(rPath, resolve);
          }),
        originalR,
      );
      if (error)
        console.warn(`[renv_r_version_switch] unable to switch back to ${originalR}: ${error}`);
    }

    try {
      await closeProjectIfOpen(page);
    } catch (err) {
      console.warn("[renv_r_version_switch] afterAll closeProjectIfOpen failed:", err);
    }
  });

  test("offers to switch to the R version recorded in the lockfile", async ({
    rstudioPage: page,
  }) => {
    test.setTimeout(180000);

    const current = (await getVersion(page)).r;
    const target = otherR(current, true);
    test.skip(!target, "needs rig and a second installed R version that runs as itself");

    await openProjectRequestingR(page, `${sandbox.dir.replace(/\\/g, "/")}/renv-r-switch`, target!.version);

    // the warning bar names both versions and offers the switch
    await expect(
      page.getByText(`created with R ${target!.version}, but R ${current} is in use`),
    ).toBeVisible({
      timeout: 60000,
    });
    const switchLink = page.getByText(`Switch to R ${target!.version}`, { exact: true });
    await expect(switchLink).toBeVisible();

    originalR ||= await captureResult(page, SESSION_R);

    await switchLink.click();
    await waitForSessionRestart(page);

    // the project is still open, now under the requested R
    await expect(page.locator(PROJECT_MENU)).toContainText("renv-r-switch", { timeout: 30000 });
    await expect.poll(async () => (await getVersion(page)).r).toBe(target!.version);

    // and the session finds nothing more to report (asked directly: the
    // check itself runs in the background, with no signal when it's done)
    const check = await captureResult(page, '.rs.projectRVersionCheck(getwd(), "", "3.6.0", FALSE, FALSE)$type');
    expect(check).toBe("none");

    // R processes the session starts run that R as well
    expect(await captureResult(page, CHILD_R_VERSION)).toBe(target!.version);
  });

  test("asks before updating an R that would run the default version", async ({
    rstudioPage: page,
  }) => {
    test.setTimeout(180000);

    const current = (await getVersion(page)).r;
    const target = otherR(current, false);
    test.skip(!target, "needs a second macOS framework version of R that isn't orthogonal");

    await openProjectRequestingR(page, `${sandbox.dir.replace(/\\/g, "/")}/renv-r-update`, target!.version);

    const switchLink = page.getByText(`Switch to R ${target!.version}`, { exact: true });
    await expect(switchLink).toBeVisible({ timeout: 60000 });
    await switchLink.click();

    // the update is explained, and declining it leaves the installation and
    // the session as they were, with the offer still up
    await expect(page.getByText("This changes the installation for all users of this Mac", { exact: false })).toBeVisible();
    await expect(page.locator(YES_BTN)).toBeVisible();
    await page.locator(NO_BTN).click();

    await expect(page.locator(NO_BTN)).toHaveCount(0);
    await expect(switchLink).toBeVisible();
    expect(isOrthogonal(target!.binary)).toBe(false);
    expect((await getVersion(page)).r).toBe(current);
  });

  // Updating an installation changes it for every user of the machine, so
  // this runs only when asked to.
  test("updates an R that would run the default version when asked, then switches", async ({
    rstudioPage: page,
  }) => {
    test.setTimeout(180000);
    test.skip(
      !process.env.PW_ALLOW_R_FRAMEWORK_UPDATE,
      "updates a system-wide installation of R; set PW_ALLOW_R_FRAMEWORK_UPDATE=1 to allow",
    );

    const current = (await getVersion(page)).r;
    const target = otherR(current, false);
    test.skip(!target, "needs a second macOS framework version of R that isn't orthogonal");

    await openProjectRequestingR(page, `${sandbox.dir.replace(/\\/g, "/")}/renv-r-orthogonal`, target!.version);

    const switchLink = page.getByText(`Switch to R ${target!.version}`, { exact: true });
    await expect(switchLink).toBeVisible({ timeout: 60000 });

    originalR ||= await captureResult(page, SESSION_R);

    await switchLink.click();
    await page.locator(YES_BTN).click();
    await waitForSessionRestart(page);

    // the installation now runs as itself, in the session and its children
    expect(isOrthogonal(target!.binary)).toBe(true);
    await expect.poll(async () => (await getVersion(page)).r).toBe(target!.version);
    expect(await captureResult(page, CHILD_R_VERSION)).toBe(target!.version);
  });
});

// The check runs again when R restarts (as renv's own check does) and when a
// page reconnects to the session, so a dismissed warning comes back. These
// only need a version other than the current one, not an installation of it,
// and run on either edition.
test.describe("renv lockfile R version after a restart or refresh", () => {
  const sandbox = useSuiteSandbox();

  test.afterAll(async ({ rstudioPage: page }) => {
    try {
      await closeProjectIfOpen(page);
    } catch (err) {
      console.warn("[renv_r_version_switch] afterAll closeProjectIfOpen failed:", err);
    }
  });

  test("warns again after Restart R", async ({ rstudioPage: page }) => {
    test.setTimeout(180000);

    const current = (await getVersion(page)).r;
    const requested = majorMinor(current) === "4.1" ? "4.0.5" : "4.1.3";

    await openProjectRequestingR(page, `${sandbox.dir.replace(/\\/g, "/")}/renv-r-restart`, requested);

    const warning = page.getByText(`created with R ${requested}, but R ${current} is in use`);
    await expect(warning).toBeVisible({ timeout: 60000 });

    await page.getByRole("button", { name: "Dismiss Warning Bar" }).click();
    await expect(warning).toBeHidden();

    await restartSessionWithSentinel(page);
    await expect(warning).toBeVisible({ timeout: 60000 });
  });

  test("warns again after a browser refresh", async ({ rstudioPage: page }) => {
    test.setTimeout(180000);

    const current = (await getVersion(page)).r;
    const requested = majorMinor(current) === "4.1" ? "4.0.5" : "4.1.3";

    await openProjectRequestingR(page, `${sandbox.dir.replace(/\\/g, "/")}/renv-r-refresh`, requested);

    const warning = page.getByText(`created with R ${requested}, but R ${current} is in use`);
    await expect(warning).toBeVisible({ timeout: 60000 });

    await page.getByRole("button", { name: "Dismiss Warning Bar" }).click();
    await expect(warning).toBeHidden();

    await page.reload();
    await page.waitForFunction(() => window.rstudio?.ready === true, null, { timeout: 60000, polling: 100 });
    await expect(warning).toBeVisible({ timeout: 60000 });
  });
});
