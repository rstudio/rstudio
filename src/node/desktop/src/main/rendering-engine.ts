/*
 * rendering-engine.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import { app } from 'electron';
import { DesktopOptions } from './desktop-options';

async function inferDefaultRenderingEngine(): Promise<string> {

  const features = await app.getGPUInfo('basic');
  console.log(features);

  // On Linux, it looks like Electron prefers using ANGLE for GPU rendering;
  // however, we've seen in at least one case (Ubuntu 20.04 in Parallels VM)
  // fails to render in that case (we just get a white screen).
  if (process.platform === 'linux') {
    return 'desktop';
  }

  return 'auto';

}

export async function initializeRenderingEngine(): Promise<void> {

  // if the user has selected a rendering engine, respect it
  const options = DesktopOptions();
  let engine = options.renderingEngine();
  console.log(`Rendering engine: ${engine}`);
  if (engine === '' || engine === 'auto') {
    engine = await inferDefaultRenderingEngine();
  }

  // set the engine via 'use-gl' flag
  if (engine && engine !== 'auto' && !app.commandLine.hasSwitch('use-gl')) {
    app.commandLine.appendSwitch('use-gl', engine);
  }

}