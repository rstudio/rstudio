

/*
 * omni_insert.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import { CommandFn } from "./command";

// descriptive info for omni insert
export interface OmniInsert {
  name: string;
  keywords?: string[];
  description: string;
  group: OmniInsertGroup; 
  image: () => string;
}

// descriptive info + ability to identify/execute
export interface OmniInserter extends OmniInsert {
  id: string;
  command: CommandFn;
}

export enum OmniInsertGroup {
  Headings = 'Headings',
  Content = 'Content'
}
