/*
 * r-user-data.ts
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

import { Err } from './err';

export const kRStudioInitialWorkingDir = 'RS_INITIAL_WD';
export const kRStudioInitialEnvironment = 'RS_INITIAL_ENV';
export const kRStudioInitialProject = 'RS_INITIAL_PROJECT';

export enum SessionType
{
   SessionTypeDesktop = 0,
   SessionTypeServer = 1
}

/**
 * This routine migrates user state data from its home in RStudio 1.3 and prior
 * (usually ~/.rstudio) to its XDG-compliant home in RStudio 1.4 and later
 * (~/.local/share/rstudio or $XDG_DATA_HOME).
 *
 * This is a one-time migration that cleans up the old folder. 
 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export function migrateUserStateIfNecessary(sessionType: SessionType): Err {
  // TODO Implement migrateUserStateIfNecessary()
  return new Error('migrateUserStateIfNecessary NYI');
}
