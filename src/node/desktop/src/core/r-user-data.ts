/*
 * r-user-data.ts
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

export const kRStudioInitialWorkingDir = 'RS_INITIAL_WD';
export const kRStudioInitialEnvironment = 'RS_INITIAL_ENV';
export const kRStudioInitialProject = 'RS_INITIAL_PROJECT';

export enum SessionType {
  SessionTypeDesktop = 0,
  SessionTypeServer = 1,
}
