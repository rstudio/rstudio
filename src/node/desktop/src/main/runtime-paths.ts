/*
 * runtime-paths.ts
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

import { FilePath } from '../core/file-path';

export interface RuntimePaths {

  /**
   * Path to rdesktop-dev.conf; only in dev builds
   */
  confPath?: FilePath,

  /**
   * Path to the rsession executable
   */
  sessionPath: FilePath,

  /**
   * Resolves to 'desktop' sub-directory in development builds and 'bin'
   * directory in release builds
   */
  scriptsPath: FilePath,

  /**
   * Path to the desktop executable
   */
  executablePath: FilePath,

  /**
   * Resolves to 'root' install directory in both development
   * and release builds, on macOS, points to 'Resources' directory
   */
  supportingFilePath: FilePath,

  /**
   * Resolves to 'desktop/resources' sub-directory in development builds and
   * to 'resources' sub-directory in release builds.
   */
  resourcesPath: FilePath,
  
  wwwDocsPath: FilePath
  urlopenerPath: FilePath,
  rsinversePath: FilePath
}