/*
 * product-info.ts
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

export interface ProductInfo {
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
