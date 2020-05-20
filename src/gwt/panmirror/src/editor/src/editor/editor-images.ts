/*
 * editor-images.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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

import { EditorUIImages } from '../api/ui-images';

import copyImage from './images/copy.png';
import propertiesImage from './images/properties.png';
import removelinkImage from './images/removelink.png';
import runchunkImage from './images/runchunk.png';
import runprevchunksImage from './images/runprevchunks.png';

export function defaultEditorUIImages(): EditorUIImages {
  return {
    copy: copyImage,
    properties: propertiesImage,
    removelink: removelinkImage,
    runchunk: runchunkImage,
    runprevchunks: runprevchunksImage,
  };
}
