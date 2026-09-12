/*
 * application-launch.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

import path from 'path';
import { spawn } from 'child_process';
import { existsSync, readdirSync } from 'fs';
import { setenv, unsetenv } from '../core/environment';
import { kProjectNone, kRStudioInitialProject, kRStudioInitialWorkingDir } from '../core/r-user-data';
import { MainWindow } from './main-window';
import { isAutomated } from './utils';
import { app } from 'electron';

export interface LaunchRStudioOptions {
  projectFilePath?: string;
  workingDirectory?: string;

  // start the new session with no project, in the user's default working directory
  noProject?: boolean;
}

export function resolveProjectFile(projectDir: string): string {
  // check that the project directory exists
  if (!existsSync(projectDir)) {
    return '';
  }

  // list files in the directory, looking for a .Rproj file
  const files = readdirSync(projectDir, { encoding: 'utf-8' });
  for (const file of files) {
    const ext = path.extname(file).toLowerCase();
    if (ext === '.rproj') {
      return path.join(projectDir, file);
    }
  }

  // nothing found
  return '';
}

export class ApplicationLaunch {
  mainWindow?: MainWindow;
  static init(): ApplicationLaunch {
    return new ApplicationLaunch();
  }

  setActivationWindow(window: MainWindow): void {
    this.mainWindow = window;
  }

  activateWindow(): void {
    // TODO - reimplement (if needed at all)
  }

  launchRStudio(options: LaunchRStudioOptions): void {
    // in devmode, we need to pass the directory path when launching the application;
    // for package builds, we have no such requirement
    const argv = app.isPackaged ? [] : [process.argv[1]];

    // keep the relaunched instance from stealing focus during automation runs
    if (isAutomated()) {
      argv.push('--automation-agent');
    }

    // this instance may have been started by opening a project or a file, which leaves these
    // variables set in our own environment; clear them so they can't leak to the new session
    unsetenv(kRStudioInitialProject);
    unsetenv(kRStudioInitialWorkingDir);

    if (options.noProject) {
      // ask for no project explicitly, so the session doesn't restore the last one; with no
      // working directory of our own the session uses the user's default working directory
      setenv(kRStudioInitialProject, kProjectNone);
    } else {
      // resolve working directory; callers give us either a directory or a project file
      const workingDir =
        options.workingDirectory ??
        (options.projectFilePath === undefined ? undefined : path.dirname(options.projectFilePath));
      if (workingDir !== undefined) {
        setenv(kRStudioInitialWorkingDir, workingDir);
      }

      // resolve project file, if any
      const projectFile = options.projectFilePath ?? (workingDir === undefined ? '' : resolveProjectFile(workingDir));
      if (existsSync(projectFile)) {
        setenv(kRStudioInitialProject, projectFile);
      }
    }

    // run it
    const childProcess = spawn(process.execPath, argv, {
      detached: true,
      stdio: 'ignore', // don't reuse the stdio from parent
    });
    childProcess.unref();

    // restore environment variables
    unsetenv(kRStudioInitialProject);
    unsetenv(kRStudioInitialWorkingDir);
  }
}
