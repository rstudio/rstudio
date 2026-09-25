import { test, expect } from "@fixtures/rstudio.fixture";
import { executeInConsole, waitForConsoleIdle } from "@pages/console_pane.page";
import { waitForSessionRestart } from "@utils/project";
import { getVersion, openProject } from "@utils/commands";
import { useSuiteSandbox } from "@utils/sandbox";
import { rStringLiteral } from "@utils/r";
import { execFileSync } from "child_process";

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

// A project whose renv lockfile records a different (installed) version of R
// gets a warning bar offering to switch; taking the offer restarts the
// session with that R. Needs rig and a second R on the machine, so the test
// skips elsewhere. Desktop only: switching R relaunches the session process,
// which only the desktop can do.
test.describe("renv lockfile R version", { tag: ["@desktop_only"] }, () => {
  const sandbox = useSuiteSandbox();

  test("offers to switch to the R version recorded in the lockfile", async ({
    rstudioPage: page,
  }) => {
    test.setTimeout(180000);

    // the newest installed R from another major.minor series
    const current = (await getVersion(page)).r;
    const target = installedRVersions()
      .filter((entry) => majorMinor(entry.version) !== majorMinor(current))
      .sort((a, b) => compareVersions(b.version, a.version))[0];
    test.skip(!target, "needs rig and a second installed R version");

    // set up a project with a lockfile asking for the other R
    const projectDir = `${sandbox.dir.replace(/\\/g, "/")}/renv-r-switch`;
    const rprojPath = `${projectDir}/renv-r-switch.Rproj`;
    const lockfile = JSON.stringify({
      R: { Version: target!.version, Repositories: [] },
      Packages: {},
    });
    await executeInConsole(
      page,
      `{ dir.create(${rStringLiteral(projectDir)}); ` +
        `writeLines(c("Version: 1.0", "", "RestoreWorkspace: Default", "SaveWorkspace: Default"), ${rStringLiteral(rprojPath)}); ` +
        `writeLines(${rStringLiteral(lockfile)}, ${rStringLiteral(`${projectDir}/renv.lock`)}) }`,
    );
    await waitForConsoleIdle(page);

    await openProject(page, rprojPath);

    // the warning bar names both versions and offers the switch
    await expect(
      page.getByText(`created with R ${target!.version}, but R ${current} is in use`),
    ).toBeVisible({
      timeout: 60000,
    });
    const switchLink = page.getByText(`Switch to R ${target!.version}`, { exact: true });
    await expect(switchLink).toBeVisible();

    await switchLink.click();
    await waitForSessionRestart(page);

    // the project is still open, now under the requested R
    await expect(page.locator(PROJECT_MENU)).toContainText("renv-r-switch", { timeout: 30000 });
    await expect.poll(async () => (await getVersion(page)).r).toBe(target!.version);

    // no mismatch is reported once the versions agree
    await expect(page.getByText(`Switch to R`)).toHaveCount(0);
  });
});
