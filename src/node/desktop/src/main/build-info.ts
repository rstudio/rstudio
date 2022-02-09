/*
 * build-info.ts.in
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

export interface BuildInfo {
  RSTUDIO_VERSION: string;
  RSTUDIO_BUILD_DATE: string;
  RSTUDIO_COPYRIGHT_YEAR: string;
  RSTUDIO_VERSION_PATCH: number;
  RSTUDIO_R_MAJOR_VERSION_REQUIRED: number;
  RSTUDIO_R_MINOR_VERSION_REQUIRED: number;
  RSTUDIO_R_PATCH_VERSION_REQUIRED: number;
  RSTUDIO_PACKAGE_OS: string;
  RSTUDIO_GIT_COMMIT: string;
  RSTUDIO_RELEASE_NAME: string;
}

// -----------------------------------------------------------------------------
// This file gets updated when doing a full build of Electron via make-package.
// Do not commit the updated file (won't break anything, but will make developer
// builds have the same version info as that make-package build).
// -----------------------------------------------------------------------------
export function buildInfo(): BuildInfo {
  return {
    RSTUDIO_VERSION: '2022.7.1-daily+321',
    RSTUDIO_BUILD_DATE: '2022-02-09',
    RSTUDIO_COPYRIGHT_YEAR: '2022',
    RSTUDIO_VERSION_PATCH: 1,
    RSTUDIO_R_MAJOR_VERSION_REQUIRED: 3,
    RSTUDIO_R_MINOR_VERSION_REQUIRED: 0,
    RSTUDIO_R_PATCH_VERSION_REQUIRED: 1,
    RSTUDIO_PACKAGE_OS: 'Unknown OS',
    RSTUDIO_GIT_COMMIT: 'c58914566b7f443d4311d7d7da944e4ad49f2baa',
    RSTUDIO_RELEASE_NAME: 'Spotted Wakerobin',
  };
}
